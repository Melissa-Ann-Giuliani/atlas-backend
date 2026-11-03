# User Story Description

The system must allow a Global Administrator or Unit Administrator to modify the pedagogical data of an existing subject ("materia"). The modification is performed via partial update (`PATCH /api/materias/{id}`) and allows changing the subject's name, weekly workload hours (between 1 and 40), academic career year (formatted as an ordinal string, e.g. "1°", "2°", "3°"), and deployment schedule ("Anual", "Primer Semestre", or "Segundo Semestre"). The subject's academic unit (`unidad`) and study plan year (`planEstudio`) are strictly immutable once created. The system verifies that the target subject exists (returning 404 if not), enforces unit-level scoping for Unit Administrators (returning 403 Forbidden if attempting to modify subjects outside their assigned unit), validates that modifying `horas` or `despliegue` is disallowed if the subject currently has active chairs (`Catedra`) associated (returning 409 Conflict), verifies that any updated `nombre` does not collide with another subject in the same study plan and unit (`id != materiaId`), and returns the updated subject details with HTTP 200 OK.

---

# Story (INVEST)

**As a** Global Administrator or Unit Administrator  
**I want** to partially modify the pedagogical data of an existing subject (name, hours, year level, and deployment)  
**So that** curricula adjustments, updated academic contents, or scheduling corrections can be kept up to date while protecting institutional integrity (study plan, academic unit, and active chairs).

---

# Analysis

#### Approach

This vertical slice covers the endpoint `PATCH /api/materias/{id}` to partially update an existing `Materia` entity.
- The request payload `MateriaPatchRequest` contains optional fields: `nombre`, `horas`, `anio`, and `despliegue`.
- **Inmutability of Unit and Plan**:
  - `unidad` and `planEstudio` are not modifiable. They are preserved as originally registered.
- **Existence check**:
  - The system searches for the subject by its `id`. If not found, it responds with `404 Not Found`.
- **Authorization & Unit Scoping**:
  - `ADMIN_GLOBAL` can modify subjects belonging to any academic unit.
  - `ADMIN_UNIDAD` can only modify subjects that belong to their own assigned academic unit. If the subject belongs to another unit, the backend rejects the request with `403 Forbidden`.
- **Active Chairs Integrity Check (Impact Validation)**:
  - If the patch request attempts to change `horas` or `despliegue`, the system queries if there are any active chairs associated (`Catedra` with `materia_id = :id` and `estado = 'ACTIVA'`). If active chairs exist, it rejects the request with `409 Conflict` (or `400 Bad Request`) stating that modifying weekly hours or deployment schedule is restricted when active chairs exist.
- **Uniqueness Check**:
  - If `nombre` is present in the request and differs from the current name, the system checks whether another subject with that same name (case-insensitive) already exists for the same `unidad` and `planEstudio` with a different ID (`id != materiaId`). If a collision occurs, it returns `409 Conflict`.
- **Field Validations**:
  - `nombre`: if provided, non-blank, max 50 characters.
  - `horas`: if provided, integer between 1 and 40.
  - `anio`: if provided, non-blank, max 10 characters ordinal format.
  - `despliegue`: if provided, must be one of `"Anual"`, `"Primer Semestre"`, `"Segundo Semestre"`.
- On success, it persists the changes and returns HTTP 200 OK with `MateriaDetailDTO`.

#### In-Scope
- Partial updates via `PATCH /api/materias/{id}` with `MateriaPatchRequest`.
- Subject existence validation (`404 Not Found`).
- Unit scoping for `ADMIN_UNIDAD` (`403 Forbidden`).
- Impact validation against active chairs for `horas` and `despliegue` (`409 Conflict`).
- Uniqueness validation excluding the current subject ID (`409 Conflict`).
- Selective field persistence in database table `materias` and returning updated `MateriaDetailDTO`.

#### Out-of-Scope
- Changing `unidad` or `planEstudio` (strictly immutable).
- Deletion of subjects.
- Modifying chairs or teaching positions directly.

---

# Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` |
| **Classes** | `src/main/java/com/atlas/atlas_backend/materia/MateriaController.java`<br>`src/main/java/com/atlas/atlas_backend/materia/MateriaService.java`<br>`src/main/java/com/atlas/atlas_backend/materia/MateriaRepository.java`<br>`src/main/java/com/atlas/atlas_backend/materia/Materia.java`<br>`src/main/java/com/atlas/atlas_backend/materia/MateriaPatchRequest.java`<br>`src/main/java/com/atlas/atlas_backend/catedra/CatedraRepository.java` |
| **Method / mapping** | `PATCH /api/materias/{id}` (`MateriaController.modificarMateria`)<br>`MateriaService.modificarMateria` |

---

# Context

See `raw-requirement.md` for the original specification, `open-questions.md` for finalized decisions (B1: pedagogical fields only, B2: self-ID exclusion, R1: unit scoping, R2: active chair protection, M1: PATCH method), and `solution-diagrams.md` for sequence and architecture flows.

---

# Acceptance Criteria

**Scenario 1: Successful partial modification of subject name and academic year by Global Administrator (Happy path)**
* **Given** a subject exists with `id` = `10`, `nombre` = `"Álgebra I"`, `horas` = `4`, `anio` = `"1°"`, `despliegue` = `"Primer Semestre"`, `planEstudio` = `2020`, under unit `1`
* **And** the authenticated user has the role `ADMIN_GLOBAL`
* **When** a `PATCH` request is sent to `/api/materias/10` with:
  ```json
  {
    "nombre": "Álgebra Lineal",
    "anio": "2°"
  }
  ```
