# spec-01-baja-materia: Baja lógica de una materia

## User Story Description

As a Global Administrator (`ADMIN_GLOBAL`) or Academic Unit Administrator (`ADMIN_UNIDAD`), I need to decommission / mark as inactive an existing subject (`Materia`) in the system, so that obsolete or discontinued academic subjects are disabled from future cátedra offerings while preserving full historical academic records, adhering to academic unit security boundaries, and preventing decommission when active cátedras are running.

---

## Story (INVEST)

```
**As a** Global Administrator (ADMIN_GLOBAL) or Academic Unit Administrator (ADMIN_UNIDAD)  
**I want to** decommission (dar de baja) an existing subject that has no active cátedras  
**So that** the subject ceases to be available for new courses while keeping its full historical record intact
```

---

## Analysis

### Approach
1. **Vertical slice**: Expose a dedicated lifecycle transition endpoint `PATCH /api/materias/{id}/baja`.
2. **Business Rules**:
   - **Existence**: The subject identified by `id` must exist in the database (HTTP `404 Not Found` if missing).
   - **Role Authorization & Scoping (Decision R2)**:
     - `ADMIN_GLOBAL` can decommission subjects across any academic unit.
     - `ADMIN_UNIDAD` can only decommission subjects belonging to their assigned academic unit (`materia.getUnidad().getUnidadId().equals(userUnidadId)`). If they attempt to decommission a subject from another unit, the backend returns HTTP `403 Forbidden`.
   - **Active Cátedra Impact Check (Decision R1)**:
     - The subject must **not** have active cátedras.
     - Evaluated via `catedraRepository.existsByMateria_IdAndEstadoIgnoreCase(id, "ACTIVA")`.
     - If active cátedras exist, the system rejects the operation with HTTP `409 Conflict` ("No se puede dar de baja la materia porque tiene cátedras activas asociadas").
     - If the subject has no cátedras or only finished/inactive cátedras, the operation is permitted.
   - **Current State Check**:
     - If the subject already has state `"Dada de baja"`, the system rejects the request with HTTP `400 Bad Request` ("La materia ya se encuentra dada de baja").
   - **Persistence / Soft Delete Strategy (Decision B1)**:
     - `Materia` incorporates the lifecycle attribute `materia_estado` (`estado`: `"Activa"` / `"Dada de baja"`).
     - Upon decommission, `materia.estado` transitions to `"Dada de baja"`.
     - When created (RF-24), new subjects default to `"Activa"`.
     - Historical relations (`Catedra`, plans, etc.) remain intact.
   - **Response Format (Decision M1)**:
     - Returns HTTP `200 OK` with the updated `MateriaDetailDTO` (including `estado = "Dada de baja"`).
   - **Atomicity**: The decommission runs within a single `@Transactional` boundary.

---

## Starting Point

| Component | Detail |
| --------- | ------ |
| **Project** | `atlas-backend` |
| **Classes** | `com.atlas.atlas_backend.materia.MateriaController`<br>`com.atlas.atlas_backend.materia.MateriaService`<br>`com.atlas.atlas_backend.materia.MateriaRepository`<br>`com.atlas.atlas_backend.materia.MateriaDetailDTO`<br>`com.atlas.atlas_backend.catedra.CatedraRepository` |
| **Method / Endpoint** | `PATCH /api/materias/{id}/baja` |
| **Payload / Response** | Request body: Empty<br>Response: HTTP `200 OK` with `MateriaDetailDTO` |

---

## Context

- Raw Requirement: [raw-requirement.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-26/raw-requirement.md)
- Open Questions: [open-questions.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-26/open-questions.md)
- Solution Diagrams: [solution-diagrams.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-26/solution-diagrams.md)

---

## Acceptance Criteria

