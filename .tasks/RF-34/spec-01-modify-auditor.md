# User Story – Modify Auditor Information

## User Story Description

Allows a Global Administrator to modify personal data (`nombre`, `apellido`), contact data (`correo`, `username`), and assigned institutional position (`cargoNombre` from `CargoAuditor`) of any registered Auditor (`Auditor`). The system ensures data integrity, validates format constraints, enforces uniqueness across users for username and email (excluding the target auditor), and registers audit log records in the `cambios` table for each changed field.

---

## Story (INVEST)

```
As a Global Administrator
I want to update the personal information, contact details, and cargo of an Auditor
So that their contact information and institutional auditing role remain accurate, up-to-date, and fully audited
```

---

## Analysis

### Approach
- **REST Endpoint:** `PUT /api/usuarios/auditor/{id}`
- **Authorization:** Restricted to `ADMIN_GLOBAL` / `ADMINISTRADOR_GLOBAL`.
- **Fields modifiable:**
  - Personal data: `nombre` (not blank, max 50), `apellido` (not blank, max 50)
  - Account & Contact data: `username` (not blank, max 50, unique across `usuarios` excluding current record), `correo` (valid email format, unique across `usuarios` excluding current record)
  - Position: `cargoNombre` (`CargoAuditor`: `DECANO`, `VICEDECANO`, `SECRETARIA_ACADEMICA`)
- **Auditing:**
  - Track deltas via `CambioService.registrarCambio("auditores", campo, valorAnterior, valorNuevo, usuarioResponsable)` for fields: `username`, `nombre`, `apellido`, `correo`, `cargo_au_nombre`.
  - Only genuine changes (`valorAnterior != valorNuevo`) persist to the `cambios` table.

### Scope & Domain Boundaries
- **In-scope:** Updating `nombre`, `apellido`, `username`, `correo`, `cargoNombre`, change auditing in `cambios`.
- **Out-of-scope:** Password reset (handled via password recovery/reset workflows), account activation/deactivation (handled by status management endpoints).

---

## Starting Point

| | |
|---|---|
| **Project** | `atlas-backend` |
| **Classes** | `com.atlas.atlas_backend.usuario.UsuarioController`<br>`com.atlas.atlas_backend.usuario.AuditorService`<br>`com.atlas.atlas_backend.usuario.AuditorUpdateRequest`<br>`com.atlas.atlas_backend.usuario.Auditor`<br>`com.atlas.atlas_backend.cambio.CambioService` |
| **Method / mapping** | `PUT /api/usuarios/auditor/{id}` calling `AuditorService.modificarAuditor(id, request, responsibleUsername)` |

---

## Context

- `raw-requirement.md`: [raw-requirement.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-34/raw-requirement.md)
- `open-questions.md`: [open-questions.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-34/open-questions.md)

---

## Acceptance Criteria

### Scenario 1: Successfully modify Auditor (Happy Path)
- **Given** an authenticated user with role `ADMINISTRADOR_GLOBAL`
- **And** an existing `Auditor` with ID `15`, username `"auditor_unc"`, name `"Carlos"`, surname `"Lopez"`, email `"carlos.lopez@unc.edu.ar"`, and cargo `"VICEDECANO"`
- **When** the administrator submits a PUT request to `/api/usuarios/auditor/15` with:
  ```json
  {
    "username": "carlos_lopez_decano",
    "nombre": "Carlos Alberto",
    "apellido": "Lopez Martinez",
    "correo": "c.lopez@unc.edu.ar",
    "cargoNombre": "DECANO"
  }
  ```
- **Then** the response status must be `200 OK`
- **And** the `Auditor` record in the database is updated with the new values
- **And** 5 audit entries are logged in the `cambios` table (`tablaAfectada = "auditores"`, for fields `username`, `nombre`, `apellido`, `correo`, `cargo_au_nombre`) recording old values, new values, timestamp, and responsible user.

### Scenario 2: Unchanged fields do not generate redundant audit records
- **Given** an existing `Auditor` with ID `15`, username `"auditor_unc"`, name `"Carlos"`, surname `"Lopez"`, email `"carlos.lopez@unc.edu.ar"`, and cargo `"VICEDECANO"`
- **When** the administrator submits a PUT request modifying only `correo` to `"carlos.nuevo@unc.edu.ar"` while keeping `username`, `nombre`, `apellido`, and `cargoNombre` unchanged
- **Then** the response status is `200 OK`
- **And** exactly 1 audit entry for field `correo` is persisted in the `cambios` table.

### Scenario 3: Target Auditor not found
- **Given** an authenticated user with role `ADMINISTRADOR_GLOBAL`
- **When** a PUT request is submitted for non-existent ID `9999`
- **Then** the response status must be `404 Not Found` with message `"Auditor no encontrado"`
- **And** no audit logs or database records are modified.

### Scenario 4: Username collision with another user
- **Given** another registered user with username `"juan_perez"`
- **When** the administrator attempts to update `Auditor` setting `username` to `"juan_perez"`
- **Then** the response status must be `409 Conflict` with message `"El nombre de usuario ya se encuentra en uso"`
- **And** no data changes or audit records are persisted.

### Scenario 5: Email collision with another user
- **Given** another registered user with email `"juan@unc.edu.ar"`
- **When** the administrator attempts to update `Auditor` setting `correo` to `"juan@unc.edu.ar"`
- **Then** the response status must be `409 Conflict` with message `"El correo electrónico ya se encuentra en uso"`
- **And** no data changes or audit records are persisted.

### Scenario 6: Validation failure on invalid request fields
- **Given** an authenticated user with role `ADMINISTRADOR_GLOBAL`
- **When** a PUT request is submitted with blank `nombre`, malformed `correo`, or null `cargoNombre`
- **Then** the response status must be `400 Bad Request`
- **And** no changes are persisted to the database.

### Scenario 7: Unauthorized access by non-global admin
- **Given** an authenticated user with role `DOCENTE` or `ADMIN_UNIDAD` or `AUDITOR`
- **When** they attempt to execute `PUT /api/usuarios/auditor/15`
- **Then** the response status must be `403 Forbidden`.
