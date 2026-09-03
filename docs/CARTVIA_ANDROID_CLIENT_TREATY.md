# CartVia Client–Backend Treaty

**Version:** 1.0  
**Backend:** `cartvia-backend` (Spring Boot)  
**Primary client:** Android (CartVia customer app)  
**Status:** Binding integration contract

This document defines the rules, connection parameters, and behavioral agreements between the **CartVia Android app** and the **cartvia-backend** API. Android Studio implementations MUST follow this treaty to remain compatible with the server.

For exhaustive JSON examples and field-level validation, see:

- `doc-agent/CartRex-API-JSON-Bodies.md`
- `doc-agent/CartRex-API-Error-Validation.md`
- `doc-agent/CartRex-Authorization-Spec.md`

---

## 1. Purpose

The treaty answers four questions for the Android client:

1. **Where** does the app connect?
2. **How** does it authenticate?
3. **What** REST and WebSocket contracts must it honor?
4. **What** must the app never do (security and state rules)?

The Android app acts exclusively as a **CUSTOMER** client. It does not provision trolleys, manage inventory, or call admin/staff endpoints.

---

## 2. Connection Configuration

### 2.1 Base URL

| Environment | Base URL | Notes |
|---|---|---|
| Android Emulator → dev machine | `http://10.0.2.2:8080` | `10.0.2.2` is the emulator alias for host `localhost` |
| Physical device → dev machine | `http://<LAN-IP>:8080` | Use the PC's Wi‑Fi IP (e.g. `192.168.1.42`) |
| Production | `https://<your-domain>` | HTTPS required in production |

All REST paths are rooted at:

```
{BASE_URL}/api/...
```

Context path is `/` (no extra prefix).

### 2.2 Server Properties (from `application.properties`)

| Property | Default | Android impact |
|---|---|---|
| `server.port` | `8080` | Port in base URL |
| `server.address` | `0.0.0.0` | Server listens on all interfaces (LAN access works) |
| `cartvia.jwt.access-token-expiration-minutes` | `60` | Refresh access token before expiry |
| `cartvia.jwt.refresh-token-expiration-days` | `30` | Persist refresh token securely |
| `cartvia.cors.allowed-origins` | `http://localhost:8080,http://10.0.2.2:8080` | Native Android is not browser-CORS-bound; ensure dev server allows your test origins if using a WebView |
| `cartvia.websocket.endpoint` | `/ws` | WebSocket STOMP endpoint |
| `cartvia.payment.mock-enabled` | `true` (dev) | Mock UPI links in dev; real gateway in prod |
| `cartvia.payment.upi-payee-address` | `cartvia@razorpay` | Shown in UPI deep link |
| `cartvia.payment.upi-payee-name` | `CartVia` | Payee display name in UPI deep link |

### 2.3 Required HTTP Headers (all authenticated REST calls)

```
Content-Type: application/json
Accept: application/json
Authorization: Bearer <accessToken>
```

The Android app MUST attach the JWT access token on every protected REST request. Missing or expired tokens receive `401`.

---

## 3. Authentication Treaty

### 3.1 Roles

The Android app operates as role **`CUSTOMER`** only.

| Role | Android app |
|---|---|
| `CUSTOMER` | ✅ Yes — primary client |
| `STORE_STAFF` | ❌ No |
| `ADMIN` | ❌ No |
| `DEVICE` | ❌ No — reserved for ESP32 trolley firmware |

### 3.2 Public Endpoints (no JWT)

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/auth/register` | Create account |
| `POST` | `/api/auth/login` | Obtain tokens |
| `POST` | `/api/auth/refresh` | Renew access token |
| `POST` | `/api/auth/forgot-password` | Request reset |
| `POST` | `/api/auth/reset-password` | Complete reset |

### 3.3 Login Flow (Android MUST implement)

```
1. POST /api/auth/login  { username, password }
2. Store accessToken + refreshToken securely (EncryptedSharedPreferences / Keystore)
3. Attach Authorization: Bearer <accessToken> on all subsequent calls
4. On 401 TOKEN_EXPIRED → POST /api/auth/refresh { refreshToken }
5. On refresh failure → redirect user to login screen
```

**JWT claims (server-issued, do not forge):**

```json
{
  "sub": "<user-uuid>",
  "role": "CUSTOMER",
  "iat": 0,
  "exp": 0
}
```

### 3.4 Token Storage Rules

| Data | Storage | Rule |
|---|---|---|
| `accessToken` | Memory + secure storage | Never log, never expose in UI |
| `refreshToken` | Encrypted storage | Rotate on refresh |
| `password` | Never persist | Clear from memory after login |
| `userId` | Derived from JWT `sub` or login response | Do not let user edit |

### 3.5 Identity Rule (critical)

The server derives the authenticated user from the JWT. The Android app MUST NOT:

- Send a different `userId` in query/body and expect it to work
- Trust a locally stored `userId` over the JWT
- Include `role` in any request body

For `POST /api/sessions`, `userId` in the body is **optional**. If sent, it MUST equal the JWT user or the server returns `403 FORBIDDEN`.

---

## 4. Global Response Envelope

Every REST response uses this shape:

**Success:**
```json
{
  "success": true,
  "data": { },
  "errorCode": null,
  "message": null
}
```

**Error:**
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_TOKEN",
  "message": "Authentication is required",
  "fieldErrors": []
}
```

