# CartRex — Full JSON Request/Response Bodies (All 52 Endpoints)

Concrete, realistic JSON payloads for every endpoint — ready to paste into Postman, Spring `@RequestBody` test fixtures, or `MockMvc` tests. All UUIDs, timestamps, and values below are sample data; replace with real values in actual use.

Every success response is wrapped in the global envelope:
```json
{ "success": true, "data": { }, "errorCode": null, "message": null }
```
Below, only the `data` payload is shown for brevity — wrap it as above when implementing.

---

## 1. AUTHENTICATION

### 1.1 POST /api/auth/register

**Request**
```json
{
  "username": "priya_shah",
  "name": "Priya Shah",
  "email": "priya.shah@example.com",
  "phone": "9876543210",
  "password": "Str0ngPass!",
  "gender": "F"
}
```

**Response — 201**
```json
{
  "success": true,
  "data": {
    "userId": "b3f1a2e0-6c4d-4a9b-9f3e-1a2b3c4d5e6f"
  },
  "errorCode": null,
  "message": null
}
```

**Error — 409 (username taken)**
```json
{
  "success": false,
  "data": null,
  "errorCode": "USERNAME_TAKEN",
  "message": "Username 'priya_shah' is already taken"
}
```

**Error — 400 (validation)**
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

---

### 1.2 POST /api/auth/login

**Request**
```json
{
  "username": "priya_shah",
  "password": "Str0ngPass!"
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJiM2YxYTJlMC02YzRkLTRhOWItOWYzZS0xYTJiM2M0ZDVlNmYiLCJyb2xlIjoiQ1VTVE9NRVIiLCJpYXQiOjE3NTY0NTYwMDAsImV4cCI6MTc1NjQ1OTYwMH0.6H5F8...signature",
    "refreshToken": "d290f1ee-6c54-4b01-90e6-d701748f0851",
    "user": {
      "id": "b3f1a2e0-6c4d-4a9b-9f3e-1a2b3c4d5e6f",
      "username": "priya_shah",
      "name": "Priya Shah",
      "email": "priya.shah@example.com",
      "phone": "9876543210",
      "role": "CUSTOMER"
    }
  },
  "errorCode": null,
  "message": null
}
```

**Error — 401**
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_CREDENTIALS",
  "message": "Invalid username or password"
}
```

---

### 1.3 POST /api/auth/refresh

**Request**
```json
{
  "refreshToken": "d290f1ee-6c54-4b01-90e6-d701748f0851"
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJiM2YxYTJlMCIsInJvbGUiOiJDVVNUT01FUiIsImlhdCI6MTc1NjQ1OTYwMCwiZXhwIjoxNzU2NDYzMjAwfQ.newSig...",
  },
  "errorCode": null,
  "message": null
}
```

**Error — 401**
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_TOKEN",
  "message": "Invalid or expired refresh token"
}
```

---

### 1.4 POST /api/auth/forgot-password

**Request**
```json
{
  "email": "priya.shah@example.com",
  "phone": null
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "resetTokenSent": true
  },
  "errorCode": null,
  "message": null
}
```

---

### 1.5 POST /api/auth/reset-password

**Request**
```json
{
  "token": "5f4dcc3b5aa765d61d8327deb882cf99",
  "newPassword": "NewStr0ngPass!"
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "success": true
  },
  "errorCode": null,
  "message": null
}
```

**Error — 401**
```json
{
  "success": false,
  "data": null,
  "errorCode": "TOKEN_EXPIRED",
  "message": "Invalid or expired reset token"
}
```

---

## 2. PRODUCTS

### 2.1 GET /api/products?storeId=...&search=milk&category=Dairy&page=0&size=20

**Request**: no body.

**Response — 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
        "barcode": "8901234567890",
        "name": "Amul Toned Milk 1L",
        "description": "Fresh toned milk, 3% fat",
        "category": "Dairy",
        "price": 62.00,
        "discountPct": 5.00,
        "expectedWeightG": 1030.00,
        "weightTolerancePct": 3.00,
        "imageUrl": "https://cdn.cartrex.app/products/amul-milk-1l.jpg",
        "inStock": true
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20
  },
  "errorCode": null,
  "message": null
}
```

---

### 2.2 GET /api/products/{id}

**Response — 200**
```json
{
  "success": true,
  "data": {
    "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
    "barcode": "8901234567890",
    "name": "Amul Toned Milk 1L",
    "description": "Fresh toned milk, 3% fat",
    "category": "Dairy",
    "price": 62.00,
    "discountPct": 5.00,
    "expectedWeightG": 1030.00,
    "weightTolerancePct": 3.00,
    "imageUrl": "https://cdn.cartrex.app/products/amul-milk-1l.jpg",
    "inStock": true
  },
  "errorCode": null,
  "message": null
}
```

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
}
```

