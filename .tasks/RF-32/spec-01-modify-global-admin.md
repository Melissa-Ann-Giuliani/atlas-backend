# User Story – Modify Global Administrator

## User Story Description

Allows a Global Administrator to modify personal data, contact information (email), and assigned cargo (`CargoAdminGlobal`) of any registered Global Administrator. Every modification must validate data integrity, enforce email uniqueness across users, and automatically record audit log deltas into the `cambios` system table.

---

## Story (INVEST)

```
As a Global Administrator
I want to update the profile and cargo information of a Global Administrator
So that their operational contact details and institutional responsibilities remain accurate and properly audited
```

---

## Analysis

### Approach
- Single cohesive endpoint `PUT /api/usuarios/admin-global/{id}` or `PUT /api/admin-global/{id}` (matching existing pattern in `UsuarioController` where `POST /api/usuarios/admin-global` creates global admins).
- Requires role `ADMINISTRADOR_GLOBAL` / `ADMIN_GLOBAL`.
- Fields modifiable:
  - Personal data: `nombre` (not blank, max 50), `apellido` (not blank, max 50)
  - Account & Contact data: `username` (not blank, max 50, unique across `usuarios` excluding current user ID), `correo` (valid email format, unique across `usuarios` table excluding current record)
  - Cargo: `cargoNombre` (enum `CargoAdminGlobal`: `DIRECCION_DE_PERSONAL`, `DESPACHO`)
- Auditing:
  - Every modified attribute generates an entry in `cambios` table via `CambioService.registrarCambio(...)` specifying field name, previous value, new value, date, and responsible user.
- Response:
  - Updated `AdminGlobal` or a DTO containing the fresh user data.

### Scope & Domain Boundaries
- In-scope: Updating `nombre`, `apellido`, `correo`, `cargoNombre`, change auditing in `cambios`.
- Out-of-scope: Password resetting (handled by `POST /api/usuarios/{id}/reset-password`), activation/deactivation (handled by user status endpoints), teaching functions (do not apply to Global Admins).

---

## Starting Point

| | |
|---|---|
| **Project** | `atlas-backend` |
| **Classes** | `com.atlas.atlas_backend.usuario.UsuarioController`<br>`com.atlas.atlas_backend.usuario.AdminGlobalService`<br>`com.atlas.atlas_backend.usuario.AdminGlobal`<br>`com.atlas.atlas_backend.cambio.CambioService` |
| **Method / mapping** | `PUT /api/usuarios/admin-global/{id}` calling `AdminGlobalService.modificarAdminGlobal(id, request, responsibleUsername)` |

---

## Context

- `raw-requirement.md`: [raw-requirement.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-32/raw-requirement.md)
- `open-questions.md`: [open-questions.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-32/open-questions.md)

---

## Acceptance Criteria

### Scenario 1: Successfully modify Global Administrator information (Happy Path)
- **Given** an authenticated user with role `ADMINISTRADOR_GLOBAL`
- **And** an existing `AdminGlobal` with ID `5`, username `"admin_rrhh"`, name `"Juan"`, surname `"Perez"`, email `"juan.perez@unc.edu.ar"`, and cargo `"DIRECCION_DE_PERSONAL"`
- **When** the administrator submits a PUT request to `/api/usuarios/admin-global/5` with:
  ```json
  {
    "username": "admin_rrhh_nuevo",
    "nombre": "Juan Carlos",
    "apellido": "Perez Gomez",
    "correo": "jc.perez@unc.edu.ar",
    "cargoNombre": "DESPACHO"
  }
  ```
- **Then** the response status must be `200 OK`
- **And** the `AdminGlobal` record must be updated in the database
- **And** audit entries must be logged in the `cambios` table for each changed field (`username`, `nombre`, `apellido`, `correo`, `cargo_ag_nombre`) noting the prior values, new values, timestamp, and responsible user.

### Scenario 2: Unchanged fields do not generate redundant audit records
- **Given** an existing `AdminGlobal` with username `"admin_rrhh"`, name `"Juan"`, surname `"Perez"`, email `"juan.perez@unc.edu.ar"`, and cargo `"DESPACHO"`
- **When** a PUT request is sent modifying only `correo` to `"jperez_new@unc.edu.ar"` while keeping username, name, surname, and cargo unchanged
- **Then** the response status is `200 OK`
- **And** only 1 audit entry for field `correo` is persisted in the `cambios` table.

### Scenario 3: Global Admin not found
- **Given** an authenticated user with role `ADMINISTRADOR_GLOBAL`
- **When** a PUT request is submitted for non-existent ID `999`
- **Then** the response status must be `404 Not Found`.

### Scenario 4: Email or username already in use by another user
- **Given** an existing user with username `"otro_admin"` and email `"otro.admin@unc.edu.ar"`
- **When** the administrator attempts to update an `AdminGlobal` setting `username` to `"otro_admin"` or `correo` to `"otro.admin@unc.edu.ar"`
- **Then** the response status must be `409 Conflict` (or `400 Bad Request` with appropriate message indicating collision)
- **And** no database changes or audit records are persisted.

### Scenario 5: Missing or invalid required fields
- **Given** a request with blank `username`, blank `nombre`, invalid email format `"not-an-email"`, or null `cargoNombre`
- **When** the request is sent to `/api/usuarios/admin-global/{id}`
- **Then** the response status must be `400 Bad Request` with validation error details.

### Scenario 6: Unauthorized access
- **Given** an unauthenticated request or a user without role `ADMINISTRADOR_GLOBAL` / `ADMIN_GLOBAL`
- **When** a PUT request is submitted to modify an `AdminGlobal`
- **Then** the response status must be `401 Unauthorized` or `403 Forbidden`.

---

## Test Plan

#### Context
Requires running Spring Boot application context with database or repository mocks, testing authorization annotations and persistence validation.

#### Scenarios

| # | Type | Description | Expected Status |
|---|------|-------------|-----------------|
| 1 | Success | Update username, name, surname, email, and cargo with valid data | 200 OK + audit records |
| 2 | Success | Partial update (only cargo modified, unchanged fields) | 200 OK + 1 audit record |
| 3 | Error | Non-existent AdminGlobal ID | 404 Not Found |
| 4 | Error | Email or username collision with another registered user | 409 Conflict |
| 5 | Validation | Blank username, blank name, or invalid email format | 400 Bad Request |
| 6 | Security | Caller without ADMIN_GLOBAL role | 403 Forbidden |

---

## Post-implementation Validation

1. Run unit & integration tests:
   ```powershell
   .\mvnw test -Dtest=AdminGlobalServiceTest,UsuarioControllerTest
   ```
2. Verify existing suite regression:
   ```powershell
   .\mvnw clean test
   ```

---

## Non-functional notes

- **Auditing**: Leverage existing `CambioService` and `cambios` table.
- **Transactions**: Update operations must be wrapped in `@Transactional` so failure in updating or auditing rolls back cleanly.
- **Security**: Method security via `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL')")`.
