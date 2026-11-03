# User Story Description

The system must allow Global Administrators and Unit Administrators to modify and update a teacher's cargo and designation details. Unit Administrators are strictly restricted to updating cargos/designations belonging to their assigned academic unit. Any changes made to cargo attributes (such as category, dedication, character, resolution number, start/end dates) are registered in the existing `cambios` audit table.

---

### # Story (INVEST)

**As a** Global Administrator or Unit Administrator  
**I want** to update the cargo and designation details associated with a teacher  
**So that** academic assignments and legal resolutions remain current and all changes are audited in the `cambios` log.

---

### # Analysis

#### Approach

- Exposed via a REST endpoint `PUT /docentes/{docenteId}/cargos/{cargoId}` (or integration with cargo modification endpoint ensuring teacher relationship).
- Authorization verification:
  - If `AdminGlobal`: can modify any teacher's cargo.
  - If `AdminUnidad`: verifies that the target cargo belongs to the administrator's `Unidad` and is assigned to the teacher via active `Designacion`.
- Modifiable attributes:
  - `Designacion`: `designacionNumeroResolucion`, `designacionFechaInicio`, `designacionFechaFin`, `designacionEstadoActual`.
  - `Cargo`: `categoriaId`, `dedicacionId`, `caracterId`, `origenId`.
- Audit in `cambios` table:
  - Each modified attribute records a row in `cambios` with `tabla_afectada` (`"cargos"` or `"designaciones"`), field name, previous value, new value, responsible user ID, and date.

---

### # Acceptance Criteria

**Scenario 1: Successful modification of teacher cargo by Unit Administrator (Happy Path)**
* **Given** an existing teacher with a cargo in Unit A
* **And** the authenticated user is an Administrator of Unit A
* **When** they update the cargo resolution number or dates
* **Then** the record is updated in the database
* **And** the changes are recorded in the `cambios` table
* **And** the system returns HTTP 200 with the updated details.

**Scenario 2: Unit Administrator attempts to modify cargo of another unit (Unhappy Path - Forbidden)**
* **Given** a cargo belonging to Unit B
* **And** the authenticated user is an Administrator of Unit A
* **When** they attempt to update this cargo
* **Then** the system rejects the request
* **And** returns HTTP 403 Forbidden.

**Scenario 3: Cargo not associated with specified teacher (Unhappy Path)**
* **Given** a cargo with ID `{cargoId}` that is not designated to teacher `{docenteId}`
* **When** the administrator attempts the modification via `/docentes/{docenteId}/cargos/{cargoId}`
* **Then** the system returns HTTP 400 Bad Request or 404 Not Found.

---

### # Test Plan

| # | Type | Description |
|---|---|---|
| 1 | Success | Unit Admin updates cargo/designation attributes for a teacher in their unit; verify DB and `cambios` entries. |
| 2 | Error | Unit Admin attempts to update a cargo belonging to a different unit; verify HTTP 403 Forbidden. |
| 3 | Error | Attempt to update cargo not linked to the specified teacher; verify HTTP 400/404 error. |

---

### # Non-functional notes

- Audit records must be created atomically alongside the entity updates.
