# User Story Description

The system must allow a Global Administrator or Unit Administrator to register a new subject ("materia") in an academic unit. The registration captures the subject's name, weekly hours workload (between 1 and 40), academic year level formatted as an ordinal string (e.g. "1°", "2°", "3°", "4°", "5°"), deployment schedule ("Anual", "Primer Semestre", or "Segundo Semestre"), academic unit and dependency, and the study plan year ("N° de Plan de Estudio", e.g. 2023). The system enforces composite uniqueness on `(nombre, unidad, planEstudio)` to prevent duplicate subjects in the same study plan and unit, enforces strict unit-level scoping for Unit Administrators (both UI pre-selection and backend verification), and provides immediate confirmation upon success.

---

# Story (INVEST)

**As a** Global Administrator or Unit Administrator  
**I want** to register a new academic subject (materia) with its weekly hours, academic year level, deployment, academic unit, dependency, and study plan year  
**So that** the academic unit can organize its study plan curricula and later establish teaching chairs (cátedras) and positions (cargos).

---

# Analysis

#### Approach

This vertical slice covers the endpoint `POST /api/materias` to create and persist a new `Materia` entity.
- The request payload `MateriaCreateRequest` contains: `nombre`, `horas`, `anio`, `despliegue`, `unidadId`, `dependenciaId` (optional/validated), and `planEstudio` (representing the plan year, e.g. 2023).
- **Validations**:
  - `nombre`: non-blank, max 50 characters.
  - `horas`: positive integer between 1 and 40.
  - `anio`: string ordinal representing the year of the career (e.g. `"1°"`, `"2°"`, `"3°"`, `"4°"`, `"5°"`).
  - `despliegue`: must be one of `"Anual"`, `"Primer Semestre"`, `"Segundo Semestre"`.
  - `planEstudio`: positive integer representing a valid year (e.g., between 1900 and 2100).
  - `unidadId`: mandatory valid ID of an existing `Unidad`.
- **Authorization & Unit Scoping**:
  - `ADMIN_GLOBAL` can register subjects for any academic unit.
  - `ADMIN_UNIDAD` can only register subjects for their own assigned academic unit. In the UI, the dependency and unit dropdowns are fixed/locked to their assigned values. If an `ADMIN_UNIDAD` attempts to submit a different `unidadId`, the backend rejects with `403 Forbidden`.
- **Uniqueness Check**:
  - The system checks if a subject with the same `nombre` (case-insensitive) already exists for the same `unidadId` AND `planEstudio`. If so, it rejects with `409 Conflict`.
- On success, it persists the new `Materia` and returns HTTP 201 Created with `MateriaDetailDTO`.

#### In-Scope
- Payload data validation according to rules from open questions.
- Scoping verification for `ADMIN_UNIDAD`.
- Uniqueness validation for `(nombre, unidadId, planEstudio)`.
- Persistence in database table `materias` and return of created resource.

#### Out-of-Scope
- Creating chairs (`Catedra`) associated with the new subject (covered by separate requirement).
- Modification or deletion of subjects.

---

# Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` |
| **Classes** | `src/main/java/com/atlas/atlas_backend/materia/MateriaController.java`<br>`src/main/java/com/atlas/atlas_backend/materia/MateriaService.java`<br>`src/main/java/com/atlas/atlas_backend/materia/MateriaRepository.java`<br>`src/main/java/com/atlas/atlas_backend/materia/Materia.java` |
| **Method / mapping** | `POST /api/materias` (`MateriaController.crearMateria`)<br>`MateriaService.crearMateria` |

---

# Context

See `raw-requirement.md` for the input fields and `open-questions.md` for resolved business and technical decisions.

---

# Acceptance Criteria

**Scenario 1: Successful creation of a new subject by Global Administrator (Happy path)**
* **Given** the authenticated user has the role `ADMIN_GLOBAL`
* **And** an academic unit exists with `unidadId` = `1`
* **And** no subject with name `"Álgebra Lineal"` exists in unit `1` for study plan `2023`
* **When** a `POST` request is sent to `/api/materias` with:
  | Field | Value |
  | `nombre` | `"Álgebra Lineal"` |
  | `horas` | `6` |
  | `anio` | `"1°"` |
  | `despliegue` | `"Primer Semestre"` |
  | `unidadId` | `1` |
  | `planEstudio` | `2023` |
* **Then** the system persists the new subject
* **And** returns HTTP status `201 Created` with the registered subject details including its generated `id`.

