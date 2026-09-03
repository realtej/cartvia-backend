# CartRex — API Error & Validation Contract

## 1. Global Response Envelope

All endpoints return the same wrapper on success:

```json
{
  "success": true,
  "data": { },
  "errorCode": null,
  "message": null
}
```

`data` holds the endpoint-specific payload (object, array, or `null` for endpoints with no response body).

All endpoints return the same wrapper on failure:

```json
{
  "success": false,
  "data": null,
  "errorCode": "STRING (ErrorCode enum name)",
  "message": "string (human-readable)"
}
```

---

## 2. HTTP Status ↔ errorCode Matrix

| HTTP Status | errorCode | Meaning | Typical Trigger |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | Request body/params failed validation | `@Valid` failure, bad enum, out-of-range value, malformed business rule |
| 401 | `INVALID_TOKEN` | Auth token missing, malformed, invalid, or signature mismatch | Missing/garbled JWT, bad device token, bad webhook signature |
| 401 | `TOKEN_EXPIRED` | Token was valid but has expired | Expired access/refresh/reset token |
| 401 | `INVALID_CREDENTIALS` | Login credentials incorrect | Wrong username/password |
| 403 | `FORBIDDEN` | Authenticated identity lacks permission for this resource/operation | Cross-user, cross-store, cross-device access; wrong role |
| 404 | `PRODUCT_NOT_FOUND` | Product does not exist (or not in scope) | Bad `productId`/`barcode` |
| 404 | `USER_NOT_FOUND` | User does not exist | Bad `userId` |
| 404 | `TROLLEY_NOT_FOUND` | Trolley does not exist | Bad `trolleyCode`/`id` |
| 404 | `SESSION_NOT_FOUND` | Shopping session does not exist | Bad `sessionId` |
| 404 | `NOT_FOUND` | Generic resource not found (order, offer, notification, list item, etc.) | Bad id for a resource without its own dedicated code |
| 409 | `USERNAME_TAKEN` | Username already registered | Register with existing username |
| 409 | `EMAIL_TAKEN` | Email already registered | Register with existing email |
| 409 | `PHONE_TAKEN` | Phone already registered | Register with existing phone |
| 409 | `VALIDATION_ERROR` | Conflicting state (used for both field validation and state conflicts) | Duplicate barcode/trolley code, trolley already in active session, order not payable |
| 500 | `INTERNAL_ERROR` | Uncaught server exception | Any unhandled error |

> Note: `VALIDATION_ERROR` is reused across 400 (field-level) and 409 (state-conflict) responses in the existing contract. Preserve this as-is unless a dedicated conflict code is explicitly introduced.

---

## 3. Field Validation Error Format

For `400 VALIDATION_ERROR` responses caused by `@Valid`/Jakarta Validation failures, extend the base envelope with a `fieldErrors` array (additive — does not break existing consumers relying on `success`/`errorCode`/`message`):

```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": [
    {
      "field": "password",
      "constraint": "size",
      "rejectedValue": "abc",
      "message": "password must be between 6 and 100 characters"
    }
  ]
}
```

| Property | Description |
|---|---|
| `field` | Name of the offending field (dot-path for nested, e.g. `items[0].barcode`) |
| `constraint` | Short constraint identifier: `required`, `size`, `min`, `max`, `pattern`, `enum`, `email`, `positive` |
| `rejectedValue` | The value submitted (omit or mask for sensitive fields such as `password`) |
| `message` | Human-readable explanation |

---

## 4. Field Constraints by Resource

### 4.1 Auth

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `username` | register, login | string | required, max 50 |
| `name` | register | string | required, max 100 |
| `email` | register | string | optional, max 150, valid email format |
| `phone` | register | string | optional, max 15 |
| `password` | register, reset-password | string | required, min 6, max 100 |
| `gender` | register | string | optional, max 10 |
| `token` | reset-password | string | required |
| `refreshToken` | refresh | string | required |