---

### 2.3 GET /api/products/barcode/{barcode}?storeId=...

**Response — 200**: identical shape to 2.2.

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "PRODUCT_NOT_FOUND",
  "message": "Product not found for barcode 8901234567890"
}
```

---

### 2.4 POST /api/products (ADMIN, STORE_STAFF)

**Request**
```json
{
  "barcode": "8901234567890",
  "name": "Amul Toned Milk 1L",
  "description": "Fresh toned milk, 3% fat",
  "category": "Dairy",
  "price": 62.00,
  "discountPct": 5.00,
  "imageUrl": "https://cdn.cartrex.app/products/amul-milk-1l.jpg",
  "expectedWeightG": 1030.00,
  "weightTolerancePct": 3.00,
  "gstSlabPct": 5.00,
  "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d"
}
```

**Response — 201**: same shape as 2.2's `data`.

**Error — 409**
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "A product with this barcode already exists"
}
```

---

### 2.5 PUT /api/products/{id} (ADMIN, STORE_STAFF)

**Request**: same shape as 2.4.
```json
{
  "barcode": "8901234567890",
  "name": "Amul Toned Milk 1L (Pack of 1)",
  "description": "Fresh toned milk, 3% fat",
  "category": "Dairy",
  "price": 64.00,
  "discountPct": 0.00,
  "imageUrl": "https://cdn.cartrex.app/products/amul-milk-1l.jpg",
  "expectedWeightG": 1030.00,
  "weightTolerancePct": 3.00,
  "gstSlabPct": 5.00,
  "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d"
}
```

**Response — 200**: same shape as 2.2's `data`.

---

### 2.6 DELETE /api/products/{id} (ADMIN, STORE_STAFF)

**Request**: no body.

**Response — 200**
```json
{ "success": true, "data": null, "errorCode": null, "message": null }
```

---

## 3. CARTS

### 3.1 POST /api/carts/{sessionId}/items

**Request**
```json
{
  "barcode": "8901234567890",
  "source": "SCAN"
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "cartId": "2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e",
    "item": {
      "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
      "name": "Amul Toned Milk 1L",
      "quantity": 1,
      "unitPrice": 62.00
    },
    "cartTotal": 62.00,
    "recommendations": [
      { "productId": "3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f", "name": "Amul Butter 100g", "reason": "FREQUENTLY_BOUGHT_TOGETHER" }
    ]
  },
  "errorCode": null,
  "message": null
}
```

**Error — 403**
```json
{
  "success": false,
  "data": null,
  "errorCode": "FORBIDDEN",
  "message": "Device token is not authorized for this session's trolley"
}
```

---

### 3.2 POST /api/carts/{sessionId}/items/batch

**Request**
```json
{
  "items": [
    { "barcode": "8901234567890", "source": "SCAN", "clientTs": "2026-08-29T14:35:00" },
    { "barcode": "8901234500011", "source": "SCAN", "clientTs": "2026-08-29T14:36:12" }
  ]
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "cartId": "2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e",
    "sessionId": "4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a",
    "totalAmount": 137.50,
    "items": [
      {
        "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
        "name": "Amul Toned Milk 1L",
        "quantity": 1,
        "unitPrice": 62.00,
        "verificationStatus": "PENDING"
      },
      {
        "productId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
        "name": "Britannia Brown Bread 400g",
        "quantity": 1,
        "unitPrice": 45.00,
        "verificationStatus": "PENDING"
      }
    ]
  },
  "errorCode": null,
  "message": null
}
```

---

### 3.3 GET /api/carts/{sessionId}

