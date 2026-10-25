# User Story – Modify Unit Administrator

## User Story Description

Allows a Global Administrator to modify personal data (`nombre`, `apellido`), contact data (`correo`, `username`), assigned position (`cargoNombre` from `CargoAdminUnidad`), and academic unit (`unidadId`) of any registered Unit Administrator (`AdminUnidad`). The system ensures data integrity, enforces uniqueness across users for username and email, validates academic unit existence, and registers audit log records in the `cambios` table for each changed field.

---

## Story (INVEST)

```
As a Global Administrator
I want to update the personal information, contact details, cargo, and assigned unit of a Unit Administrator
So that their operational contact details and institutional responsibilities in the academic unit remain up-to-date and fully audited
```

---

## Analysis

### Approach
- REST Endpoint: `PUT /api/usuarios/admin-unidad/{id}`
- Authorization: Restricted to `ADMIN_GLOBAL` / `ADMINISTRADOR_GLOBAL`.
- Fields modifiable:
  - Personal data: `nombre` (not blank, max 50), `apellido` (not blank, max 50)
  - Account & Contact data: `username` (not blank, max 50, unique across `usuarios` excluding current record), `correo` (valid email format, unique across `usuarios` excluding current record)
  - Cargo: `cargoNombre` (`CargoAdminUnidad`: `SECRETARIA_ACADEMICA`, `DEPARTAMENTO_DOCENTE`, `DIRECTOR_DEPARTAMENTO`)
  - Unit: `unidadId` (must reference an active existing `Unidad`)
- Auditing:
  - Track deltas via `CambioService.registrarCambio("admin_de_unidad", campo, valorAnterior, valorNuevo, usuarioResponsable)` for fields: `username`, `nombre`, `apellido`, `correo`, `cargo_ad_nombre`, `unidad_id`.
  - Only genuine changes (`valorAnterior != valorNuevo`) persist to the `cambios` table.

### Scope & Domain Boundaries
- In-scope: Updating `nombre`, `apellido`, `username`, `correo`, `cargoNombre`, `unidadId`, change auditing in `cambios`.
- Out-of-scope: Password resetting (handled via dedicated reset endpoint), activation/deactivation (handled by user status endpoints), teaching functions (exclusive to `Docente`).

---

## Starting Point

| | |
|---|---|
| **Project** | `atlas-backend` |
| **Classes** | `com.atlas.atlas_backend.usuario.UsuarioController`<br>`com.atlas.atlas_backend.usuario.AdminUnidadService`<br>`com.atlas.atlas_backend.usuario.AdminUnidadUpdateRequest`<br>`com.atlas.atlas_backend.usuario.AdminUnidad`<br>`com.atlas.atlas_backend.cambio.CambioService` |
| **Method / mapping** | `PUT /api/usuarios/admin-unidad/{id}` calling `AdminUnidadService.modificarAdminUnidad(id, request, responsibleUsername)` |

---

## Context

- `raw-requirement.md`: [raw-requirement.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-33/raw-requirement.md)
- `open-questions.md`: [open-questions.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-33/open-questions.md)

---

## Acceptance Criteria

### Scenario 1: Successfully modify Unit Administrator (Happy Path)
- **Given** an authenticated user with role `ADMINISTRADOR_GLOBAL`
- **And** an existing `AdminUnidad` with ID `10`, username `"admin_famaf"`, name `"Maria"`, surname `"Gomez"`, email `"maria.gomez@famaf.unc.edu.ar"`, cargo `"SECRETARIA_ACADEMICA"`, and unidad ID `1`
- **And** an existing `Unidad` with ID `2`
- **When** the administrator submits a PUT request to `/api/usuarios/admin-unidad/10` with:
  ```json
  {
    "username": "maria_gomez_dir",
    "nombre": "Maria Elena",
    "apellido": "Gomez Perez",
    "correo": "m.gomez@famaf.unc.edu.ar",
    "cargoNombre": "DIRECTOR_DEPARTAMENTO",
    "unidadId": 2
  }
  ```
