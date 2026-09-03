# CartRex — Authorization Implementation Specification

## 1. Purpose

Implement authorization for the existing CartRex Spring Boot REST API.

This document is an **implementation contract**, not a conceptual explanation.

The coding agent MUST:

1. Inspect the existing project before modifying anything.
2. Preserve the existing architecture and endpoint contracts.
3. Implement authentication and authorization without breaking existing APIs.
4. Enforce role-based access control.
5. Enforce customer resource ownership.
6. Enforce store-level isolation for `STORE_STAFF`.
7. Enforce trolley/device binding for `DEVICE`.
8. Secure payment webhooks using signature verification.
9. Never trust user identity, role, price, payment status, store ownership, or trolley identity supplied by an untrusted client.
10. Add tests for every authorization rule.

---

## 2. Technology Context

| Layer | Technology |
|---|---|
| Framework | Spring Boot |
| Language | Java |
| Authentication | JWT |
| Password hashing | bcrypt |
| Validation | Jakarta Validation |
| API | REST |
| Database | MySQL |

Existing roles:

- `CUSTOMER`
- `STORE_STAFF`
- `ADMIN`
- `DEVICE`

Use Spring Security. Do not introduce another authentication framework unless the existing project already requires it.

---

## 3. Security Architecture

```
Authentication
      ↓
JWT / Device Token validation
      ↓
Role authorization
      ↓
Resource ownership
      ↓
Store authorization
      ↓
Device/trolley authorization
      ↓
Business authorization
      ↓
Controller
```

Authorization must **not** be implemented only by checking the role (e.g. `if (role.equals("CUSTOMER")) allow();` is insufficient). A customer must also own the requested resource.

---

## 4. Security Principals

Implement these authorities using the standard Spring Security representation:

- `ROLE_CUSTOMER`
- `ROLE_STORE_STAFF`
- `ROLE_ADMIN`
- `ROLE_DEVICE`

Do not use inconsistent representations (`CUSTOMER`, `ROLE-CUSTOMER`, `customer`, `ROLE_customer`).

---

## 5. Public Endpoints

No access JWT required:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh` (requires a valid **refresh token**)
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password` (requires a valid **password-reset token**)

Do not make protected business endpoints public.

---

## 6. Customer Authorization

CUSTOMER may access:

```
GET  /api/products
GET  /api/products/{id}
GET  /api/products/barcode/{barcode}

POST /api/carts/{sessionId}/items
POST /api/carts/{sessionId}/items/batch
GET  /api/carts/{sessionId}
PATCH /api/carts/{sessionId}/items/{productId}
DELETE /api/carts/{sessionId}/items/{productId}
POST /api/carts/{sessionId}/items/{productId}/verify-weight

POST /api/sessions
GET  /api/sessions/{id}
POST /api/sessions/{id}/end

GET /api/orders/{id}

POST /api/payments/initiate
GET  /api/payments/{orderId}/status

GET /api/offers
GET /api/offers/{id}

GET /api/notifications
PATCH /api/notifications/{id}/read

GET /api/shopping-list
POST /api/shopping-list
POST /api/shopping-list/{listId}/items
PATCH /api/shopping-list/items/{itemId}

GET /api/recommendations/personalized
GET /api/recommendations/fbt
GET /api/recommendations/basic
```

Customer must **not** access: product management, inventory management, offer management, trolley management, admin APIs, recommendation recomputation, other customers' resources, device endpoints.

---

## 7. Customer Resource Ownership

**Mandatory.** Never trust `userId` from request parameters/body when determining the authenticated customer. Use the authenticated JWT principal:

```java
UUID authenticatedUserId = authenticationService.getAuthenticatedUserId();
```

Then verify ownership.

### 7.1 Session Ownership

For:
```
GET /api/sessions/{id}
POST /api/sessions/{id}/end
GET /api/carts/{sessionId}
POST /api/carts/{sessionId}/items
POST /api/carts/{sessionId}/items/batch
PATCH /api/carts/{sessionId}/items/{productId}
DELETE /api/carts/{sessionId}/items/{productId}
POST /api/carts/{sessionId}/items/{productId}/verify-weight
```