**Response — 200**: same shape as 3.2's `data`.

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "SESSION_NOT_FOUND",
  "message": "Session not found"
}
```

---

### 3.4 PATCH /api/carts/{sessionId}/items/{productId}

**Request**
```json
{
  "quantity": 3
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "cartId": "2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e",
    "item": {
      "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
      "name": "Amul Toned Milk 1L",
      "quantity": 3,
      "unitPrice": 62.00
    },
    "cartTotal": 186.00,
    "recommendations": []
  },
  "errorCode": null,
  "message": null
}
```

---

### 3.5 DELETE /api/carts/{sessionId}/items/{productId}

**Request**: no body.

**Response — 200**: same shape as 3.2's `data`, with the item removed and totals recalculated.

---

### 3.6 POST /api/carts/{sessionId}/items/{productId}/verify-weight

**Request**
```json
{
  "measuredWeightG": 1015.00
}
```

**Response — 200 (verified)**
```json
{
  "success": true,
  "data": {
    "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
    "verificationStatus": "VERIFIED",
    "measuredWeightG": 1015.00,
    "expectedWeightG": 1030.00
  },
  "errorCode": null,
  "message": null
}
```

**Response — 200 (mismatch)**
```json
{
  "success": true,
  "data": {
    "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
    "verificationStatus": "MISMATCH",
    "measuredWeightG": 1300.00,
    "expectedWeightG": 1030.00
  },
  "errorCode": null,
  "message": null
}
```

---

## 4. SHOPPING SESSIONS

### 4.1 POST /api/sessions

**Request**
```json
{
  "trolleyCode": "TRLY-0042",
  "userId": "b3f1a2e0-6c4d-4a9b-9f3e-1a2b3c4d5e6f"
}
```

**Response — 201**
```json
{
  "success": true,
  "data": {
    "sessionId": "4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a",
    "sessionCode": "SESS-20260829-0042",
    "userId": "b3f1a2e0-6c4d-4a9b-9f3e-1a2b3c4d5e6f",
    "trolleyCode": "TRLY-0042",
    "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "status": "ACTIVE",
    "cartId": "2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e",
    "cartTotal": 0.00,
    "startedAt": "2026-08-29T14:30:00",
    "endedAt": null
  },
  "errorCode": null,
  "message": null
}
```

**Error — 409**
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Trolley is already in an active session"
}
```

---

### 4.2 GET /api/sessions/{id}

**Response — 200**: same shape as 4.1's `data`.

---

### 4.3 POST /api/sessions/{id}/end

**Request**: no body.

**Response — 200**
```json
{
  "success": true,
  "data": {
    "session": {
      "sessionId": "4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a",
      "sessionCode": "SESS-20260829-0042",
      "userId": "b3f1a2e0-6c4d-4a9b-9f3e-1a2b3c4d5e6f",
      "trolleyCode": "TRLY-0042",
      "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "status": "CHECKED_OUT",
      "cartId": "2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e",
      "cartTotal": 186.00,
      "startedAt": "2026-08-29T14:30:00",
      "endedAt": "2026-08-29T15:05:00"
    },
    "orderId": "6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c",
    "orderCode": "ORD-20260829-0871",
    "grandTotal": 195.30
  },
  "errorCode": null,
  "message": null
}
```

---

## 5. ORDERS

### 5.1 GET /api/orders/{id}

**Response — 200**
```json
{
  "success": true,
  "data": {
    "orderId": "6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c",
    "orderCode": "ORD-20260829-0871",
    "status": "PENDING",
    "subtotal": 186.00,
    "discountTotal": 3.10,
    "taxTotal": 12.40,
    "grandTotal": 195.30,
    "items": [
      {
        "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
        "name": "Amul Toned Milk 1L",
        "quantity": 3,
        "unitPrice": 62.00,
        "lineTotal": 186.00
      }
    ],
    "createdAt": "2026-08-29T15:05:00"
  },
  "errorCode": null,
  "message": null
}
```

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "NOT_FOUND",
  "message": "Order not found"
}
```

---

## 6. PAYMENTS

### 6.1 POST /api/payments/initiate

**Request**
```json
{
  "orderId": "6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c"
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "paymentId": "7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d",
    "orderId": "6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c",
    "amount": 195.30,
    "status": "INITIATED",
    "upiDeepLink": "upi://pay?pa=cartrex@razorpay&pn=CartRex&am=195.30&tr=ORD-20260829-0871&cu=INR",
    "qrPayload": "00020101021226370010A00000072301122...5303356540419530.305802IN6304ABCD"
  },
  "errorCode": null,
  "message": null
}
```

**Error — 409**
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Order is not in a payable state"
}
```

