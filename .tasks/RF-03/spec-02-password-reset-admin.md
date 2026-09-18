# User Story Description

This vertical slice implements the **admin-driven password reset flow**. A Global Administrator can reset the password of any user from the `Gestión → Modificar Usuario` screen. This is the fallback path for users who cannot complete the self-service recovery (spec-01) — for example, because they no longer have access to their registered email. The system generates a provisional password, hashes it, stores it, and notifies the affected user, without the admin ever seeing the password in plain text.

This slice depends on spec-01 having established the provisional password generation and hashing logic in `AuthService`.

---

# Story (INVEST)

**As** a Global Administrator
**I want** to reset the password of any user from the `Gestión → Modificar Usuario` management screen
**So that** I can unblock users who cannot complete self-service recovery, while ensuring I never see their password in plain text

---

# Analysis

## Approach

A new endpoint `POST /api/auth/admin-reset-password/{userId}` (or `POST /api/usuarios/{userId}/reset-password` — **pending R1** in open-questions.md) will be added. The endpoint is protected: only users with role `ADMIN_GLOBAL` may call it (enforced via Spring Security `@PreAuthorize` or role-based route restriction in `SecurityConfig`).

`AuthService` will reuse the provisional password generation logic from spec-01:

1. Look up the target user by `userId` via `UsuarioRepository.findById()`.
2. If not found, return `404 Not Found`.
3. Generate a random provisional password, encode with BCrypt, persist.
4. Mark `debeCambiarContrasenia = true` (pending B2).
5. Communicate the provisional password to the user *(pending B1 — the admin sees only a success confirmation, never the password)*.

**In-scope:** Backend generation, hashing, storage, authorization check, API response.
**Out-of-scope (this slice):** Frontend UI for the management screen, email delivery infrastructure (pending B1), audit log UI.

---

# Starting Point

| | |
|--|--|
| **Project** | `atlas-backend` |
| **Classes** | `AuthController` (or new `UsuarioController`), `AuthService`, `UsuarioRepository`, `Usuario`, `SecurityConfig` |
| **Reused logic** | `AuthService` provisional password generation from spec-01 |
| **New endpoint** | `POST /api/auth/admin-reset-password/{userId}` *(endpoint location pending R1)* |
| **Authorization** | Only `ADMIN_GLOBAL` role may call this endpoint |

---

# Context

- Raw requirement: [raw-requirement.md](./raw-requirement.md)
- Open questions (blockers): [open-questions.md](./open-questions.md)
- Self-service slice (dependency): [spec-01-password-reset-self-service.md](./spec-01-password-reset-self-service.md)
- Technical diagrams: [solution-diagrams.md](./solution-diagrams.md)

---

# Acceptance Criteria

**Scenario 1: Successful admin-triggered password reset (Happy Path)**
- **Given** a Global Admin is authenticated (valid JWT with role `ADMIN_GLOBAL`)
- **And** a target user with `userId = 42` exists and is active
- **When** the admin sends `POST /api/auth/admin-reset-password/42`
- **Then** the system responds with `200 OK`
- **And** the target user's `contrasenia` in the database is updated to a new BCrypt-hashed provisional password
- **And** the provisional password is communicated to the target user *(delivery method pending B1)*
- **And** the admin's response body contains only a success message — **never the provisional password in plain text**

**Scenario 2: Non-admin role attempts reset (Unhappy Flow — Authorization)**
- **Given** an authenticated user with role `DOCENTE` or `AUDITOR` or `ADMIN_UNIDAD`
- **When** that user sends `POST /api/auth/admin-reset-password/42`
- **Then** the system responds with `403 Forbidden`
- **And** the target user's password is **not changed**

**Scenario 3: Unauthenticated request (Unhappy Flow — Authentication)**
- **Given** no JWT token is included in the request
- **When** a request is sent to `POST /api/auth/admin-reset-password/42`
- **Then** the system responds with `401 Unauthorized`

**Scenario 4: Target user not found (Unhappy Flow)**
- **Given** a Global Admin is authenticated
- **And** no user with `userId = 9999` exists
- **When** the admin sends `POST /api/auth/admin-reset-password/9999`
- **Then** the system responds with `404 Not Found`
- **And** the error message is `"User not found."`

**Scenario 5: Target user is inactive (Edge Case)**
- **Given** a Global Admin is authenticated
- **And** the target user with `userId = 43` has `activo = false`
- **When** the admin sends `POST /api/auth/admin-reset-password/43`
- **Then** the system responds with `409 Conflict` (or `400 Bad Request`)
- **And** the error message is `"Cannot reset password for an inactive account."`

---

# Test Plan

## Context

- A user with role `ADMIN_GLOBAL` must exist with a valid JWT for test calls.
- A user with role `DOCENTE` must exist with a valid JWT for Scenario 2.
- A target user `userId = 42` (active) and `userId = 43` (inactive) must exist.

## Scenarios

| # | Scenario | Input | Expected Result | Type |
|---|----------|-------|-----------------|------|
| 1 | Admin resets an active user's password | Valid ADMIN_GLOBAL JWT + `userId = 42` | `200 OK`, DB password changed, no plain text in response | Success |
| 2 | Non-admin role attempts reset | DOCENTE JWT + `userId = 42` | `403 Forbidden`, password unchanged | Authorization |
| 3 | Unauthenticated request | No JWT + `userId = 42` | `401 Unauthorized` | Authentication |
| 4 | Target user not found | ADMIN_GLOBAL JWT + `userId = 9999` | `404 Not Found` | Error |
| 5 | Target user inactive | ADMIN_GLOBAL JWT + `userId = 43` | `409 Conflict` | Edge Case |

---

# Post-implementation Validation

1. `mvn clean install`
2. `mvn test` — verify all unit tests for `AuthService.adminResetPassword()` pass
3. Local startup: test all 5 scenarios via Postman/Swagger
4. Inspect DB row for target user: verify `usuario_contrasenia` is a BCrypt hash after reset
5. Verify that `DOCENTE` JWT receives `403` and the password in DB is unchanged

---

# Non-functional Notes

- **Security — Plain text password never exposed to admin:** The admin's response must contain only a confirmation message. The provisional password must only be communicated directly to the target user (via email or equivalent — pending B1).
- **Security — Authorization at service layer:** The `ADMIN_GLOBAL` role check must be enforced at the service/controller layer via Spring Security, not only at the frontend/UI layer.
- **Audit:** All admin reset actions must be logged (admin userId, target userId, timestamp, IP) without logging the password itself. This supports security audit trails.
- **BCrypt encoding:** The provisional password must be encoded via `BCryptPasswordEncoder` before persistence, consistent with the rest of the authentication domain.

---

# Open Questions

See [open-questions.md](./open-questions.md). Items affecting this spec directly:

- **B1** — How is the provisional password communicated to the target user after an admin reset?
- **B2** — Is a forced password change on next login required?
- **R1** — Should this endpoint live in `AuthController` or a dedicated `UsuarioController`?