### Scenario 1: Decommissioning an existing subject without active cátedras successfully (Happy Path)
* **Given** an authenticated user with role `ADMIN_GLOBAL` (or `ADMIN_UNIDAD` assigned to unit `1`)
* **And** an existing subject with ID `101`, state `"Activa"`, belonging to unit `1`
* **And** the subject has no active cátedras (`Catedra`) associated
* **When** the user sends a `PATCH /api/materias/101/baja` request
* **Then** the system responds with HTTP `200 OK`
* **And** the subject in the database is updated to `materia_estado = "Dada de baja"`
* **And** the response body contains `MateriaDetailDTO` with `id: 101` and `estado: "Dada de baja"`.

### Scenario 2: Decommissioning a subject with only inactive/finished cátedras successfully
* **Given** an authenticated user with role `ADMIN_GLOBAL` or authorized `ADMIN_UNIDAD`
* **And** an existing subject with ID `102` having state `"Activa"` and cátedras with status `"INACTIVA"` / `"FINALIZADA"` (no active cátedras)
* **When** the user sends a `PATCH /api/materias/102/baja` request
* **Then** the system responds with HTTP `200 OK`
* **And** the subject state is updated to `"Dada de baja"`.

### Scenario 3: Error when attempting to decommission a subject with active cátedras (Conflict)
* **Given** an authenticated user with role `ADMIN_GLOBAL` or authorized `ADMIN_UNIDAD`
* **And** an existing subject with ID `103` linked to at least one active cátedra (`estado = "ACTIVA"`)
* **When** the user sends a `PATCH /api/materias/103/baja` request
* **Then** the system rejects the operation with HTTP `409 Conflict`
* **And** returns an error message indicating that the subject cannot be decommissioned because it has active cátedras associated
* **And** the subject state in the database remains `"Activa"`.

### Scenario 4: Error when subject is already decommissioned
* **Given** an authenticated user with role `ADMIN_GLOBAL` or authorized `ADMIN_UNIDAD`
* **And** an existing subject with ID `104` that already has state `"Dada de baja"`
* **When** the user sends a `PATCH /api/materias/104/baja` request
* **Then** the system rejects the operation with HTTP `400 Bad Request`
* **And** returns an error message stating "La materia ya se encuentra dada de baja".

### Scenario 5: Error when subject does not exist
* **Given** an authenticated administrator
* **When** the user sends a `PATCH /api/materias/99999/baja` request for a non-existent subject
* **Then** the system responds with HTTP `404 Not Found`
* **And** returns an error message indicating that the subject was not found.

### Scenario 6: Error when ADMIN_UNIDAD attempts to decommission a subject of another unit (Security)
* **Given** an authenticated user with role `ADMIN_UNIDAD` assigned to unit `1`
* **And** an existing subject with ID `201` belonging to unit `2`
* **When** the user sends a `PATCH /api/materias/201/baja` request
* **Then** the system rejects the request with HTTP `403 Forbidden`
* **And** returns an error message stating that the user does not have permissions to decommission subjects in that academic unit
* **And** the subject in unit `2` remains unchanged.

---

## Test Plan

#### Context
Tests require Spring Security context with roles `ADMIN_GLOBAL` and `ADMIN_UNIDAD`, existing `Materia` entities in the repository, and mocked/seeded `Catedra` records.

#### Scenarios
| Scenario ID | Type | Description | Expected Status |
|---|---|---|---|
| TC-01 | Happy Path | Global admin decommissions a subject without cátedras | HTTP 200 OK (`estado: "Dada de baja"`) |
| TC-02 | Happy Path | Unit admin decommissions a subject of their own unit | HTTP 200 OK (`estado: "Dada de baja"`) |
| TC-03 | Happy Path | Decommission subject with only inactive cátedras | HTTP 200 OK (`estado: "Dada de baja"`) |
| TC-04 | Conflict | Decommission subject with active cátedras | HTTP 409 Conflict |
| TC-05 | Bad Request | Decommission subject that is already decommissioned | HTTP 400 Bad Request |
| TC-06 | Not Found | Decommission non-existent subject | HTTP 404 Not Found |
| TC-07 | Forbidden | Unit admin decommissions a subject from another unit | HTTP 403 Forbidden |
| TC-08 | Unauthorized | Unauthenticated request | HTTP 401 Unauthorized |