---

### 6.2 POST /api/payments/webhook

**Request (Razorpay-style payload, illustrative)**
```json
{
  "orderId": "6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c",
  "status": "SUCCESS",
  "signature": "t=1756483500,v1=5f8b2c1e9d3a4f6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b",
  "upiTxnRef": "402912345678"
}
```

**Response — 200**
```json
{ "success": true, "data": null, "errorCode": null, "message": null }
```

**Error — 401**
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_TOKEN",
  "message": "Invalid webhook signature"
}
```

---

### 6.3 GET /api/payments/{orderId}/status

**Response — 200**
```json
{
  "success": true,
  "data": {
    "orderId": "6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c",
    "paymentId": "7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d",
    "status": "SUCCESS",
    "amount": 195.30
  },
  "errorCode": null,
  "message": null
}
```

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "NOT_FOUND",
  "message": "No payment initiated for this order"
}
```

---

## 7. TROLLEYS

### 7.1 POST /api/trolleys (ADMIN)

**Request**
```json
{
  "trolleyCode": "TRLY-0042",
  "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "esp32Mac": "24:6F:28:AB:CD:EF"
}
```

**Response — 201**
```json
{
  "success": true,
  "data": {
    "id": "8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e",
    "trolleyCode": "TRLY-0042",
    "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "status": "INACTIVE",
    "qrPayload": "CARTREX:TROLLEY:TRLY-0042:8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e",
    "deviceToken": "dtok_5f8b2c1e9d3a4f6b7c8d9e0f1a2b3c4d"
  },
  "errorCode": null,
  "message": null
}
```

**Error — 409**
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "A trolley with this code already exists"
}
```

---

### 7.2 GET /api/trolleys/{code}

**Response — 200**
```json
{
  "success": true,
  "data": {
    "id": "8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e",
    "trolleyCode": "TRLY-0042",
    "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "status": "IN_SESSION",
    "qrPayload": "CARTREX:TROLLEY:TRLY-0042:8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e",
    "lastSeenAt": "2026-08-29T14:59:47"
  },
  "errorCode": null,
  "message": null
}
```

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "TROLLEY_NOT_FOUND",
  "message": "Trolley not found"
}
```

---

### 7.3 PATCH /api/trolleys/{id}/status (ADMIN, STORE_STAFF)

**Request**
```json
{
  "status": "MAINTENANCE"
}
```

**Response — 200**: same shape as 7.2's `data`, `status` updated.

---

### 7.4 POST /api/trolleys/{code}/heartbeat (ROLE_DEVICE)

**Request**
```json
{
  "batteryPct": 78,
  "rssi": -62
}
```

**Response — 200**
```json
{ "success": true, "data": null, "errorCode": null, "message": null }
```

**Error — 401**
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_TOKEN",
  "message": "Device token does not match trolley code"
}
```

---

## 8. INVENTORY

### 8.1 GET /api/inventory?storeId=...

**Response — 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f",
      "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
      "productName": "Amul Toned Milk 1L",
      "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "stockQty": 142,
      "lowStockThreshold": 20,
      "updatedAt": "2026-08-29T09:00:00"
    }
  ],
  "errorCode": null,
  "message": null
}
```

---

### 8.2 GET /api/inventory/low-stock?storeId=...

**Response — 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "0d1e2f3a-4b5c-6d7e-8f9a-0b1c2d3e4f5a",
      "productId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
      "productName": "Britannia Brown Bread 400g",
      "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "stockQty": 6,
      "lowStockThreshold": 15,
      "updatedAt": "2026-08-29T09:00:00"
    }
  ],
  "errorCode": null,
  "message": null
}
```

---

### 8.3 PATCH /api/inventory/{productId}/adjust

**Request**
```json
{
  "delta": -3
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "id": "9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f",
    "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
    "productName": "Amul Toned Milk 1L",
    "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "stockQty": 139,
    "lowStockThreshold": 20,
    "updatedAt": "2026-08-29T15:06:12"
  },
  "errorCode": null,
  "message": null
}
```

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "NOT_FOUND",
  "message": "No inventory row for this product"
}
```

---

## 9. OFFERS

### 9.1 GET /api/offers?storeId=...&productId=...