**Scenario 2: Successful creation of a new subject by Unit Administrator within assigned unit**
* **Given** the authenticated user has the role `ADMIN_UNIDAD` assigned to unit `2`
* **And** no subject with name `"Estructuras de Datos"` exists in unit `2` for study plan `2020`
* **When** a `POST` request is sent to `/api/materias` with:
  | Field | Value |
  | `nombre` | `"Estructuras de Datos"` |
  | `horas` | `4` |
  | `anio` | `"2°"` |
  | `despliegue` | `"Anual"` |
  | `unidadId` | `2` |
  | `planEstudio` | `2020` |
* **Then** the system persists the subject under unit `2`
* **And** returns HTTP status `201 Created`.

**Scenario 3: Same subject name allowed in different study plans of the same unit**
* **Given** a subject with name `"Cálculo I"` exists in unit `1` under study plan `2015`
* **When** a `POST` request is sent to `/api/materias` with `nombre` = `"Cálculo I"`, `unidadId` = `1`, and `planEstudio` = `2023`
* **Then** the system persists the new subject for study plan `2023`
* **And** returns HTTP status `201 Created`.

**Scenario 4: Creation rejected when Unit Administrator attempts to register for another unit (Authorization failure)**
* **Given** the authenticated user has the role `ADMIN_UNIDAD` assigned to unit `1`
* **When** a `POST` request is sent to `/api/materias` with `unidadId` = `2`
* **Then** the subject is not persisted
* **And** the system responds with HTTP status `403 Forbidden` and an error message indicating lack of permission for that academic unit.

**Scenario 5: Creation rejected when subject already exists in the same unit and study plan (Duplicate check)**
* **Given** a subject with name `"Álgebra Lineal"` already exists in unit `1` for study plan `2023`
* **When** a `POST` request is sent to `/api/materias` with `nombre` = `"álgebra lineal"`, `unidadId` = `1`, and `planEstudio` = `2023`
* **Then** the subject is not persisted
* **And** the system responds with HTTP status `409 Conflict` and an error message indicating that the subject already exists for this unit and study plan.

**Scenario 6: Creation rejected when hours are out of range (Validation error)**
* **Given** an authenticated administrator
* **When** a `POST` request is sent to `/api/materias` with `horas` = `0` or `horas` = `45`
* **Then** the subject is not created
* **And** the system responds with HTTP status `400 Bad Request` indicating that weekly hours must be between 1 and 40.

**Scenario 7: Creation rejected when deployment value is invalid**
* **Given** an authenticated administrator
* **When** a `POST` request is sent to `/api/materias` with `despliegue` = `"Trimestral"`
* **Then** the subject is not created
* **And** the system responds with HTTP status `400 Bad Request` indicating that deployment must be `"Anual"`, `"Primer Semestre"`, or `"Segundo Semestre"`.

**Scenario 8: Creation rejected when target unit does not exist**
* **Given** an authenticated `ADMIN_GLOBAL`
* **When** a `POST` request is sent to `/api/materias` with a non-existent `unidadId` = `9999`
* **Then** the system responds with HTTP status `404 Not Found` with an error message indicating that the academic unit was not found.

---

# Test Plan

#### Context
Requires running database or test fixtures with at least two `Unidad` records (`unidad_id = 1` and `unidad_id = 2`) and users with `ADMIN_GLOBAL` and `ADMIN_UNIDAD` roles.

#### Scenarios

| # | Type | Description | Expected Status |
|---|------|-------------|-----------------|
| 1 | Positive | Global admin registers valid new subject with annual deployment | `201 Created` |
| 2 | Positive | Unit admin registers valid subject for their own unit with semester deployment | `201 Created` |
| 3 | Positive | Subject with same name created in a different study plan | `201 Created` |
| 4 | Negative | Unit admin registers subject for a different unit | `403 Forbidden` |
| 5 | Negative | Registering subject with duplicate `(nombre, unidadId, planEstudio)` | `409 Conflict` |
| 6 | Negative | Payload with invalid hours (`horas < 1` or `horas > 40`) | `400 Bad Request` |
| 7 | Negative | Payload with invalid `despliegue` | `400 Bad Request` |
| 8 | Negative | Payload with non-existent `unidadId` | `404 Not Found` |
| 9 | Negative | Unauthenticated request | `401 Unauthorized` |

---

# Post-implementation Validation

1. Run backend unit & integration tests:
   ```powershell
   .\mvnw test -Dtest=MateriaServiceTest,MateriaControllerTest
   ```
2. Verify Spring Boot build:
   ```powershell
   .\mvnw clean test
   ```

---

# Non-functional notes

- **Security & Multi-tenancy**: Academic unit scoping is strictly enforced for `ADMIN_UNIDAD` via Spring Security and service layer checks.
- **Data Integrity**: Enforce composite uniqueness `(materia_nombre, unidad_id, materia_plan_estudio)` and `@Transactional` boundaries on create operations.
