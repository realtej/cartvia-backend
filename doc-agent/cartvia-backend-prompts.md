# CartVia Spring Boot Backend — Task-Split Prompt Set

Use **Master Context** once at the start of the conversation, then paste each
task prompt one at a time (Task 1 → Task 10), attaching your CartVia reference
files when you send Task 1. Wait for each task to complete before sending the next.

---

## MASTER CONTEXT (paste once at the start)

```
You are the lead backend engineer for my project: CARTVIA.

PROJECT OVERVIEW:
CartVia is a smart connected-shopping ecosystem consisting of:
- Android customer application
- Smart shopping trolley / ESP32 hardware
- Spring Boot backend
- PostgreSQL database
- REST APIs + WebSocket real-time communication
- Product/barcode scanning
- Weight/load-cell validation
- Live cart and bill synchronization
- Digital checkout/payment
- Digital receipts
- Store/admin management

I have uploaded the project/reference files. Treat them as the primary source of truth.
FIRST inspect and understand ALL uploaded files before writing or modifying any code.

TECH STACK (mandatory):
- Java 25, Spring Boot 4.x, Maven
- PostgreSQL, Spring Data JPA
- Spring Security + JWT (BCrypt password hashing)
- WebSocket
- Jakarta Bean Validation
- Lombok where useful
- Use SPRING BOOT — do NOT use FastAPI or any other framework

RULES THAT APPLY TO EVERY TASK BELOW:
- Do not create a generic demo backend — build CartVia specifically.
- Do not invent features that conflict with the uploaded requirements.
- Reuse existing code/package structure if present; if none exists, use com.cartvia.backend
  with modules: config, security, auth, user, product, category, cart, trolley, order,
  payment, receipt, admin, websocket, exception, common.
- Never expose JPA entities directly — always use request/response DTOs.
- Never hard-code DB credentials, JWT secrets, URLs — use environment variables / .env.example.
- Backend is the single source of truth for prices — never trust client-submitted prices.
- Use database transactions where correctness requires it.
- I will give you this project in 10 sequential tasks. Do not jump ahead to later tasks.
  At the end of each task, summarize what was created/modified before I say "continue".
```

---

## TASK 1 — Analyze & Plan

```
TASK 1: ANALYZE UPLOADED FILES AND PLAN ARCHITECTURE

Inspect every uploaded CartVia file. Do NOT write code yet.

Extract and report:
- Exact functional requirements found in the files
- Any existing backend architecture/package structure
- Existing database design (entities/relationships) if present
- API requirements/contracts already defined
- Authentication requirements
- Android <-> backend communication expectations
- ESP32/trolley communication protocol (if specified)
- WebSocket event requirements
- Checkout/payment flow requirements

Then propose:
- Final entity model + relationships
- DTOs needed per module
- Services, repositories, controllers per module
- Security architecture (JWT flow, roles)
- WebSocket architecture (topics/events)
- Transaction boundaries

Flag anything missing or ambiguous and state the sensible production-quality
decision you'll make for it. Wait for my confirmation before proceeding to Task 2.
```

---

## TASK 2 — Project Scaffold

```
TASK 2: SCAFFOLD THE SPRING BOOT PROJECT

Based on the Task 1 plan, create:
- pom.xml with all required dependencies (Spring Web, Data JPA, Security, WebSocket,
  Validation, PostgreSQL driver, JWT lib, Lombok, testing deps)
- Base package structure under com.cartvia.backend (or existing structure)
- application.yml / application-dev.yml / application-prod.yml using env vars
- .env.example with all required variables (DB, JWT secret, CORS origins, ports)
- Basic Spring Boot application class

Do not implement business logic yet — this is scaffolding only.
```

---

## TASK 3 — Database Layer

```
TASK 3: DATABASE ENTITIES AND REPOSITORIES

Implement JPA entities and Spring Data repositories only for entities actually
required by the uploaded requirements (e.g. User, Role, Store, Product, Category,
Inventory, Cart, CartItem, Trolley, TrolleySession, Order, OrderItem, Payment,
Receipt, ScanEvent, WeightValidation — only as needed).

Include correct relationships, constraints, and indexes.
Add a migration approach (Flyway or schema.sql) if consistent with the project.
```

---

