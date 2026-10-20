# spec-01-baja-cargo: Baja lógica de un cargo

## User Story Description

As a Global Administrator (`ADMIN_GLOBAL`), I need to decommission / mark as inactive an existing position (`Cargo`) in the system, so that positions that are no longer needed or valid are terminated while ensuring that no position with an active teacher assignment is prematurely closed, and preserving full historical auditing data.

---

## Story (INVEST)

```
**As a** Global Administrator (ADMIN_GLOBAL)  
**I want to** decommission (dar de baja) an existing position that has no active appointment  
**So that** the position ceases to be available for new assignments while keeping its full historical record in the system
```

---

## Analysis

### Approach
1. **Vertical slice**: Expose a dedicated transactional endpoint `PATCH /api/cargos/{id}/baja` that transitions `cargo_estado` to `"Dado de baja"`.
2. **Business Rules**:
   - **Existence**: The cargo identified by `id` must exist in the database (HTTP `404 Not Found` if missing).
   - **Role Authorization (Decision B3)**: Only users with role `ADMIN_GLOBAL` can execute the decommission operation. Other roles (such as `ADMIN_UNIDAD` or `DOCENTE`) receive HTTP `403 Forbidden`.
   - **Active Appointment / Designación Vigente Validation (Decision B1)**:
     - The cargo must **not** be linked to an active assignment.
     - An assignment is considered active/vigente if `cargo.getDesignacion() != null` AND:
       - `designacion.getDesignacionEstadoActual()` is active (not "Finalizada", "Cancelada" or "Baja"), AND
       - (`designacion.getDesignacionFechaFin() == null` OR `designacion.getDesignacionFechaFin().isAfter(LocalDate.now())` OR `designacion.getDesignacionFechaFin().isEqual(LocalDate.now())`).
     - If an active assignment exists, the system rejects the operation with HTTP `400 Bad Request` (or `409 Conflict`) indicating that active assignments must be terminated or unlinked first.
   - **Current State Check (Decision R1)**:
     - If the cargo already has state `"Dado de baja"`, the system rejects the request with HTTP `400 Bad Request` ("El cargo ya se encuentra dado de baja").
   - **Subdivided Positions & Historical Data Preservation (Decision R2)**:
     - If the cargo has been subdivided into child subcargos (`cargoOrigenDivisionId = cargo.id`), the system checks if any of the child subcargos are active (i.e. state != `"Dado de baja"`). If at least one subcargo is active, decommission of the parent is blocked with HTTP `400 Bad Request` ("No se puede dar de baja el cargo porque posee subcargos activos derivados de una división").
     - When decommissioned, `cargo.estado` is set to `"Dado de baja"`.
     - `cargo.horasOcupadas` is reset to `0`.
     - All historical references (`cargoPredecesor`, `cargoOrigenDivision`, `unidad`, `categoria`, `dedicacion`, etc.) remain intact.
   - **Active Positions Search Exclusion (Decision R3)**:
     - Positions with state `"Dado de baja"` are permanently excluded from the active positions search (`findActiveCargos`), even when the `estado` parameter is omitted (`null`). Decommissioned positions are reserved exclusively for the future History/Audit page (`Historial`).
   - **Atomicity**: The update executes inside a single `@Transactional` boundary.

---

## Starting Point

| Component | Detail |
| --------- | ------ |
| **Project** | `atlas-backend` |
| **Classes** | `com.atlas.atlas_backend.cargo.CargoController`<br>`com.atlas.atlas_backend.cargo.CargoService`<br>`com.atlas.atlas_backend.cargo.CargoRepository` |
| **Payload / Response** | Request: None / Empty body<br>Response: `CargoDetailDTO` with updated status `"Dado de baja"` |

---

## Acceptance Criteria

### Scenario 1: Decommissioning an existing cargo without assignment successfully (Happy Path)
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **And** an existing cargo with ID `101`, state `"Libre"`, and no linked designation (`designacion = null`)
* **And** the cargo has no active child subcargos originating from it
* **When** the user sends a `PATCH /api/cargos/101/baja` request
* **Then** the system responds with HTTP `200 OK`
* **And** the cargo in the database has `cargo_estado = "Dado de baja"`
* **And** the response returns the updated cargo details showing state `"Dado de baja"`.

### Scenario 2: Decommissioning a cargo with an expired or finalized assignment successfully
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **And** an existing cargo with ID `102` having a linked designation with status `"Finalizada"` and end date in the past
* **And** the cargo has no active child subcargos originating from it
* **When** the user sends a `PATCH /api/cargos/102/baja` request
* **Then** the system responds with HTTP `200 OK`
* **And** the cargo in the database is updated to state `"Dado de baja"`.

### Scenario 3: Error when attempting to decommission a cargo with an active assignment (Unhappy Path)
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **And** an existing cargo with ID `103` linked to an active designation (`designacion_estado_actual = "Activa"`, end date in the future or null)
* **When** the user sends a `PATCH /api/cargos/103/baja` request
* **Then** the system rejects the operation with HTTP `400 Bad Request`
* **And** returns an error message indicating that the position cannot be decommissioned while linked to an active assignment
* **And** the cargo state in the database remains unchanged.

### Scenario 4: Error when cargo does not exist
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **When** the user sends a `PATCH /api/cargos/99999/baja` request for a non-existent cargo
* **Then** the system responds with HTTP `404 Not Found`
* **And** returns an error message indicating the cargo was not found.

### Scenario 5: Error when cargo is already decommissioned
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **And** an existing cargo with ID `104` with state `"Dado de baja"`
* **When** the user sends a `PATCH /api/cargos/104/baja` request
* **Then** the system rejects the request with HTTP `400 Bad Request`
* **And** indicates that the position is already decommissioned.

### Scenario 6: Access denied for Unit Admin or unauthorized roles
* **Given** an authenticated user with role `ADMIN_UNIDAD` or `DOCENTE`
* **When** the user sends a `PATCH /api/cargos/101/baja` request
* **Then** the system rejects the request with HTTP `403 Forbidden`.

### Scenario 7: Error when cargo has active subcargos derived from a division
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **And** an existing parent cargo with ID `105` that has child subcargos in state `"Libre"` or `"Asignado"`
* **When** the user sends a `PATCH /api/cargos/105/baja` request
* **Then** the system rejects the operation with HTTP `400 Bad Request`
* **And** returns an error message indicating that the position cannot be decommissioned because it has active subdivided subcargos.

### Scenario 8: Decommissioned positions are excluded from active search
* **Given** an existing cargo with state `"Dado de baja"`
* **When** a user performs a search for active positions via `GET /api/cargos` without specifying an `estado` parameter or with `estado != 'Dado de baja'`
* **Then** the decommissioned cargo is not included in the paginated results list.