`fieldErrors` appears only on `400 VALIDATION_ERROR`.

### 4.1 Android Parsing Rules

1. Always check `success` before reading `data`.
2. Map `errorCode` to user-facing messages (never show raw stack traces).
3. On `401 INVALID_TOKEN` or `TOKEN_EXPIRED` → attempt refresh once, then logout.
4. On `403 FORBIDDEN` → show permission error; do not retry blindly.
5. Treat `data` as `null` on all error responses.

### 4.2 Date/Time Format

All datetime fields use **ISO-8601 local datetime without offset**:

```
2026-08-29T14:35:00
```

Parse as UTC unless the product explicitly documents store-local time.

---

## 5. Customer REST Endpoints (Android Scope)

These are the endpoints the Android app is allowed to call.

### 5.1 Authentication

| Method | Path | When |
|---|---|---|
| `POST` | `/api/auth/register` | Onboarding |
| `POST` | `/api/auth/login` | Login |
| `POST` | `/api/auth/refresh` | Token renewal |
| `POST` | `/api/auth/forgot-password` | Password recovery |
| `POST` | `/api/auth/reset-password` | Password recovery |

### 5.2 Products (read-only)

| Method | Path | When |
|---|---|---|
| `GET` | `/api/products?storeId=&search=&category=&page=&size=` | Browse catalog |
| `GET` | `/api/products/{id}` | Product detail |
| `GET` | `/api/products/barcode/{barcode}?storeId=` | Barcode lookup |

### 5.3 Shopping Session (trolley binding)

| Method | Path | When |
|---|---|---|
| `POST` | `/api/sessions` | After scanning trolley QR |
| `GET` | `/api/sessions/{id}` | Poll / resume session |
| `POST` | `/api/sessions/{id}/end` | Checkout |

**Create session request:**
```json
{
  "trolleyCode": "TRLY-0042",
  "userId": null
}
```

Omit `userId` or set it to the authenticated user's UUID.

**Trolley QR payload format (scanned from physical trolley):**
```
CARTREX:TROLLEY:<trolleyCode>:<trolleyUuid>
```

Example: `CARTREX:TROLLEY:TRLY-0042:8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e`

Android MUST parse `trolleyCode` from this string and send it in `POST /api/sessions`.

### 5.4 Cart

| Method | Path | When |
|---|---|---|
| `GET` | `/api/carts/{sessionId}` | Display cart |
| `POST` | `/api/carts/{sessionId}/items` | Manual add (barcode) |
| `PATCH` | `/api/carts/{sessionId}/items/{productId}` | Change quantity |
| `DELETE` | `/api/carts/{sessionId}/items/{productId}` | Remove item |

> **Note:** `POST .../items/batch` and `POST .../verify-weight` are used by the ESP32 device, not the Android app. The app receives those events over WebSocket.

### 5.5 Orders & Payments

| Method | Path | When |
|---|---|---|
| `GET` | `/api/orders/{id}` | Order detail after checkout |
| `POST` | `/api/payments/initiate` | Start UPI payment |
| `GET` | `/api/payments/{orderId}/status` | Poll payment status |
| `GET` | `/api/receipts/{orderId}` | Digital receipt |

**Payment initiate request:**
```json
{ "orderId": "<uuid>" }
```

**Payment initiate response (`data`):**
```json
{
  "paymentId": "<uuid>",
  "orderId": "<uuid>",
  "amount": 195.30,
  "status": "INITIATED",
  "upiDeepLink": "upi://pay?pa=...",
  "qrPayload": "MOCKQR|pa=..."
}
```

Android MUST open `upiDeepLink` via an Android Intent (`ACTION_VIEW`). Poll `GET /api/payments/{orderId}/status` until `status` is `SUCCESS` or `FAILED`, or listen for `PAYMENT_COMPLETED` on WebSocket.

