# User Story Description

This slice implements the **login error feedback flow**: when a user submits incorrect credentials on the login page, the backend denies access and returns a structured error response that the frontend can display. It is a foundational security slice that validates the full authentication failure path end-to-end — from the HTTP request through Spring Security's `AuthenticationManager`, down to the error response reaching the client.

This story does **not** cover the success path (JWT issuance), account registration, password reset, or session management. Those are separate stories.

---

# Story (INVEST)

```
As a registered Atlas system user who has entered incorrect login credentials,
I want to receive a clear and immediate error message on the login screen,
So that I know my attempt failed and I can retry with the correct information
or seek help — without being given information that could aid unauthorised access.
```

---

# Analysis

## Approach

**Vertical slice:** This story covers the full failure path: `AuthController → AuthService → Spring Security AuthenticationManager → error response → client display`.

**Security assumption (B1 resolved):** The spec is written using **Option A** (generic error message).

**In scope:**
- HTTP 401 response body with an error message on invalid credentials.
- HTTP 401 response on a disabled/inactive account (`usuario_activo = false`).
- Consistent structured JSON response format (M1 resolved).
- Rate limiting / account lockout (R1 resolved as must-have).

**Out of scope:**
- Frontend rendering logic.
- JWT token generation (success path).
- Registration or password reset flows.

---

# Starting Point

|  |  |
|--|--|
| **Project** | `atlas-backend` — Spring Boot 3 / Spring Security 6 / JWT |
| **Endpoint** | `POST /api/auth/login` |
| **Controller** | [`AuthController.java`](../../src/main/java/com/atlas/atlas_backend/auth/AuthController.java) — `login(AuthRequest): ResponseEntity<?>` |
| **Service** | [`AuthService.java`](../../src/main/java/com/atlas/atlas_backend/auth/AuthService.java) — `authenticate(AuthRequest): AuthResponse` |
| **DTOs** | [`AuthRequest.java`](../../src/main/java/com/atlas/atlas_backend/auth/AuthRequest.java) · [`AuthResponse.java`](../../src/main/java/com/atlas/atlas_backend/auth/AuthResponse.java) |
| **User details** | [`CustomUserDetailsService.java`](../../src/main/java/com/atlas/atlas_backend/security/CustomUserDetailsService.java) — `loadUserByUsername(String)` |
| **Domain entity** | [`Usuario.java`](../../src/main/java/com/atlas/atlas_backend/usuario/Usuario.java) — field `activo: Boolean` controls account status |
| **Exception types** | `BadCredentialsException`, `DisabledException` (Spring Security) |

---

# Context

- [raw-requirement.md](./raw-requirement.md)
- [open-questions.md](./open-questions.md)
- [solution-diagrams.md](./solution-diagrams.md)

---

# Acceptance Criteria

> **Note:** Scenarios 2a/2b are mutually exclusive depending on B1 resolution. The spec is written for Option A (generic message). If Option B is chosen, Scenario 2b replaces 2a and additional backend logic is required.

**Scenario 1 — Happy path: Valid credentials**

- **Given** a registered and active user exists with username `"jdoe"` and password `"secret123"`
- **When** a `POST /api/auth/login` request is made with body `{ "username": "jdoe", "password": "secret123" }`
- **Then** the server responds with HTTP `200 OK`
- **And** the response body contains a non-empty `token` field

---

**Scenario 2 — Unhappy path: Invalid credentials**

- **Given** the system is configured to return a generic error message
- **When** a `POST /api/auth/login` request is made with an unrecognised username or an incorrect password
- **Then** the server responds with HTTP `401 Unauthorized`
- **And** the response body is a JSON object containing `{ "message": "Invalid username or password" }`
- **And** the response does **not** reveal whether the username or the password was the source of failure

---

**Scenario 3 — Unhappy path: Account is inactive**

- **Given** a user with username `"jsmith"` exists but has `activo = false`
- **When** a `POST /api/auth/login` request is made with `{ "username": "jsmith", "password": "any" }`
- **Then** the server responds with HTTP `401 Unauthorized`
- **And** the response body is a JSON object containing `{ "message": "Account is inactive, please contact support." }`

---

**Scenario 4 — Unhappy path: Empty credentials submitted**

- **Given** a user is attempting to log in
- **When** a `POST /api/auth/login` request is made with an empty `username` field or an empty `password` field
- **Then** the server responds with HTTP `400 Bad Request`
- **And** the response body is a JSON object indicating that the missing field is required

---

**Scenario 5 — Unhappy path: Rate limit exceeded**

- **Given** rate limiting is enforced on the login endpoint
- **When** a user submits 5 consecutive failed login attempts from the same IP address within 1 minute
- **And** a 6th attempt is made
- **Then** the server responds with HTTP `429 Too Many Requests`
- **And** the response body is a JSON object containing `{ "message": "Too many failed attempts. Please try again later." }`

---

# Test Plan

## Context

- Environment: local Spring Boot instance with an H2 in-memory database (or staging DB).
- Seed data: at least one active user and one inactive user pre-loaded.
- Tool: Postman / REST client or `mvn test` with Spring Boot Test + MockMvc.

## Scenarios

| # | Type | Input | Expected result |
|---|------|-------|----------------|
| 1 | **Success** | Valid username + valid password | HTTP 200, `token` in response |
| 2 | **Error** | Valid username + wrong password | HTTP 401, JSON generic error message |
| 3 | **Error** | Non-existent username + any password | HTTP 401, same JSON generic error message as #2 |
| 4 | **Error** | Valid username + valid password but account inactive | HTTP 401, JSON `"Account is inactive..."` message |
| 5 | **Error** | Empty `username` field | HTTP 400, JSON validation error |
| 6 | **Error** | Empty `password` field | HTTP 400, JSON validation error |
| 7 | **Regression** | Previously successful login still works after any change | HTTP 200, valid token |
| 8 | **Security** | Same invalid username tried 10 times in rapid succession | First 5 return HTTP 401, subsequent return HTTP 429 (Rate limited) |

---

# Post-implementation Validation

1. `mvn clean install` — full build must pass with no compilation errors.
2. `mvn test` — all existing tests must pass; new tests for scenarios 2–4 must be added.
3. Manual test: submit a `POST /api/auth/login` with wrong credentials via Postman and verify the HTTP status and JSON body match Scenario 2.
4. Manual test: submit a login for an inactive user and verify Scenario 3.

---

# Non-functional notes

- **Security (NFR-1):** Error messages must not reveal whether the username exists in the system (OWASP Authentication Cheat Sheet). This is enforced by returning a generic JSON error message.
- **Security (NFR-2):** Brute-force protection (max 5 attempts/min per IP) is enforced on the login endpoint.
- **Consistency (NFR-3):** The error response format is a structured JSON object `{ "message": "..." }` to match REST API conventions.
