# User Story Description

The system must allow Global Administrators and Unit Administrators to modify and update teaching functions (`funciones`) assigned to a teacher's cargo. Unit Administrators can only modify functions linked to cargos belonging to their assigned academic unit. Every modification to teaching functions (such as hours assigned, function name, or associated subject/materia) is registered in the existing `cambios` audit table.

---

### # Story (INVEST)

**As a** Global Administrator or Unit Administrator  
**I want** to modify the teaching functions assigned to a teacher's cargo  
**So that** workload hours and course assignments are accurate and changes are tracked in the `cambios` table.

---

### # Analysis

#### Approach

- Exposed via a REST endpoint `PUT /docentes/{docenteId}/funciones/{funcionId}` (or `PUT /funciones/{funcionId}`).
- Authorization verification:
  - If `AdminGlobal`: can modify functions across all units.
  - If `AdminUnidad`: verifies that the cargo associated with the function belongs to the administrator's `Unidad`. Otherwise, HTTP 403 Forbidden.
- Modifiable attributes:
  - `nombre` (e.g. Teaching, Research, Extension).
  - `horas` (weekly dedicated hours).
  - `materiaId` (reassignment of subject).
- Audit in `cambios` table:
  - Each modified field creates a record in `cambios` with `tabla_afectada = "funciones"`, field name, old value, new value, responsible user ID, and date.

---

### # Acceptance Criteria

**Scenario 1: Successful modification of a teaching function by Unit Administrator (Happy Path)**
* **Given** an existing teaching function assigned to a teacher's cargo in Unit A
* **And** the authenticated user is an Administrator of Unit A
* **When** they update the assigned hours from `10` to `15`
* **Then** the function is updated in the database
* **And** a row is inserted into `cambios` (`tabla_afectada: "funciones"`, `campo_afectado: "funcion_horas"`, `valor_anterior: "10"`, `valor_nuevo: "15"`)
* **And** the system returns HTTP 200 with the updated function details.

**Scenario 2: Unit Administrator attempts to modify function in another unit (Unhappy Path - Forbidden)**
* **Given** a teaching function belonging to a cargo in Unit B
* **And** the authenticated user is an Administrator of Unit A
* **When** they attempt to update this function
* **Then** the system rejects the request
* **And** returns HTTP 403 Forbidden.

**Scenario 3: Exceeding maximum dedicated hours (Unhappy Path - Validation)**
* **Given** a cargo with dedication of 20 hours
* **When** the administrator attempts to update a function with hours exceeding the remaining dedication allowance
* **Then** the system rejects the modification with an appropriate validation error.

---

### # Test Plan

| # | Type | Description |
|---|---|---|
| 1 | Success | Unit Admin updates hours and name of a function in their unit; verify DB update and `cambios` row. |
| 2 | Error | Unit Admin attempts to update a function belonging to another unit; verify HTTP 403 Forbidden. |
| 3 | Error | Attempt to assign negative or invalid hours; verify HTTP 400 Bad Request. |

---

### # Non-functional notes

- Ensure transactional consistency between function updates and `cambios` record creation.