**Response — 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "1e2f3a4b-5c6d-7e8f-9a0b-1c2d3e4f5a6b",
      "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
      "title": "Dairy Week Discount",
      "discountPct": 10.00,
      "validFrom": "2026-08-25T00:00:00",
      "validTo": "2026-08-31T23:59:59",
      "active": true
    }
  ],
  "errorCode": null,
  "message": null
}
```

---

### 9.2 GET /api/offers/{id}

**Response — 200**: single object, same shape as one item in 9.1.

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "NOT_FOUND",
  "message": "Offer not found"
}
```

---

### 9.3 POST /api/offers (ADMIN, STORE_STAFF)

**Request**
```json
{
  "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
  "title": "Dairy Week Discount",
  "discountPct": 10.00,
  "validFrom": "2026-08-25T00:00:00",
  "validTo": "2026-08-31T23:59:59",
  "active": true
}
```

**Response — 201**: same shape as one item in 9.1.

---

### 9.4 PUT /api/offers/{id} (ADMIN, STORE_STAFF)

**Request**: same shape as 9.3.

**Response — 200**: same shape as one item in 9.1.

---

### 9.5 DELETE /api/offers/{id} (ADMIN, STORE_STAFF)

**Request**: no body.

**Response — 200**
```json
{ "success": true, "data": null, "errorCode": null, "message": null }
```

---

## 10. NOTIFICATIONS

### 10.1 GET /api/notifications?userId=...&unreadOnly=true

**Response — 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "2f3a4b5c-6d7e-8f9a-0b1c-2d3e4f5a6b7c",
      "type": "WEIGHT_MISMATCH",
      "title": "Weight mismatch detected",
      "body": "Amul Toned Milk 1L in your cart shows a weight mismatch. Please recheck the item.",
      "read": false,
      "createdAt": "2026-08-29T14:50:00"
    }
  ],
  "errorCode": null,
  "message": null
}
```

---

### 10.2 PATCH /api/notifications/{id}/read

**Request**: no body.

**Response — 200**
```json
{
  "success": true,
  "data": {
    "id": "2f3a4b5c-6d7e-8f9a-0b1c-2d3e4f5a6b7c",
    "type": "WEIGHT_MISMATCH",
    "title": "Weight mismatch detected",
    "body": "Amul Toned Milk 1L in your cart shows a weight mismatch. Please recheck the item.",
    "read": true,
    "createdAt": "2026-08-29T14:50:00"
  },
  "errorCode": null,
  "message": null
}
```

---

## 11. SHOPPING LIST

### 11.1 GET /api/shopping-list?userId=...

**Response — 200**
```json
{
  "success": true,
  "data": {
    "listId": "3a4b5c6d-7e8f-9a0b-1c2d-3e4f5a6b7c8d",
    "name": "Weekly Groceries",
    "items": [
      {
        "itemId": "4b5c6d7e-8f9a-0b1c-2d3e-4f5a6b7c8d9e",
        "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
        "name": "Amul Toned Milk 1L",
        "purchased": false,
        "purchasedAt": null
      },
      {
        "itemId": "5c6d7e8f-9a0b-1c2d-3e4f-5a6b7c8d9e0f",
        "productId": null,
        "name": "Fresh coriander bunch",
        "purchased": true,
        "purchasedAt": "2026-08-29T15:02:10"
      }
    ]
  },
  "errorCode": null,
  "message": null
}
```

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "USER_NOT_FOUND",
  "message": "User not found"
}
```

---

### 11.2 POST /api/shopping-list?userId=...

**Request**: no body.

**Response — 200**: same shape as 11.1 (creates and returns a new/empty list).

---

### 11.3 POST /api/shopping-list/{listId}/items

**Request (by product)**
```json
{
  "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
  "customName": null
}
```

**Request (custom item)**
```json
{
  "productId": null,
  "customName": "Fresh coriander bunch"
}
```

**Response — 200**: same shape as 11.1's `data`, with the new item included.

**Error — 400**
```json
{
  "success": false,
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "message": "Provide exactly one of productId or customName"
}
```

---

### 11.4 PATCH /api/shopping-list/items/{itemId}

**Request**
```json
{
  "purchased": true
}
```

**Response — 200**: same shape as 11.1's `data`, item's `purchased`/`purchasedAt` updated.

