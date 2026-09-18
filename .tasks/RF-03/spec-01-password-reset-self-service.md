# User Story Description

This vertical slice implements the **self-service password recovery flow**. It is the MVP walking skeleton of RF-03: a registered user who has forgotten their password can recover access by providing their username and registered email address. The system verifies the match, generates a provisional (temporary) password, hashes and stores it securely, and communicates it to the user. The user is expected to change this provisional password after logging in.

This slice runs end-to-end through: Controller → Service → Repository → Database, validating the full technical risk of the recovery flow before the admin-driven slice (spec-02) is built.

---

# Story (INVEST)

**As** a registered user (Docente, Auditor, or Admin de Unidad)
**I want** to request a new provisional password by providing my username and the email address associated with my account
**So that** I can recover access to my account independently, without needing to contact an administrator

---

# Analysis

## Approach

A new endpoint `POST /api/auth/reset-password` will be added to `AuthController`. The request body will contain `username` and `correo`. `AuthService` will:

1. Look up the user via `UsuarioRepository.findByUsernameAndCorreo()` (new query method).
2. If no match is found, return a **generic error** to prevent user enumeration attacks (the response must not reveal whether the username or the email was wrong — see NFR below).
3. If a match is found, generate a random provisional password (e.g., UUID-based or alphanumeric), encode it with `BCryptPasswordEncoder`, and persist it.
4. Mark the user as `debeCambiarContrasenia = true` (pending resolution of **B2** in open-questions.md).
5. Deliver the provisional password to the user (pending resolution of **B1** in open-questions.md — email vs. on-screen).

**In-scope:** Backend generation, hashing, storage, and API response.
**Out-of-scope (this slice):** Email delivery infrastructure (SMTP), frontend UI for the reset form, forced password-change interception on next login (those are separate slices pending B1 and B2 resolution).

---

# Starting Point

| | |
|--|--|
| **Project** | `atlas-backend` |
| **Classes** | `AuthController`, `AuthService`, `UsuarioRepository`, `Usuario`, `PasswordEncoder` |
| **New classes** | `PasswordResetRequest` (DTO), optionally `PasswordResetResponse` (DTO) |
| **New DB column** | `usuario_debe_cambiar_contrasenia BOOLEAN NOT NULL DEFAULT FALSE` on `usuarios` table — **pending B2** |
| **New repository method** | `UsuarioRepository.findByUsernameAndCorreo(String username, String correo)` |
| **Endpoint** | `POST /api/auth/reset-password` (must be added to `SecurityConfig` permitAll list) |

---

# Context

- Raw requirement: [raw-requirement.md](./raw-requirement.md)
- Open questions (blockers): [open-questions.md](./open-questions.md)
- Admin-driven slice: [spec-02-password-reset-admin.md](./spec-02-password-reset-admin.md)
- Technical diagrams: [solution-diagrams.md](./solution-diagrams.md)

---

# Acceptance Criteria

**Scenario 1: Successful self-service password reset (Happy Path)**
- **Given** a user with username `"jdoe"` exists and their registered email is `"jdoe@atlas.edu.ar"`
- **When** the user submits `POST /api/auth/reset-password` with `{ "username": "jdoe", "correo": "jdoe@atlas.edu.ar" }`
- **Then** the system responds with `200 OK`
- **And** the user's `contrasenia` field in the database is updated to a new BCrypt-hashed provisional password
- **And** the provisional password (plain text) is communicated to the user *(delivery method pending B1)*

**Scenario 2: Username found but email does not match (Unhappy Flow)**
- **Given** a user with username `"jdoe"` exists, but the submitted email `"wrong@email.com"` does not match their record
- **When** the user submits `POST /api/auth/reset-password` with `{ "username": "jdoe", "correo": "wrong@email.com" }`
- **Then** the system responds with `404 Not Found`
- **And** the error message is the generic `"No account found matching the provided credentials."` *(does not reveal which field was incorrect)*

**Scenario 3: Non-existent username (Unhappy Flow — User Enumeration Prevention)**
- **Given** no user with username `"nonexistent"` exists in the database
- **When** the user submits `POST /api/auth/reset-password` with `{ "username": "nonexistent", "correo": "any@email.com" }`
- **Then** the system responds with `404 Not Found`
- **And** the error message is the same generic `"No account found matching the provided credentials."` as Scenario 2 *(prevents user enumeration)*

**Scenario 4: Missing required fields (Unhappy Flow — Validation)**
- **Given** the request body is missing `username` or `correo` (or both)
- **When** the user submits `POST /api/auth/reset-password`
- **Then** the system responds with `400 Bad Request`
- **And** an error message indicating the missing field(s)

**Scenario 5: Inactive user account (Unhappy Flow)**
- **Given** a user with matching username and email exists but `activo = false`
- **When** the user submits `POST /api/auth/reset-password`
- **Then** the system responds with `403 Forbidden`
- **And** the error message is `"Account is inactive, please contact support."`

---

# Test Plan

## Context

- A test user with `username = "testuser"`, `correo = "test@atlas.edu.ar"`, `activo = true` must exist in the test DB.
- A second user with `activo = false` must exist for Scenario 5.
- The endpoint must be accessible without a JWT (added to `SecurityConfig` permitAll).

## Scenarios

| # | Scenario | Input | Expected Result | Type |
|---|----------|-------|-----------------|------|
| 1 | Valid username + matching email | `{ "username": "testuser", "correo": "test@atlas.edu.ar" }` | `200 OK`, DB password updated | Success |
| 2 | Valid username + wrong email | `{ "username": "testuser", "correo": "wrong@test.com" }` | `404`, generic message | Unhappy |
| 3 | Non-existent username | `{ "username": "ghost", "correo": "any@mail.com" }` | `404`, same generic message as #2 | Unhappy (Enumeration) |
| 4 | Missing `correo` field | `{ "username": "testuser" }` | `400 Bad Request` | Validation |
| 5 | Inactive user | Valid match but `activo = false` | `403 Forbidden` | Authorization |

---

# Post-implementation Validation

1. `mvn clean install`
2. `mvn test` — verify all unit tests for `AuthService.resetPassword()` pass
3. Local startup: hit `POST /api/auth/reset-password` via Postman/Swagger with all 5 scenarios
4. Inspect DB row for the test user: verify `usuario_contrasenia` is a BCrypt hash (starts with `$2a$`)
5. Verify the same response body is returned for Scenarios 2 and 3 (no information leakage)

---

# Non-functional Notes

- **Security — No user enumeration:** Scenarios 2 and 3 MUST return identical HTTP status codes and response bodies. The system must not hint whether the username or email was the incorrect field.
- **Security — Provisional password never stored in plain text:** The provisional password must be BCrypt-encoded before persistence. It must never appear in application logs.
- **Security — Rate limiting:** The reset endpoint should be subject to the same brute-force protection as the login endpoint (`LoginAttemptService` or equivalent), to prevent password-spray attacks via the reset path.
- **Compliance — Audit:** Every reset action (successful or failed) should be logged (username attempted, timestamp, IP, outcome) without logging the password itself.

---

# Open Questions

See [open-questions.md](./open-questions.md) for the full list. Items affecting this spec directly:

- **B1** — How is the provisional password delivered to the user?
- **B2** — Is a forced password change on next login required? (impacts `Usuario` entity and login flow)
- **B3** — Is the provisional password randomly generated by the backend or chosen by the user?