* **Then** the system updates only `nombre` and `anio`, leaving `horas`, `despliegue`, `planEstudio`, and `unidad` unchanged
* **And** returns HTTP status `200 OK` with the updated subject details.

**Scenario 2: Successful modification of hours and deployment when no active chairs exist**
* **Given** a subject exists with `id` = `15`, `horas` = `4`, `despliegue` = `"Primer Semestre"`, unit `1`
* **And** no active chairs (`Catedra`) are linked to subject `15`
* **When** a `PATCH` request is sent to `/api/materias/15` with:
  ```json
  {
    "horas": 6,
    "despliegue": "Anual"
  }
  ```
* **Then** the system updates `horas` to `6` and `despliegue` to `"Anual"`
* **And** returns HTTP status `200 OK`.

**Scenario 3: Modification of hours rejected when subject has active chairs**
* **Given** a subject exists with `id` = `10`
* **And** an active chair (`Catedra`) with `estado` = `"ACTIVA"` is associated with subject `10`
* **When** a `PATCH` request is sent to `/api/materias/10` with:
  ```json
  {
    "horas": 8
  }
  ```
* **Then** the system rejects the update
* **And** returns HTTP status `409 Conflict` with a message indicating that hours cannot be modified because active chairs are linked to the subject.

**Scenario 4: Modification of deployment schedule rejected when subject has active chairs**
* **Given** a subject exists with `id` = `10`
* **And** an active chair (`Catedra`) with `estado` = `"ACTIVA"` is associated with subject `10`
* **When** a `PATCH` request is sent to `/api/materias/10` with:
  ```json
  {
    "despliegue": "Segundo Semestre"
  }
  ```
* **Then** the system rejects the update
* **And** returns HTTP status `409 Conflict`.

**Scenario 5: Successful modification by Unit Administrator within assigned unit**
* **Given** the authenticated user has role `ADMIN_UNIDAD` assigned to unit `2`
* **And** a subject exists with `id` = `25` belonging to unit `2`
* **When** a `PATCH` request is sent to `/api/materias/25` with `nombre` = `"Estructuras de Datos Avanzadas"`
* **Then** the system updates the subject
* **And** returns HTTP status `200 OK`.

**Scenario 6: Modification rejected when Unit Administrator attempts to modify a subject from another unit**
* **Given** the authenticated user has role `ADMIN_UNIDAD` assigned to unit `2`
* **And** a subject exists with `id` = `10` belonging to unit `1`
* **When** a `PATCH` request is sent to `/api/materias/10` with `nombre` = `"Nuevo Nombre"`
* **Then** the system rejects the request
* **And** returns HTTP status `403 Forbidden` with message `"No tiene permisos para modificar materias en esta unidad académica"`.

**Scenario 7: Modification rejected when subject does not exist**
* **Given** no subject exists with `id` = `9999`
* **When** a `PATCH` request is sent to `/api/materias/9999` with valid data
* **Then** the system returns HTTP status `404 Not Found` with message `"Materia no encontrada"`.

**Scenario 8: Modification rejected due to name collision with another subject in the same unit and study plan**
* **Given** a subject exists with `id` = `10` (`"Física I"`, unit `1`, plan `2023`)
* **And** another subject exists with `id` = `11` (`"Física II"`, unit `1`, plan `2023`)
* **When** a `PATCH` request is sent to `/api/materias/11` with `nombre` = `"Física I"`
* **Then** the system rejects the update
* **And** returns HTTP status `409 Conflict` with message `"Ya existe otra materia con ese nombre para esta unidad y plan de estudio"`.

**Scenario 9: Updating other fields while keeping identical name does not trigger conflict**
* **Given** a subject exists with `id` = `10` (`"Química General"`, unit `1`, plan `2020`)
* **When** a `PATCH` request is sent to `/api/materias/10` with `nombre` = `"Química General"` and `anio` = `"2°"`
* **Then** the system does not trigger a duplicate conflict
* **And** returns HTTP status `200 OK`.

---

# Test Plan

#### Context
Requires running database or Mockito unit/integration test harness with predefined units (`Unidad`), users (`ADMIN_GLOBAL`, `ADMIN_UNIDAD`), chairs (`Catedra`), and test subjects.

#### Scenarios

| # | Type | Description | Expected Status |
|---|---|---|---|
| 1 | Success | Partial update of name and academic year by ADMIN_GLOBAL | 200 OK |
| 2 | Success | Update hours and deployment when no active chairs exist | 200 OK |
| 3 | Success | ADMIN_UNIDAD updates subject belonging to their unit | 200 OK |
| 4 | Success | Updating without changing name preserves uniqueness without self-conflict | 200 OK |
| 5 | Error | Non-existent subject ID | 404 Not Found |
| 6 | Error | ADMIN_UNIDAD attempting to modify a subject from another unit | 403 Forbidden |
| 7 | Error | Attempting to modify hours or deployment when active chairs exist | 409 Conflict |
| 8 | Error | Name collision with another subject in the same unit and plan | 409 Conflict |
| 9 | Error | Invalid field format (e.g., hours `< 1` or `> 40`, invalid deployment string) | 400 Bad Request |

---

# Post-implementation Validation

1. `mvn test -Dtest=Materia*` to verify all subject controller and service unit/slice tests.
2. Verify endpoints with curl or integration tests (`PATCH /api/materias/{id}`).
3. Check SQL logs to verify that `UPDATE materias` updates only the intended columns.

---

# Non-functional notes

- **Security**: Method security enforced via `@PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")`.
- **Integrity**: Transactional execution (`@Transactional`) ensuring atomic update.