Example — register violation:
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": [
    { "field": "password", "constraint": "size", "message": "password must be between 6 and 100 characters" }
  ]
}
```

### 4.2 Products

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `barcode` | create/update | string | required, unique per catalog |
| `name` | create/update | string | required |
| `description` | create/update | string | optional |
| `category` | create/update | string | optional |
| `price` | create/update | decimal | required, `>= 0` |
| `discountPct` | create/update | decimal | optional, `0–100` |
| `expectedWeightG` | create/update | decimal | optional, `>= 0` |
| `weightTolerancePct` | create/update | decimal | optional, `0–100` |
| `gstSlabPct` | create/update | decimal | optional, `0–100` |
| `imageUrl` | create/update | string | optional, valid URL |
| `storeId` | create/update | uuid | optional (nullable = store-agnostic catalog entry) |

Example — duplicate barcode (409):
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "A product with this barcode already exists"
}
```

### 4.3 Carts

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `barcode` | add item | string | required |
| `source` | add item(s) | enum | optional, one of `SCAN`, `MANUAL` |
| `items` | batch add | array | required, non-empty |
| `items[].clientTs` | batch add | string | required, ISO-8601 `LocalDateTime` (e.g. `2026-08-29T14:35:00`) |
| `quantity` | patch item | int | required, min 1 |
| `measuredWeightG` | verify-weight | decimal | required |
| `verificationStatus` | (response only) | enum | `PENDING`, `VERIFIED`, `MISMATCH` |

Example — empty batch:
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": [
    { "field": "items", "constraint": "size", "message": "items must not be empty" }
  ]
}
```

### 4.4 Shopping Sessions

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `trolleyCode` | create | string | required |
| `userId` | create | uuid | required |
| `status` | (response only) | enum | `ACTIVE`, `CHECKED_OUT`, `ABANDONED` |

Example — trolley already in use (409):
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Trolley is already in an active session"
}
```

### 4.5 Orders

| Field | Type | Constraint |
|---|---|---|
| `status` | enum (response only) | `PENDING`, `PAID`, `FAILED`, `CANCELLED` |
| `createdAt` | string (response only) | ISO-8601 datetime |

### 4.6 Payments

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `orderId` | initiate, webhook | uuid | required |
| `status` | webhook | enum | required, one of `SUCCESS`, `FAILED` |
| `signature` | webhook | string | required to pass signature validation |
| `upiTxnRef` | webhook | string | optional |
| `status` (response) | status lookup | enum | `INITIATED`, `SUCCESS`, `FAILED` |

Example — order not payable (409):
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Order is not in a payable state"
}
```

Example — bad webhook signature (401):
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_TOKEN",
  "message": "Invalid webhook signature"
}
```

### 4.7 Trolleys

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `trolleyCode` | create | string | required, unique |
| `storeId` | create | uuid | optional |
| `esp32Mac` | create | string | optional, MAC address format `^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$` (recommended) |
| `status` | patch status | enum | required, one of `INACTIVE`, `ACTIVE`, `IN_SESSION`, `MAINTENANCE` |
| `batteryPct` | heartbeat | int | optional, `0–100` |
| `rssi` | heartbeat | int | optional |

Example — duplicate trolley code (409):
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "A trolley with this code already exists"
}
```

Example — device token mismatch (401):
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_TOKEN",
  "message": "Device token does not match trolley code"
}
```

### 4.8 Inventory

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `storeId` | list, low-stock | uuid | required (query param) |
| `delta` | adjust | int | required, positive or negative (non-zero recommended) |
| `stockQty` | (response only) | int | `>= 0` |
| `lowStockThreshold` | (response only) | int | `>= 0` |

### 4.9 Offers

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `storeId` | create | uuid | required |
| `productId` | create/update | uuid | optional, `null` = store-wide offer |
| `title` | create/update | string | required |
| `discountPct` | create/update | decimal | required, `0–100` |
| `validFrom` | create/update | string | optional, ISO-8601 datetime |
| `validTo` | create/update | string | optional, ISO-8601 datetime, must be `>= validFrom` when both present (recommended) |
| `active` | create/update | boolean | optional, default `true` |

