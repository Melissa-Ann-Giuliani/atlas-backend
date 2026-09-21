# User Story Description

The system must allow a Global Administrator to register new Global Administrators in the system. Once registered, the new user receives an email with a provisional password and must be forced to change it upon their first login. This is required to expand the administration capabilities of the system.

---

# Story (INVEST)

**As a** Global Administrator
**I want** to register a new Global Administrator in the system
**So that** they can help me manage the application's global configuration and user base.

---

# Analysis

#### Approach

Since this is a backend specification based on existing code, this slice covers the API endpoint exposed to create the administrator. It handles receiving the data, validating uniqueness (username/email), persisting the `AdminGlobal` entity with the `Administrador Global` role, and asynchronously (or synchronously) sending the welcome email with the provisional password.

---

# Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` |
| **Classes** | `src/main/java/com/atlas/atlas_backend/usuario/UsuarioController.java`<br>`src/main/java/com/atlas/atlas_backend/usuario/AdminGlobalService.java` |
| **Method / mapping** | `UsuarioController.registrarAdminGlobal`<br>`AdminGlobalService.registrarAdminGlobal` |

---

# Context

See `raw-requirement.md` for the initial unstructured requirements.

---

# Acceptance Criteria

**Scenario 1: Successful registration of a new Global Administrator**
* **Given** the active user has the role `ADMIN_GLOBAL`
* **And** the provided `username` and `correo` do not exist in the database
* **And** the `cargoNombre` belongs to the predefined allowed positions for `ADMIN_GLOBAL` (e.g. Dirección de Personal, Despacho)
* **When** a POST request is sent to `/api/usuarios/admin-global` with valid data (username, correo, apellido, nombre, cargoNombre)
* **Then** a new `AdminGlobal` user is created with the state `activo=true` and `debeCambiarContrasenia=true`
* **And** a temporary password is automatically generated and encrypted in the database
* **And** a welcome email is sent to the provided `correo` containing the cleartext provisional password
* **And** the system responds with a 201 Created status code.

**Scenario 2: Missing mandatory fields**
* **Given** the active user has the role `ADMIN_GLOBAL`
* **When** a POST request is sent to `/api/usuarios/admin-global` missing any of `username`, `correo`, `apellido`, `nombre`, or `cargoNombre`
* **Then** the system does not create the user
* **And** responds with a 400 Bad Request status code and an appropriate error message.

**Scenario 3: Invalid email format**
* **Given** the active user has the role `ADMIN_GLOBAL`
* **When** a POST request is sent to `/api/usuarios/admin-global` with a malformed `correo`
* **Then** the system does not create the user
* **And** responds with a 400 Bad Request status code and an appropriate error message.

**Scenario 4: User or Email already exists**
* **Given** the active user has the role `ADMIN_GLOBAL`
* **And** the provided `username` or `correo` already exists in the system
* **When** a POST request is sent to `/api/usuarios/admin-global`
* **Then** the system does not create the user
* **And** responds with a 409 Conflict status code and an error message indicating the duplication.

**Scenario 5: Registration successful but email delivery fails**
* **Given** the active user has the role `ADMIN_GLOBAL`
* **And** the provided `username` and `correo` do not exist in the database
* **And** the `cargoNombre` belongs to the predefined allowed positions
* **When** a POST request is sent to `/api/usuarios/admin-global` with valid data
* **And** the SMTP server is down or email sending throws an exception
* **Then** the new `AdminGlobal` user is still committed to the database
* **And** the system responds with a 201 Created status code and a warning message indicating the email could not be sent.

**Scenario 6: Invalid Cargo**
* **Given** the active user has the role `ADMIN_GLOBAL`
* **When** a POST request is sent to `/api/usuarios/admin-global` with a `cargoNombre` that is not in the predefined list for Admin Global
* **Then** the system does not create the user
* **And** responds with a 400 Bad Request status code and an appropriate error message indicating invalid cargo.

---

# Test Plan

#### Context

Ensure the database is running and the user performing the request is authenticated and has the role `ADMIN_GLOBAL`. An SMTP server (or Mailhog/Mailtrap) must be configured to catch the sent emails.

#### Scenarios

| # | Scenario | Details |
| - | -------- | ------- |
| 1 | Success | Register admin, verify HTTP 201, verify DB record, verify email received |
| 2 | Error | Omit `cargoNombre`, verify HTTP 400 |
| 3 | Error | Send `correo` = `invalid-email`, verify HTTP 400 |
| 4 | Error | Send `username` that exists in DB, verify HTTP 409 |
| 5 | Error | Send `correo` that exists in DB, verify HTTP 409 |
| 6 | Success (Warning) | Mock email failure, verify HTTP 201 with warning, verify DB record |
| 7 | Error | Send invalid `cargoNombre`, verify HTTP 400 |

---

# Post-implementation Validation

1. `mvn clean install`
2. `mvn test`
3. Local startup, obtain a token for an existing `ADMIN_GLOBAL`, and perform a manual POST via Postman to `/api/usuarios/admin-global`.

---

# Non-functional notes

- **Security**: The provisional password must be generated using a cryptographically secure method (currently `UUID.randomUUID()`) and must only be stored hashed (e.g. BCrypt) in the database. Furthermore, the 24-hour expiration for the provisional password must be enforced programmatically during authentication (checking `fechaReset` against the current time).
- **Reliability**: If the email service fails to send the email, the exception is caught and the transaction is committed. The user is saved, and a warning is returned in the HTTP 201 API response so the admin can trigger a resend manually.