verify: `session.userId == authenticatedUserId`. If false → `403 FORBIDDEN` (or the project's consistently chosen resource-isolation response). Do not expose another customer's session information.

### 7.2 Order Ownership

For:
```
GET /api/orders/{id}
POST /api/payments/initiate
GET /api/payments/{orderId}/status
```

verify: `order.userId == authenticatedUserId`. A CUSTOMER must never GET another customer's order, initiate payment for another customer's order, or read another customer's payment status.

### 7.3 Notification Ownership

Current API: `GET /api/notifications?userId={userId}`.

Do **not** trust arbitrary `userId`. Either:
- **Preferred:** derive user from JWT — `GET /api/notifications` (no query param), or
- If the existing contract must remain: enforce `requestedUserId == authenticatedUserId`.

Never allow Customer A to pass `userId = Customer B` and read Customer B's notifications.

### 7.4 Shopping List Ownership

For:
```
GET /api/shopping-list
POST /api/shopping-list
POST /api/shopping-list/{listId}/items
PATCH /api/shopping-list/items/{itemId}
```

verify `ShoppingList.userId == authenticatedUserId`. For shopping-list items, resolve `ShoppingListItem → ShoppingList → userId` and confirm it belongs to the authenticated customer.

### 7.5 Recommendation Ownership

For `GET /api/recommendations/personalized?userId={userId}`, enforce `userId == authenticatedUserId`. Prefer deriving `userId` from the JWT rather than accepting it from the request.

---

## 8. Store Staff Authorization

`STORE_STAFF` has access only to resources belonging to their authorized store.

Store staff may access:
```
GET  /api/products
GET  /api/products/{id}
GET  /api/products/barcode/{barcode}
POST /api/products
PUT  /api/products/{id}
DELETE /api/products/{id}

GET /api/trolleys/{code}
PATCH /api/trolleys/{id}/status

GET /api/inventory
GET /api/inventory/low-stock
PATCH /api/inventory/{productId}/adjust

GET /api/offers
GET /api/offers/{id}
POST /api/offers
PUT /api/offers/{id}
DELETE /api/offers/{id}

GET /api/admin/trolleys
GET /api/admin/products
GET /api/admin/stock
GET /api/admin/stock/low
GET /api/admin/orders
```

Store staff must **not** access other stores, customer accounts, customer shopping lists/notifications/sessions/carts/payments, recommendation recomputation, trolley creation, or bulk product deactivation.

### 8.1 Store Isolation

Every `STORE_STAFF` user must have an authorized store association:

```
User
 ├── id
 ├── role
 └── storeId
```

Before allowing store operations, compare `authenticatedUser.storeId` against the requested/resource store:

```java
UUID authenticatedStoreId = authenticatedUser.getStoreId();
```

Required: `resource.storeId == authenticatedUser.storeId` (or `requestedStoreId == authenticatedUser.storeId`).

### 8.2 Never Trust Request `storeId`

Forbidden:
```java
UUID storeId = request.getStoreId();
allow(); // WRONG
```

Correct flow: `JWT → authenticated user → authenticated store → requested store → compare`.

```java
if (!authenticatedStoreId.equals(requestedStoreId)) {
    throw new ForbiddenException(...);
}
```

### 8.3 Product Store Authorization

For `PUT /api/products/{id}` and `DELETE /api/products/{id}`: load the product first, then require `product.storeId == authenticatedStaff.storeId` for STORE_STAFF. ADMIN can access any product.

### 8.4 Inventory Store Authorization

For `GET /api/inventory`, `GET /api/inventory/low-stock`, `PATCH /api/inventory/{productId}/adjust`: STORE_STAFF must only operate on inventory belonging to their store. Do not authorize solely from `productId` — resolve `product → inventory → store → authenticated staff` and compare store IDs.

### 8.5 Offer Store Authorization

For `POST/PUT/DELETE /api/offers...`: STORE_STAFF may only manage offers belonging to their store. Creation: `request.storeId == authenticatedStaff.storeId`. Update/delete: `offer.storeId == authenticatedStaff.storeId`. ADMIN can manage any store's offers.

### 8.6 Trolley Store Authorization

For `GET /api/trolleys/{code}`, `PATCH /api/trolleys/{id}/status`, `GET /api/admin/trolleys`: STORE_STAFF may only access trolleys belonging to their store — `trolley.storeId == authenticatedStaff.storeId`. ADMIN can access all trolleys.

---

## 9. Admin Authorization

ADMIN has system-wide administrative access to: product management, inventory management, offer management, trolley management, admin product/stock/order APIs, recommendation recomputation, and (where explicitly permitted) customer-owned order lookup and payment status lookup.

ADMIN must **not** automatically be treated as a CUSTOMER. Do not assume `ADMIN → CUSTOMER` privileges unless explicitly implemented.

### 9.1 Admin-Only Endpoints

```
POST /api/trolleys
POST /api/admin/products/bulk-deactivate
POST /api/recommendations/recompute
```

STORE_STAFF must receive `403 FORBIDDEN` for these.

---

## 10. Device Authorization

DEVICE represents a physical ESP32 trolley. Device requests must **not** use customer JWTs — authenticate using the trolley's device credential:

```
Device Token → Trolley → esp32Mac → Trolley Code
```

### 10.1 Device Heartbeat

`POST /api/trolleys/{code}/heartbeat` — allowed: DEVICE only. Denied: CUSTOMER, STORE_STAFF, ADMIN, PUBLIC.

Validation required: device token exists AND is valid AND belongs to trolley `{code}`.
- Invalid → `401 INVALID_TOKEN`
- Trolley not found → `404 TROLLEY_NOT_FOUND`

### 10.2 Device/Trolley Binding

Never trust `trolleyCode` alone. Verify `authenticatedDevice.trolleyId == requestedTrolley.id`. If false → `401 INVALID_TOKEN` (or the project's consistent device-authentication failure response).

### 10.3 Device/Session Binding

A device may only operate on its currently assigned trolley session:

```
DEVICE → TROLLEY → ACTIVE SESSION
```

Required: `device.trolleyId == session.trolleyId`. The device must not send events to another trolley's session.

### 10.4 Device/Cart Access

If device-based cart operations are implemented for:
```
POST /api/carts/{sessionId}/items
POST /api/carts/{sessionId}/items/batch
POST /api/carts/{sessionId}/items/{productId}/verify-weight
```

DEVICE access requires: valid device token + device belongs to session trolley + session is active. Do not allow a DEVICE to access arbitrary customer sessions.

---

## 11. Payment Webhook Authorization

`POST /api/payments/webhook` is **not** a normal RBAC endpoint. Do not use `ROLE_CUSTOMER`/`ROLE_STORE_STAFF`/`ROLE_ADMIN`/`ROLE_DEVICE` as the primary authentication mechanism — use payment-provider webhook signature verification.

```
Webhook request
      ↓
Read raw request body
      ↓
Read gateway signature
      ↓
Verify signature
      ↓
Validate payload
      ↓
Find order
      ↓
Validate amount
      ↓
Validate payment state
      ↓
Process event
```

### 11.1 Rules

- Never trust `{"status": "SUCCESS"}` by itself — verify the event came from the payment gateway.
- Also validate: `orderId`, `amount`, payment identifier, `status`, `signature`.
- The webhook must be **idempotent**: repeated webhook events must not create duplicate payments/orders or double-update the order.

### 11.2 Order Payment Authorization

```
Customer → Own Order → Initiate Payment → Gateway → Webhook → Verify Signature → Update Payment → Update Order
```

Never allow a customer to directly set `Order = PAID`.

### 11.3 Order Status Protection

Customer requests must never directly modify order status (`PENDING`/`PAID`/`FAILED`/`CANCELLED`). State changes occur only through authorized business flows (e.g. `Payment SUCCESS → Order PAID`), not via an unrestricted `PUT /orders/{id} {"status": "PAID"}`.

---

## 12. Role Matrix

Legend: 👤 = resource ownership check · 🏪 = store-scope check · 🤖 = device authentication/binding · 🔑 = special token authentication · 💳 = payment signature authentication

| Endpoint | PUBLIC | CUSTOMER | STAFF | ADMIN | DEVICE |
|---|:---:|:---:|:---:|:---:|:---:|
| POST `/api/auth/register` | ✅ | ✅ | ❌ | ❌ | ❌ |
| POST `/api/auth/login` | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST `/api/auth/refresh` | 🔑 | 🔑 | 🔑 | 🔑 | ❌ |
| POST `/api/auth/forgot-password` | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST `/api/auth/reset-password` | 🔑 | 🔑 | 🔑 | 🔑 | ❌ |
| GET `/api/products` | ❌ | ✅ | 🏪 | ✅ | ❌ |
| GET `/api/products/{id}` | ❌ | 👤 | 🏪 | ✅ | ❌ |
| GET `/api/products/barcode/{barcode}` | ❌ | 👤 | 🏪 | ✅ | ❌ |
| POST `/api/products` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| PUT `/api/products/{id}` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| DELETE `/api/products/{id}` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| POST `/api/carts/{sessionId}/items` | ❌ | 👤 | ❌ | ❌ | 🤖 |
| POST `/api/carts/{sessionId}/items/batch` | ❌ | 👤 | ❌ | ❌ | 🤖 |
| GET `/api/carts/{sessionId}` | ❌ | 👤 | ❌ | ❌ | ❌ |
| PATCH `/api/carts/{sessionId}/items/{productId}` | ❌ | 👤 | ❌ | ❌ | ❌ |
| DELETE `/api/carts/{sessionId}/items/{productId}` | ❌ | 👤 | ❌ | ❌ | ❌ |
| POST `/api/carts/{sessionId}/items/{productId}/verify-weight` | ❌ | 👤 | ❌ | ❌ | 🤖 |
| POST `/api/sessions` | ❌ | 👤 | ❌ | ❌ | ❌ |
| GET `/api/sessions/{id}` | ❌ | 👤 | ❌ | ❌ | ❌ |
| POST `/api/sessions/{id}/end` | ❌ | 👤 | ❌ | ❌ | ❌ |
| GET `/api/orders/{id}` | ❌ | 👤 | ❌ | ✅ | ❌ |
| POST `/api/payments/initiate` | ❌ | 👤 | ❌ | ❌ | ❌ |
| POST `/api/payments/webhook` | 💳 | 💳 | 💳 | 💳 | 💳 |
| GET `/api/payments/{orderId}/status` | ❌ | 👤 | ❌ | ✅ | ❌ |
| POST `/api/trolleys` | ❌ | ❌ | ❌ | ✅ | ❌ |
| GET `/api/trolleys/{code}` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| PATCH `/api/trolleys/{id}/status` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| POST `/api/trolleys/{code}/heartbeat` | ❌ | ❌ | ❌ | ❌ | 🤖 |
| GET `/api/inventory` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| GET `/api/inventory/low-stock` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| PATCH `/api/inventory/{productId}/adjust` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| GET `/api/offers` | ❌ | ✅ | 🏪 | ✅ | ❌ |
| GET `/api/offers/{id}` | ❌ | ✅ | 🏪 | ✅ | ❌ |
| POST `/api/offers` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| PUT `/api/offers/{id}` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| DELETE `/api/offers/{id}` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| GET `/api/notifications` | ❌ | 👤 | ❌ | ❌ | ❌ |
| PATCH `/api/notifications/{id}/read` | ❌ | 👤 | ❌ | ❌ | ❌ |
| GET `/api/shopping-list` | ❌ | 👤 | ❌ | ❌ | ❌ |
| POST `/api/shopping-list` | ❌ | 👤 | ❌ | ❌ | ❌ |
| POST `/api/shopping-list/{listId}/items` | ❌ | 👤 | ❌ | ❌ | ❌ |
| PATCH `/api/shopping-list/items/{itemId}` | ❌ | 👤 | ❌ | ❌ | ❌ |
| GET `/api/recommendations/personalized` | ❌ | 👤 | ❌ | ❌ | ❌ |
| GET `/api/recommendations/fbt` | ❌ | ✅ | ❌ | ❌ | ❌ |
| GET `/api/recommendations/basic` | ❌ | ✅ | ❌ | ❌ | ❌ |
| POST `/api/recommendations/recompute` | ❌ | ❌ | ❌ | ✅ | ❌ |
| GET `/api/admin/trolleys` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| GET `/api/admin/products` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| POST `/api/admin/products/bulk-deactivate` | ❌ | ❌ | ❌ | ✅ | ❌ |
| GET `/api/admin/stock` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| GET `/api/admin/stock/low` | ❌ | ❌ | 🏪 | ✅ | ❌ |
| GET `/api/admin/orders` | ❌ | ❌ | 🏪 | ✅ | ❌ |

---

## 13. 401 vs 403

**401 — authentication missing or invalid**: missing JWT, malformed JWT, expired JWT, invalid refresh token, invalid device token, invalid webhook signature.

```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_TOKEN",
  "message": "Authentication is required"
}
```

**403 — identity valid, operation not permitted**: CUSTOMER creating a product, CUSTOMER accessing another customer's cart, STAFF accessing another store, STAFF bulk-deactivating products, DEVICE accessing another trolley.

```json
{
  "success": false,
  "data": null,
  "errorCode": "FORBIDDEN",
  "message": "You do not have permission to perform this operation"
}
```

---

## 14. Security Filter Configuration

Implement a Spring Security configuration with: CSRF handling, JWT authentication, role authorization, stateless REST security, authentication entry point, access-denied handler.

For a stateless REST API use `SessionCreationPolicy.STATELESS`. Do not use server-side HTTP sessions for normal API authentication.

---

## 15. JWT Requirements

Recommended claims:

```json
{
  "sub": "user-uuid",
  "role": "CUSTOMER",
  "iat": 0,
  "exp": 0
}
```

For STORE_STAFF, store association may be loaded from the database rather than trusted entirely from the JWT. Do not trust mutable authorization information from stale JWT claims when the application requires immediate revocation or store reassignment.

---

## 16. Password Security

- Passwords must never be stored in plaintext — use `BCryptPasswordEncoder`.
- Never return password hashes through API responses.
- Never log: password, accessToken, refreshToken, deviceToken, payment secrets, webhook secrets.

---

## 17. Device Token Security

- Treat device tokens as secrets.
- Do not expose device tokens through normal customer-facing API responses.
- If device provisioning returns a device credential: show it only during secure provisioning, do not repeatedly expose it, and ensure it can be revoked/rotated.

---

## 18. Method-Level Authorization

Use Spring Security method authorization where useful:

```java
@PreAuthorize("hasRole('ADMIN')")
```
for ADMIN-only operations, and

```java
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_STAFF')")
```
for shared management operations.

Method-level role checks are **not** a replacement for resource ownership — `@PreAuthorize("hasRole('CUSTOMER')")` does not prove `session.userId == authenticatedUserId`. Both checks are required.

---

## 19. Authorization Service

Create a reusable authorization component instead of duplicating security logic in every controller.

```java
AuthorizationService
```

Responsibilities:
- `isCustomerOwner(...)`
- `isStoreStaffForStore(...)`
- `isStoreStaffForProduct(...)`
- `isStoreStaffForInventory(...)`
- `isStoreStaffForOffer(...)`
- `isStoreStaffForTrolley(...)`
- `isStoreStaffForOrder(...)`
- `isDeviceForTrolley(...)`
- `isDeviceForSession(...)`

Example:

```java
authorizationService.requireCustomerOwnsSession(sessionId, authenticatedUserId);
```

If unauthorized: `throw ForbiddenException`.

---

## 20. Do Not Put Authorization Logic in DTOs

DTOs should represent data. Do not implement `request.getUserId().equals(...)` as the security mechanism. Authorization belongs in the security layer, the authorization service, or the service/business layer.

---

## 21. Do Not Trust Client-Supplied Values

- **Role:** never accept `{"role": "ADMIN"}` to determine authorization — the authenticated role must come from the JWT/security context plus trusted persisted user information.
- **Price:** cart/order calculations must use server-side product pricing — never authorize or calculate final payment using a client-submitted `{"price": 1}`.
- **Payment status:** never allow `{"status": "SUCCESS"}` from the customer app to mark an order paid — only verified payment gateway events may transition payment to `SUCCESS`.

---

## 22. Authorization Tests

Create automated tests for every protected endpoint.

**Customer**
- Can access own session / cannot access another session
- Can access own cart / cannot access another cart
- Can access own order / cannot access another order
- Can initiate own payment / cannot initiate another customer's payment

**Store Staff**
- Can access own store / cannot access another store
- Can modify own store product / cannot modify another store's product
- Can modify own inventory / cannot modify another store's inventory
- Can manage own store offers / cannot manage another store's offers
- Can manage own store trolley / cannot manage another store's trolley

**Admin**
- Can access Store A and Store B
- Can manage products, inventory, offers, trolleys
- Can recompute recommendations
- Can bulk deactivate products

**Device**
- Valid device token → allowed; invalid → 401
- Device A → Trolley A → allowed; Device A → Trolley B → rejected
- Device A → Session A → allowed; Device A → Session B → rejected

**Authentication**
- No token → 401; expired token → 401; malformed token → 401; valid token + wrong role → 403

---

## 23. Security Regression Tests

Before completing the implementation, explicitly test these attack scenarios — every one must be rejected:

1. Customer changes `userId` in query parameter.
2. Customer changes `sessionId`.
3. Customer changes `orderId`.
4. Customer changes `listId`.
5. Staff changes `storeId`.
6. Staff changes `productId` to another store.
7. Staff changes inventory `productId` to another store.
8. Staff changes `offerId` to another store.
9. Staff changes `trolleyId` to another store.
10. Device changes `trolleyCode`.
11. Device changes `sessionId`.
12. Customer submits payment `SUCCESS` manually.
13. Customer submits `ADMIN` role manually.
14. Staff submits `ADMIN` role manually.
15. Duplicate payment webhook.
16. Expired JWT.
17. Revoked refresh token.

---

## 24. Implementation Rules for Coding Agent

Before writing code, inspect: existing project structure, existing `SecurityConfig`, `User` entity, role implementation, JWT implementation, authentication services, and the `Product`/`Store`/`Trolley`/`Session`/`Cart`/`Order` relationships. Identify existing exception handling. Do not duplicate existing security infrastructure — reuse existing classes where correct. Then implement missing authorization incrementally.

**Do NOT:**
- Rewrite the entire backend.
- Change endpoint names unless required.
- Change request/response JSON contracts unless explicitly required.
- Remove existing functionality.

---

## 25. Required Deliverables

- `SecurityConfig`
- JWT authentication/filter integration
- Role authorization
- `AuthorizationService`
- Ownership checks
- Store-scope checks
- Device/trolley checks
- Payment webhook authentication
- 401 handler
- 403 handler
- Authorization tests

Suggested package layout:

```
security/
 ├── SecurityConfig
 ├── JwtAuthenticationFilter
 ├── JwtService
 ├── AuthenticationService
 ├── AuthorizationService
 ├── CustomAuthenticationEntryPoint
 ├── CustomAccessDeniedHandler
 └── DeviceAuthenticationService
```

Use the existing project package structure if one already exists.

---

## 26. Definition of Done

- [ ] All protected endpoints require authentication.
- [ ] Public endpoints remain accessible.
- [ ] CUSTOMER role is enforced.
- [ ] STORE_STAFF role is enforced.
- [ ] ADMIN role is enforced.
- [ ] DEVICE authentication is enforced.
- [ ] Customer ownership checks are implemented.
- [ ] Store isolation is implemented.
- [ ] Trolley/device binding is implemented.
- [ ] Session/device binding is implemented.
- [ ] Payment webhook signature verification is implemented.
- [ ] Payment status cannot be client-forged.
- [ ] User ID cannot be spoofed.
- [ ] Store ID cannot be spoofed.
- [ ] Role cannot be spoofed.
- [ ] Device/trolley identity cannot be spoofed.
- [ ] 401 responses are consistent.
- [ ] 403 responses are consistent.
- [ ] Authorization tests exist.
- [ ] Cross-user access tests pass.
- [ ] Cross-store access tests pass.
- [ ] Cross-device/trolley access tests pass.
- [ ] Existing API behavior remains intact.
- [ ] No secrets are logged.
- [ ] No passwords are returned.
- [ ] No unnecessary API contract changes were introduced.

---

## 27. Final Implementation Principle

```
WHO ARE YOU?
     ↓
WHAT ROLE DO YOU HAVE?
     ↓
WHAT RESOURCE ARE YOU ACCESSING?
     ↓
DO YOU OWN IT?
     ↓
DOES IT BELONG TO YOUR STORE?
     ↓
IF DEVICE: DOES THE DEVICE BELONG TO THIS TROLLEY?
     ↓
IS THE OPERATION CURRENTLY ALLOWED?
     ↓
ALLOW
```

Never implement authorization as role checking alone. The final system must enforce:

**RBAC + Resource Ownership + Store Isolation + Device/Trolley Binding + Payment Signature Verification**

as the authoritative CartRex authorization model.