### 4.10 Notifications

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `userId` | list | uuid | required (query param) |
| `unreadOnly` | list | boolean | optional, default `false` |
| `type` | (response only) | string | free-form category |
| `read` | (response only) | boolean | — |

### 4.11 Shopping List

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `userId` | get/create list | uuid | required (query param) |
| `productId` | add item | uuid | optional — exactly one of `productId`/`customName` required |
| `customName` | add item | string | optional — exactly one of `productId`/`customName` required |
| `purchased` | patch item | boolean | required |

Example — both/neither productId & customName supplied (400):
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Provide exactly one of productId or customName"
}
```

### 4.12 Recommendations

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `userId` | personalized | uuid | required (query param) |
| `productId` | fbt | uuid | required (query param) |
| `category` | basic | string | optional (query param) |
| `reason` | (response only) | enum | `PERSONALIZED_CATEGORY_MATCH`, `BESTSELLER`, `FREQUENTLY_BOUGHT_TOGETHER` |

### 4.13 Admin

| Field | Endpoint(s) | Type | Constraint |
|---|---|---|---|
| `productIds` | bulk-deactivate | array\<uuid\> | required, non-empty |
| `storeId` | admin listings | uuid | optional (query param) |
| `status` | admin orders | enum | optional, one of `PENDING`, `PAID`, `FAILED`, `CANCELLED` |

---

## 5. Nullable Fields Reference

Fields explicitly nullable in responses:

- `Product.description`, `Product.category`, `Product.imageUrl`
- `Session.endedAt`
- `EndSession.orderId`, `EndSession.orderCode`
- `Trolley.lastSeenAt`
- `Offer.productId` (null = store-wide), `Offer.validFrom`, `Offer.validTo`
- `ShoppingListItem.productId` (null when added via `customName`), `ShoppingListItem.purchasedAt`

All other documented fields are non-null when present in a successful response.

---

## 6. Date/Time Formats

| Format | Used For | Example |
|---|---|---|
| ISO-8601 `LocalDateTime` (no timezone offset) | `clientTs`, `validFrom`, `validTo`, `createdAt`, `startedAt`, `endedAt`, `lastSeenAt`, `purchasedAt` | `2026-08-29T14:35:00` |

Recommendation: standardize on UTC `LocalDateTime` semantics server-side and document the assumed timezone (e.g. store-local or UTC) if not already fixed in the existing implementation. Do not silently change to `OffsetDateTime`/`Instant` without updating this contract and client parsers.

---

## 7. Enums Reference

| Enum | Values |
|---|---|
| `Role` | `CUSTOMER`, `STORE_STAFF`, `ADMIN` (+ `DEVICE` for device auth, not a `User.role` value) |
| `Cart source` | `SCAN`, `MANUAL` |
| `VerificationStatus` | `PENDING`, `VERIFIED`, `MISMATCH` |
| `SessionStatus` | `ACTIVE`, `CHECKED_OUT`, `ABANDONED` |
| `OrderStatus` | `PENDING`, `PAID`, `FAILED`, `CANCELLED` |
| `PaymentStatus` | `INITIATED`, `SUCCESS`, `FAILED` |
| `TrolleyStatus` | `INACTIVE`, `ACTIVE`, `IN_SESSION`, `MAINTENANCE` |
| `WebhookStatus` | `SUCCESS`, `FAILED` |
| `RecommendationReason` | `PERSONALIZED_CATEGORY_MATCH`, `BESTSELLER`, `FREQUENTLY_BOUGHT_TOGETHER` |
| `ErrorCode` | `VALIDATION_ERROR`, `USERNAME_TAKEN`, `EMAIL_TAKEN`, `PHONE_TAKEN`, `INVALID_CREDENTIALS`, `INVALID_TOKEN`, `TOKEN_EXPIRED`, `PRODUCT_NOT_FOUND`, `USER_NOT_FOUND`, `TROLLEY_NOT_FOUND`, `SESSION_NOT_FOUND`, `NOT_FOUND`, `FORBIDDEN`, `INTERNAL_ERROR` |

Invalid enum values submitted by a client must produce `400 VALIDATION_ERROR` with a `fieldErrors` entry (`constraint: "enum"`), not a silent default or `500`.

---

## 8. Pagination

Applies to: `GET /api/products`, `GET /api/admin/products`, `GET /api/admin/orders`.

| Param | Type | Default | Constraint |
|---|---|---|---|
| `page` | int | `0` | `>= 0` |
| `size` | int | `20` | `1–100` (recommended cap to prevent unbounded queries) |

Response shape (`Page<T>`):

```json
{
  "content": [ ],
  "totalElements": 0,
  "totalPages": 0,
  "number": 0,
  "size": 0
}
```

Requests with `size` above the recommended cap should be clamped server-side rather than rejected, unless stricter validation is explicitly desired — if rejected, use `400 VALIDATION_ERROR`.

---

## 9. Sorting & Filtering

Not explicitly defined in the current endpoint contracts beyond the query params already listed per endpoint. Current filterable query params by endpoint:

| Endpoint | Filters |
|---|---|
| `GET /api/products`, `GET /api/admin/products` | `storeId`, `search`, `category` |
| `GET /api/offers` | `storeId`, `productId` |
| `GET /api/notifications` | `userId`, `unreadOnly` |
| `GET /api/inventory`, `GET /api/inventory/low-stock`, `GET /api/admin/stock`, `GET /api/admin/stock/low` | `storeId` (required) |
| `GET /api/admin/trolleys` | `storeId` |
| `GET /api/admin/orders` | `storeId`, `status` |
| `GET /api/recommendations/basic` | `category` |

No dedicated `sort` query parameter exists in the current contract. If sorting is added, use Spring's conventional `sort=field,direction` (e.g. `sort=price,desc`) and validate `field` against an allow-list per resource to prevent arbitrary/unsafe property exposure.

---

## 10. Search Behavior

`search` (on `GET /api/products` / `GET /api/admin/products`):

- Optional, free-text.
- Recommended: case-insensitive partial match against `name` (and optionally `description`, `barcode`).
- No minimum length enforced by default; recommend trimming and ignoring empty-string search terms (treat as "no filter") rather than erroring.
- Combined with `category`/`storeId` filters via logical AND.

`GET /api/products/barcode/{barcode}` performs an **exact match** lookup, not a search — no partial matching.

---

## 11. Full Worked Example — Validation Error

**Request:** `POST /api/products`
```json
{
  "barcode": "",
  "name": "Milk 1L",
  "price": -5,
  "discountPct": 150
}
```

**Response — 400**
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": [
    { "field": "barcode", "constraint": "required", "rejectedValue": "", "message": "barcode is required" },
    { "field": "price", "constraint": "min", "rejectedValue": -5, "message": "price must be >= 0" },
    { "field": "discountPct", "constraint": "max", "rejectedValue": 150, "message": "discountPct must be between 0 and 100" }
  ]
}
```

---

## 12. Full Worked Example — Not Found vs Forbidden

**Request:** `GET /api/sessions/{id}` for a session owned by a different customer.

**Response — 403** (identity valid, resource not owned — do not leak existence via 404 in this case per the authorization spec; project must pick one behavior and apply it consistently)
```json
{
  "success": false,
  "data": null,
  "errorCode": "FORBIDDEN",
  "message": "You do not have permission to perform this operation"
}
```

**Request:** `GET /api/sessions/{id}` for a session that does not exist at all.

**Response — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "SESSION_NOT_FOUND",
  "message": "Session not found"
}
```

---

## 13. Consistency Rules

1. Every 4xx/5xx response uses the exact same envelope shape (`success`, `data`, `errorCode`, `message`), with `fieldErrors` as an additive array only present on `VALIDATION_ERROR` (400) responses.
2. `errorCode` is always one of the enumerated `ErrorCode` values — never a raw exception class name or stack trace.
3. `message` is safe to display to end users; it must never leak internal details (SQL, stack traces, class names).
4. `data` is always `null` on error responses.
5. Uncaught exceptions always map to `500 INTERNAL_ERROR` with a generic message — never expose the underlying exception message to the client.