### 5.6 Offers, Notifications, Shopping List, Recommendations

| Method | Path | When |
|---|---|---|
| `GET` | `/api/offers?storeId=&productId=` | Promotions |
| `GET` | `/api/offers/{id}` | Offer detail |
| `GET` | `/api/notifications?userId=&unreadOnly=` | Alerts (weight mismatch, etc.) |
| `PATCH` | `/api/notifications/{id}/read` | Mark read |
| `GET` | `/api/shopping-list?userId=` | Saved list |
| `POST` | `/api/shopping-list?userId=` | Create list |
| `POST` | `/api/shopping-list/{listId}/items` | Add item |
| `PATCH` | `/api/shopping-list/items/{itemId}` | Mark purchased |
| `GET` | `/api/recommendations/personalized?userId=` | Personalized picks |
| `GET` | `/api/recommendations/fbt?productId=` | Frequently bought together |
| `GET` | `/api/recommendations/basic?category=` | Category bestsellers |

For endpoints with `userId` query param: pass the authenticated user's UUID only.

---

## 6. End-to-End Shopping Flow (Android)

This is the canonical customer journey the Android app MUST implement.

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│ Login/Register│───▶│ Scan Trolley │───▶│ POST /sessions  │
└─────────────┘     │ QR Code      │     └────────┬────────┘
                    └──────────────┘              │
                                                  ▼
                    ┌──────────────────────────────────────────┐
                    │ Connect WebSocket + subscribe to session │
                    └────────────────────┬─────────────────────┘
                                         │
         ┌───────────────────────────────┼───────────────────────────────┐
         ▼                               ▼                               ▼
  ESP32 scans item              App shows live cart              Weight events arrive
  (device REST)                 (GET /carts or WS)               (WS WEIGHT_*)
         │                               │                               │
         └───────────────────────────────┴───────────────────────────────┘
                                         │
                                         ▼
                              POST /sessions/{id}/end
                                         │
                                         ▼
                              POST /payments/initiate
                                         │
                          ┌──────────────┴──────────────┐
                          ▼                             ▼
                   Open UPI deep link          Poll status / WS PAYMENT_COMPLETED
                          │                             │
                          └──────────────┬──────────────┘
                                         ▼
                              GET /receipts/{orderId}