**Error — 404**
```json
{
  "success": false,
  "data": null,
  "errorCode": "NOT_FOUND",
  "message": "Shopping list item not found"
}
```

---

## 12. RECOMMENDATIONS

### 12.1 GET /api/recommendations/personalized?userId=...

**Response — 200**
```json
{
  "success": true,
  "data": [
    { "productId": "3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f", "name": "Amul Butter 100g", "reason": "PERSONALIZED_CATEGORY_MATCH" },
    { "productId": "6d7e8f9a-0b1c-2d3e-4f5a-6b7c8d9e0f1a", "name": "Nescafe Classic Coffee 50g", "reason": "BESTSELLER" }
  ],
  "errorCode": null,
  "message": null
}
```

---

### 12.2 GET /api/recommendations/fbt?productId=...

**Response — 200**
```json
{
  "success": true,
  "data": [
    { "productId": "3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f", "name": "Amul Butter 100g", "reason": "FREQUENTLY_BOUGHT_TOGETHER" }
  ],
  "errorCode": null,
  "message": null
}
```

---

### 12.3 GET /api/recommendations/basic?category=Dairy

**Response — 200**
```json
{
  "success": true,
  "data": [
    { "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b", "name": "Amul Toned Milk 1L", "reason": "BESTSELLER" }
  ],
  "errorCode": null,
  "message": null
}
```

---

### 12.4 POST /api/recommendations/recompute (ADMIN)

**Request**: no body.

**Response — 200**
```json
{ "success": true, "data": null, "errorCode": null, "message": null }
```

---

## 13. ADMIN

### 13.1 GET /api/admin/trolleys?storeId=...

**Response — 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e",
      "trolleyCode": "TRLY-0042",
      "storeId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "status": "IN_SESSION",
      "qrPayload": "CARTREX:TROLLEY:TRLY-0042:8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e",
      "lastSeenAt": "2026-08-29T14:59:47"
    }
  ],
  "errorCode": null,
  "message": null
}
```

---

### 13.2 GET /api/admin/products?storeId=...&search=...&category=...&page=0&size=20

**Response — 200**: same shape as 2.1's `data` (`Page<ProductResponse>`).

---

### 13.3 POST /api/admin/products/bulk-deactivate (ADMIN only)

**Request**
```json
{
  "productIds": [
    "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
    "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b"
  ]
}
```

**Response — 200**
```json
{
  "success": true,
  "data": {
    "requested": 2,
    "succeeded": 1,
    "failedIds": ["5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b"]
  },
  "errorCode": null,
  "message": null
}
```

---

### 13.4 GET /api/admin/stock?storeId=...

**Response — 200**: same shape as 8.1's `data`.

---

### 13.5 GET /api/admin/stock/low?storeId=...

**Response — 200**: same shape as 8.2's `data`.

---

### 13.6 GET /api/admin/orders?storeId=...&status=PAID&page=0&size=20

**Response — 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": "6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c",
        "orderCode": "ORD-20260829-0871",
        "status": "PAID",
        "subtotal": 186.00,
        "discountTotal": 3.10,
        "taxTotal": 12.40,
        "grandTotal": 195.30,
        "items": [
          {
            "productId": "0e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
            "name": "Amul Toned Milk 1L",
            "quantity": 3,
            "unitPrice": 62.00,
            "lineTotal": 186.00
          }
        ],
        "createdAt": "2026-08-29T15:05:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20
  },
  "errorCode": null,
  "message": null
}
```

---

## 14. GLOBAL ERROR BODIES (reusable across any endpoint)

```json
{ "success": false, "data": null, "errorCode": "VALIDATION_ERROR", "message": "Validation failed", "fieldErrors": [ { "field": "price", "constraint": "min", "message": "price must be >= 0" } ] }
```
```json
{ "success": false, "data": null, "errorCode": "INVALID_TOKEN", "message": "Authentication is required" }
```
```json
{ "success": false, "data": null, "errorCode": "TOKEN_EXPIRED", "message": "Access token has expired" }
```
```json
{ "success": false, "data": null, "errorCode": "FORBIDDEN", "message": "You do not have permission to perform this operation" }
```
```json
{ "success": false, "data": null, "errorCode": "NOT_FOUND", "message": "Resource not found" }
```
```json
{ "success": false, "data": null, "errorCode": "INTERNAL_ERROR", "message": "An unexpected error occurred" }
```