## TASK 4 — Security & Auth

```
TASK 4: AUTHENTICATION AND AUTHORIZATION

Implement:
- User registration and login endpoints (/api/auth/...)
- JWT access token generation/validation
- BCrypt password hashing
- Spring Security config with role-based authorization
- Protected endpoint setup
- Proper 401/403 error responses

Use DTOs for all requests/responses. No entities exposed directly.
```

---

## TASK 5 — Product/Category/Store/Admin

```
TASK 5: CATALOG AND ADMIN MODULES

Implement REST APIs for:
- /api/products/...
- /api/categories/...
- /api/admin/... (store/product/inventory management)

Include DTOs, service layer, validation, and role-restricted admin operations.
```

---

## TASK 6 — Cart & Trolley

```
TASK 6: CART AND SMART TROLLEY MODULES

Implement the authoritative server-side cart and trolley/session management:
- Trolley registration, identification, connection/disconnection
- Shopping-session association
- Scan event flow: receive scan -> identify product -> validate availability
  -> validate trolley/session -> add/update CartItem -> recalculate bill -> persist
- /api/carts/... and /api/trolleys/... endpoints

Follow the ESP32 protocol from the uploaded files if one is specified; otherwise
define a clean, documented protocol and state that assumption explicitly.
```

---

## TASK 7 — Weight Validation & WebSocket

```
TASK 7: WEIGHT VALIDATION + REAL-TIME WEBSOCKET

Implement:
- Weight/load-cell validation: receive measured weight, calculate expected
  cart weight, compare with configurable tolerance, record results
- WebSocket support for events: TROLLEY_CONNECTED, TROLLEY_DISCONNECTED,
  PRODUCT_SCANNED, PRODUCT_ADDED, PRODUCT_REMOVED, CART_UPDATED, WEIGHT_UPDATED,
  WEIGHT_VALIDATION_RESULT, CHECKOUT_STARTED, PAYMENT_COMPLETED, SESSION_UPDATED
- Clean JSON event payload structure, session/trolley-scoped subscriptions

Do not hard-code tolerance values unless specified in the uploaded files —
make tolerance configurable.
```

---

## TASK 8 — Checkout, Payment, Receipts

```
TASK 8: ORDER, CHECKOUT, PAYMENT, RECEIPTS

Implement the full checkout flow as a transactional operation:
Cart -> validate cart -> validate inventory -> calculate final bill -> create order
-> create payment transaction -> confirm payment -> finalize order -> generate
receipt -> close shopping session.

Payment: implement a mock/test payment provider behind a clean, replaceable
service abstraction (unless the uploaded files specify a real provider).
Clearly mark mock payments as non-production.

Receipts: include order info, products, quantities, prices, subtotal,
tax/discount, total, payment status, timestamp.

Endpoints: /api/orders/..., /api/payments/..., /api/receipts/...
```

---

## TASK 9 — Reliability, Errors, Tests

```
TASK 9: ERROR HANDLING, RELIABILITY, TESTS

Implement:
- Centralized @RestControllerAdvice covering validation, auth, not-found,
  duplicate resource, invalid trolley/session, invalid product, cart conflict,
  payment failure, and generic business/DB errors — with correct HTTP status codes
- Duplicate scan protection and idempotency where appropriate
- Tests covering: registration, login, JWT auth, authorization, product retrieval,
  cart creation, add/remove item, bill calculation, trolley/session management,
  checkout, payment handling, and key validation/error cases
```

---

## TASK 10 — Build Verification & Documentation

```
TASK 10: BUILD VERIFICATION, README, API DOCS

- Actually run `mvn clean test` and `mvn clean package`; fix all failures — do not
  claim success without running them.
- Add OpenAPI/Swagger documentation.
- Write/update README.md covering: overview, architecture, tech stack, project
  structure, PostgreSQL setup, environment variables, how to run, auth flow,
  REST API docs, WebSocket docs, Android integration (10.0.2.2:<PORT> for
  emulator, <PC-LAN-IP>:<PORT> for physical device), ESP32 integration, example
  requests/responses, testing, deployment.

Finish with a summary: files created/modified, final project structure, entities,
endpoints, WebSocket events, auth flow, checkout flow, how to run, test/build
results, assumptions made, remaining TODOs.
```