```

### 6.1 Session States

| `SessionStatus` | Android behavior |
|---|---|
| `ACTIVE` | Show cart UI, listen to WebSocket |
| `CHECKED_OUT` | Session ended; show order/payment screen |
| `ABANDONED` | Clear local session state; prompt re-scan |

### 6.2 Order States

| `OrderStatus` | Android behavior |
|---|---|
| `PENDING` | Allow payment initiation |
| `PAID` | Show receipt / success |
| `FAILED` | Show retry / support |
| `CANCELLED` | Show cancelled state |

### 6.3 Payment States

| `PaymentStatus` | Android behavior |
|---|---|
| `INITIATED` | UPI app opened; poll or wait for WS |
| `SUCCESS` | Navigate to receipt |
| `FAILED` | Show failure; allow retry if order still `PENDING` |

---

## 7. WebSocket Real-Time Treaty

The backend uses **STOMP over native WebSocket** (no SockJS). The Android app MUST use a STOMP client (e.g. OkHttp WebSocket + STOMP library).

### 7.1 Connection URL

**Customer (Android):**
```
ws://<host>:<port>/ws?token=<accessToken>
```

| Parameter | Required | Value |
|---|---|---|
| `token` | Yes (customer) | Same JWT access token as REST `Authorization` header |

Handshake is rejected with **HTTP 401** if the token is missing, malformed, or expired. Refresh the JWT before reconnecting.

> Device firmware uses `?deviceToken=...&trolleyCode=...` instead. The Android app does NOT use device tokens.

### 7.2 STOMP Broker Configuration

| Setting | Value |
|---|---|
| Application prefix | `/app` |
| Topic prefix | `/topic` |
| User prefix | `/user` |
| Queue prefix | `/queue` |

### 7.3 Subscriptions (Android MUST subscribe after session start)

| Topic | Purpose |
|---|---|
| `/topic/sessions/{sessionId}` | Cart, weight, checkout, payment, session events |
| `/topic/trolleys/{trolleyCode}` | Optional: trolley online/offline status |

Replace `{sessionId}` and `{trolleyCode}` with values from `POST /api/sessions` response.

### 7.4 Event Envelope

Every message on a topic is a `WebSocketEvent`:

```json
{
  "type": "CART_UPDATED",
  "data": { },
  "timestamp": "2026-08-29T14:35:00"
}
```

Android MUST switch on `type` and parse `data` according to the table below.

### 7.5 Event Types & `data` Shapes

| `type` | When fired | `data` shape |
|---|---|---|
| `PRODUCT_SCANNED` | ESP32 scanned a barcode | `{ productId, name, barcode }` |
| `PRODUCT_ADDED` | Item added to cart | `CartItemDto` |
| `PRODUCT_REMOVED` | Item removed | `productId` (UUID string) |
| `CART_UPDATED` | Cart total changed | `BigDecimal` total or full cart response |
| `WEIGHT_UPDATED` | Scale reading received | `measuredWeightG` (number) |
| `WEIGHT_VALIDATION_RESULT` | Weight check completed | `{ productId, verificationStatus, measuredWeightG, expectedWeightG }` |
| `CHECKOUT_STARTED` | Session ending | `sessionId` (UUID) |
| `SESSION_UPDATED` | Session fields changed | `SessionResponse` |
| `PAYMENT_COMPLETED` | Payment succeeded | `{ orderId, paymentId, status, amount }` |
| `TROLLEY_CONNECTED` | Device heartbeat received | `TrolleyResponse` |
| `TROLLEY_DISCONNECTED` | Heartbeat timeout (180s default) | `TrolleyResponse` |

### 7.6 `verificationStatus` Values

| Value | Android UI |
|---|---|
| `PENDING` | Waiting for weight check |
| `VERIFIED` | OK — no action needed |
| `MISMATCH` | Show warning; prompt user to verify item |

### 7.7 WebSocket Lifecycle Rules

1. Connect WebSocket **after** `POST /api/sessions` succeeds.
2. Subscribe to `/topic/sessions/{sessionId}` immediately.
3. On JWT refresh, **reconnect** WebSocket with the new token.
4. On disconnect, exponential backoff reconnect (max 30s).
5. On `CHECKOUT_STARTED` or `SESSION_UPDATED` with `CHECKED_OUT`, unsubscribe and close WebSocket.
6. WebSocket failures MUST NOT block REST operations (cart/checkout still work via polling).

---

## 8. Error Code Treaty

Android MUST handle these `errorCode` values:

| HTTP | `errorCode` | Android action |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Show field errors or message |
| 401 | `INVALID_TOKEN` | Refresh token or logout |
| 401 | `TOKEN_EXPIRED` | Refresh token |
| 401 | `INVALID_CREDENTIALS` | Show login error |
| 403 | `FORBIDDEN` | Show permission denied |
| 404 | `PRODUCT_NOT_FOUND` | Barcode not in catalog |
| 404 | `TROLLEY_NOT_FOUND` | Invalid QR scan |
| 404 | `SESSION_NOT_FOUND` | Clear session; re-scan trolley |
| 404 | `NOT_FOUND` | Generic not found |
| 409 | `USERNAME_TAKEN` / `EMAIL_TAKEN` / `PHONE_TAKEN` | Registration error |
| 409 | `VALIDATION_ERROR` | Conflict (trolley in use, order not payable) |
| 500 | `INTERNAL_ERROR` | Generic retry message |

---

## 9. Security Obligations (Android)

The Android app agrees to the following rules. Violations break the integration contract.

### 9.1 MUST

- Store tokens in Android Keystore / EncryptedSharedPreferences
- Use HTTPS in production
- Derive `userId` from JWT, not from user input
- Use server prices from API responses (never compute checkout totals locally)
- Wait for `PAYMENT_COMPLETED` or `GET /payments/{orderId}/status` before showing "Paid"
- Parse trolley QR using the `CARTREX:TROLLEY:...` format
- Send `Content-Type: application/json` on all POST/PATCH/PUT bodies

### 9.2 MUST NOT

- Call staff/admin/device endpoints
- Send `X-Device-Token` header (device-only)
- Forge payment success (`status: SUCCESS` is webhook-only)
- Send another user's `userId`, `sessionId`, or `orderId`
- Log access tokens, refresh tokens, or passwords
- Cache and replay another customer's cart or order data
- Assume WebSocket delivery is guaranteed (always support REST fallback polling)

---

## 10. Android Implementation Checklist

Use this as a merge gate before connecting to the real backend.

### Networking
- [ ] Configurable `BASE_URL` (BuildConfig / `local.properties`)
- [ ] OkHttp/Retrofit with `Authorization` interceptor
- [ ] Token refresh interceptor on `401 TOKEN_EXPIRED`
- [ ] Global `ApiResponse<T>` deserializer matching server envelope

### Auth
- [ ] Register, login, logout flows
- [ ] Secure token persistence
- [ ] Auto-refresh before expiry (optional proactive refresh at 80% lifetime)

### Shopping
- [ ] QR scanner → parse `CARTREX:TROLLEY:<code>:<uuid>`
- [ ] `POST /api/sessions` → store `sessionId`, `cartId`, `storeId`
- [ ] Cart screen with `GET /api/carts/{sessionId}`
- [ ] Quantity update / remove via PATCH / DELETE
- [ ] Checkout via `POST /api/sessions/{id}/end`

### Real-time
- [ ] STOMP WebSocket client
- [ ] Connect with `?token=<accessToken>`
- [ ] Subscribe `/topic/sessions/{sessionId}`
- [ ] Handle all 11 `WebSocketEventType` values
- [ ] Reconnect on token refresh / network loss

### Payment
- [ ] `POST /api/payments/initiate`
- [ ] Open `upiDeepLink` via Intent
- [ ] Poll payment status OR listen for `PAYMENT_COMPLETED`
- [ ] `GET /api/receipts/{orderId}` on success

### Error UX
- [ ] Map all `errorCode` values to user messages
- [ ] Handle `409` trolley-already-in-session gracefully
- [ ] Handle `WEIGHT_VALIDATION_RESULT` mismatch in UI

---

## 11. Emulator & Device Testing Matrix

| Scenario | Base URL | WebSocket URL |
|---|---|---|
| Emulator → local backend | `http://10.0.2.2:8080` | `ws://10.0.2.2:8080/ws?token=...` |
| Phone → PC on same Wi‑Fi | `http://192.168.x.x:8080` | `ws://192.168.x.x:8080/ws?token=...` |
| Production | `https://api.cartvia.com` | `wss://api.cartvia.com/ws?token=...` |