- **Then** the response status must be `200 OK`
- **And** the `AdminUnidad` record in the database is updated with the new values
- **And** 6 audit entries are logged in the `cambios` table (one for each updated field: `username`, `nombre`, `apellido`, `correo`, `cargo_ad_nombre`, `unidad_id`) recording the old value, new value, timestamp, and responsible user.

### Scenario 2: Unchanged fields do not generate redundant audit records
- **Given** an existing `AdminUnidad` with ID `10`, username `"admin_famaf"`, name `"Maria"`, surname `"Gomez"`, email `"maria.gomez@famaf.unc.edu.ar"`, cargo `"SECRETARIA_ACADEMICA"`, and unidad ID `1`
- **When** the administrator submits a PUT request modifying only `correo` to `"maria.nueva@famaf.unc.edu.ar"` while keeping `username`, `nombre`, `apellido`, `cargoNombre`, and `unidadId` unchanged
- **Then** the response status is `200 OK`
- **And** exactly 1 audit entry for field `correo` is persisted in the `cambios` table.

### Scenario 3: Target Unit Administrator not found
- **Given** an authenticated user with role `ADMINISTRADOR_GLOBAL`
- **When** a PUT request is submitted for non-existent ID `9999`
- **Then** the response status must be `404 Not Found` with message `"Administrador de Unidad no encontrado"`.

### Scenario 4: Target Unidad not found
- **Given** an authenticated user with role `ADMINISTRADOR_GLOBAL`
- **And** an existing `AdminUnidad` with ID `10`
- **When** a PUT request is submitted with a non-existent `unidadId: 9999`
- **Then** the response status must be `404 Not Found` with message `"Unidad no encontrada"`
- **And** no data changes or audit records are persisted.

### Scenario 5: Username or email collision with another user
- **Given** another registered user with username `"otro_usuario"` and email `"otro@unc.edu.ar"`
- **When** the administrator attempts to update `AdminUnidad` setting `username` to `"otro_usuario"` or `correo` to `"otro@unc.edu.ar"`
- **Then** the response status must be `409 Conflict` with appropriate error message
- **And** no modifications or audit logs are persisted.

### Scenario 6: Validation errors on required fields
- **Given** an update request with blank `nombre`, blank `apellido`, blank `username`, invalid email format, null `cargoNombre`, or null `unidadId`
- **When** the request is sent to `/api/usuarios/admin-unidad/{id}`
- **Then** the response status must be `400 Bad Request` containing validation details.

### Scenario 7: Unauthorized access
- **Given** an unauthenticated request or an authenticated user with role `DOCENTE` or `ADMIN_UNIDAD`
- **When** a PUT request is sent to `/api/usuarios/admin-unidad/{id}`
- **Then** the response status must be `401 Unauthorized` or `403 Forbidden`.

---

## Test Plan

### Context
Requires running test database or mocked repositories for `AdminUnidadRepository`, `UsuarioRepository`, `UnidadRepository`, and `CambioRepository`.

### Scenarios

| # | Scenario Type | Target / Input | Expected Result |
|---|---|---|---|
| 1 | Success | Full update of all fields with valid new values | `200 OK`, entity updated, audit records registered |
| 2 | Partial change | Update only email and cargo | `200 OK`, entity updated, exactly 2 audit records logged |
| 3 | Error (404) | ID `999` not found | `404 Not Found` |
| 4 | Error (404) | Non-existent `unidadId` | `404 Not Found` ("Unidad no encontrada") |
| 5 | Error (409) | `username` collision with another user | `409 Conflict` |
| 6 | Error (409) | `correo` collision with another user | `409 Conflict` |
| 7 | Error (400) | Missing required fields or invalid email | `400 Bad Request` |
| 8 | Security | Call with unauthorized role (e.g. DOCENTE) | `403 Forbidden` |

---

## Post-implementation Validation

1. `mvn test -Dtest=AdminUnidadServiceTest`
2. `mvn test -Dtest=UsuarioControllerTest`
3. `./mvnw test`
4. Verify audit entries in table `cambios` when testing via integration/end-to-end endpoints.

---

## Non-functional notes

- **Auditing Integrity:** The update and audit insertions should be transactional (`@Transactional`) to prevent state desynchronization between user state and the change history.
- **Security:** Access control strictly enforced via Spring Security `@PreAuthorize`.