Ensure the backend is started with `server.address=0.0.0.0` (default) so LAN devices can connect.

---

## 12. Enum Reference (Android models)

```kotlin
enum class Role { CUSTOMER, STORE_STAFF, ADMIN }

enum class SessionStatus { ACTIVE, CHECKED_OUT, ABANDONED }

enum class OrderStatus { PENDING, PAID, FAILED, CANCELLED }

enum class PaymentStatus { INITIATED, SUCCESS, FAILED }

enum class VerificationStatus { PENDING, VERIFIED, MISMATCH }

enum class WebSocketEventType {
    TROLLEY_CONNECTED, TROLLEY_DISCONNECTED,
    PRODUCT_SCANNED, PRODUCT_ADDED, PRODUCT_REMOVED,
    CART_UPDATED, WEIGHT_UPDATED, WEIGHT_VALIDATION_RESULT,
    CHECKOUT_STARTED, PAYMENT_COMPLETED, SESSION_UPDATED
}
```

---

## 13. Versioning & Compatibility

| Rule | Detail |
|---|---|
| API prefix | `/api` — stable |
| Envelope | `ApiResponse<T>` — stable; do not parse bare objects |
| Breaking changes | Require treaty version bump |
| New WebSocket events | Android should ignore unknown `type` values |
| New optional JSON fields | Android must tolerate unknown fields (use `@JsonIgnoreProperties(ignoreUnknown = true)`) |

---

## 14. Quick Reference — Retrofit Setup (Kotlin)

```kotlin
// BuildConfig
const val BASE_URL = "http://10.0.2.2:8080/"

// Interceptor
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = chain.request().newBuilder()
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}

// WebSocket (conceptual)
// ws://10.0.2.2:8080/ws?token=$accessToken
// STOMP SUBSCRIBE id:sub-0 destination:/topic/sessions/$sessionId
```

---

## 15. Treaty Acceptance

An Android build is **treaty-compliant** when:

1. It connects only to agreed base URLs and uses the global response envelope.
2. It authenticates exclusively via JWT as `CUSTOMER`.
3. It follows the shopping flow: scan → session → WebSocket → checkout → payment → receipt.
4. It subscribes to the correct STOMP topics and handles all documented event types.
5. It never impersonates another user, forges payment state, or calls device/admin endpoints.
6. It handles all documented error codes with correct retry/logout behavior.

**Backend source of truth:** `src/main/resources/application.properties` and controller implementations under `src/main/java/com/cartvia/cartvia_backend/`.

**Treaty version:** 1.0 — aligned with `cartvia-backend` as of September 2026.
