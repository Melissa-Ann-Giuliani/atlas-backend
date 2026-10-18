# User Story Description

The system must allow a Global Administrator or Unit Administrator to create a new academic/teaching position ("cargo") in the institution. The cargo must be uniquely identified by its code, registered in an initial state of "Libre" with 0 occupied hours and no associated designation, and optionally linked to a predecessor cargo. This enables academic departments to organize and publish positions ready for future teacher designations.

---

# Story (INVEST)

**As a** Global Administrator or Unit Administrator  
**I want** to register a new academic position (cargo) in the system with its initial properties and classifications  
**So that** it becomes available in "Libre" status for future staff appointment and designation procedures.

---

# Analysis

#### Approach

This slice covers the backend API endpoint (`POST /api/cargos`) to persist a new `Cargo` entity. The controller receives `CargoCreateRequest`, performs validation, checks permissions (an `AdminUnidad` is restricted to their assigned unit, while `AdminGlobal` can register cargos for any unit), verifies that `codigo` is not already taken, looks up the referenced catalogue entities (`Origen`, `Caracter`, `Categoria`, `Dedicacion`, `Unidad`), and optionally associates a predecessor cargo. Default attributes (`estado = "Libre"`, `horasOcupadas = 0`, `designacion = null`) are enforced by the service layer.

---

# Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` |
| **Classes** | `src/main/java/com/atlas/atlas_backend/cargo/CargoController.java`<br>`src/main/java/com/atlas/atlas_backend/cargo/CargoService.java`<br>`src/main/java/com/atlas/atlas_backend/cargo/CargoRepository.java` |
| **Method / mapping** | `POST /api/cargos` (`CargoController.crearCargo`)<br>`CargoService.crearCargo` |

---

# Context

See `raw-requirement.md` for the original unstructured requirement.

---

# Acceptance Criteria

**Scenario 1: Successful creation of a new cargo by Global Administrator (Happy path)**
* **Given** the authenticated user has the role `ADMIN_GLOBAL`
* **And** no cargo exists with `codigo` = `5001`
* **And** valid entities exist for `origenId`, `caracterId`, `categoriaId`, `dedicacionId`, and `unidadId`
* **When** a `POST` request is sent to `/api/cargos` with:
  | Field | Value |
  | `codigo` | `5001` |
  | `fechaCreacion` | `"2026-10-18"` |
  | `origenId` | `1` |
  | `caracterId` | `1` |
  | `categoriaId` | `1` |
  | `dedicacionId` | `1` |
  | `unidadId` | `1` |
* **Then** the system persists the new cargo
* **And** sets `estado` to `"Libre"`
* **And** sets `horasOcupadas` to `0`
* **And** sets `designacion` to `null`
* **And** returns HTTP status `201 Created` with the created cargo details.

**Scenario 2: Successful creation of a new cargo with predecessor link**
* **Given** the authenticated user has the role `ADMIN_GLOBAL` or `ADMIN_UNIDAD`
* **And** an existing cargo exists with `codigo` = `4001` (even if belonging to a different academic unit)
* **And** no cargo exists with `codigo` = `5002`
* **When** a `POST` request is sent to `/api/cargos` with valid fields and `predecesorCodigo` = `4001`
* **Then** the new cargo is created and its `cargoPredecesor` references the cargo with `codigo` `4001`
* **And** the system responds with HTTP status `201 Created`.

**Scenario 3: Creation rejected when cargo code already exists (Unhappy path)**
* **Given** a cargo with `codigo` = `5001` already exists in the database
* **When** a `POST` request is sent to `/api/cargos` with `codigo` = `5001`
* **Then** the cargo is not persisted
* **And** the system responds with HTTP status `409 Conflict` and an error message indicating that the cargo number already exists.

**Scenario 4: Creation rejected when mandatory fields are missing (Validation error)**
* **Given** an authenticated user with permission to create cargos
* **When** a `POST` request is sent to `/api/cargos` missing any required field (`codigo`, `fechaCreacion`, `origenId`, `caracterId`, `categoriaId`, `dedicacionId`, `unidadId`)
* **Then** the cargo is not created
* **And** the system responds with HTTP status `400 Bad Request` with field validation errors.

**Scenario 5: Creation rejected when referenced catalogue entity does not exist**
* **Given** an authenticated user
* **When** a `POST` request is sent referencing a non-existent `origenId`, `caracterId`, `categoriaId`, `dedicacionId`, or `unidadId`
* **Then** the system responds with HTTP status `404 Not Found` or `400 Bad Request` indicating the invalid foreign reference.

**Scenario 6: Creation rejected when specified predecessor does not exist**
* **Given** an authenticated user
* **When** a `POST` request is sent specifying a `predecesorCodigo` that does not exist in the database
* **Then** the system does not create the cargo
* **And** responds with HTTP status `404 Not Found` indicating the predecessor cargo was not found.

**Scenario 7: Unit Administrator restricted to their assigned unit**
* **Given** the authenticated user has the role `ADMIN_UNIDAD` assigned to unit ID `10`
* **When** the user sends a `POST` request to `/api/cargos` specifying `unidadId` = `20`
* **Then** the cargo is not created
* **And** the system responds with HTTP status `403 Forbidden` indicating unauthorized unit access.

---

# Test Plan

#### Context

Ensure test database has seeded data for `Origen`, `Caracter`, `Categoria`, `Dedicacion`, and `Unidad`. Authenticate with JWT tokens corresponding to `ADMIN_GLOBAL` and `ADMIN_UNIDAD`.

#### Scenarios

| # | Scenario | Role | Payload Details | Expected Status |
| - | -------- | ---- | --------------- | --------------- |
| 1 | Success (Stand-alone cargo) | `ADMIN_GLOBAL` | Valid fields, without predecessor | `201 Created` |
| 2 | Success (With predecessor) | `ADMIN_GLOBAL` | Valid fields, valid `predecesorCodigo` | `201 Created` |
| 3 | Success (Admin Unidad in own unit) | `ADMIN_UNIDAD` | Valid fields matching user's unit | `201 Created` |
| 4 | Conflict (Duplicate cargo code) | `ADMIN_GLOBAL` | Existing `codigo` | `409 Conflict` |
| 5 | Validation (Missing fields) | `ADMIN_GLOBAL` | Missing `codigo` or `categoriaId` | `400 Bad Request` |
| 6 | Error (Invalid predecessor) | `ADMIN_GLOBAL` | Non-existent `predecesorCodigo` | `404 Not Found` |
| 7 | Error (Non-existent foreign key) | `ADMIN_GLOBAL` | Non-existent `origenId` | `404 Not Found` |
| 8 | Forbidden (Admin Unidad cross-unit) | `ADMIN_UNIDAD` | `unidadId` different from user's unit | `403 Forbidden` |

---

# Post-implementation Validation

1. `mvn test -Dtest=CargoControllerTest,CargoServiceTest`
2. `mvn clean install`
3. Execute POST request against running server via Postman or integration test and verify record in `cargos` table with `cargo_estado = 'Libre'` and `cargo_horas_ocupadas = 0`.

---

# Non-functional notes

- **Data Integrity**: Concurrency control and unique constraint on `cargo_codigo` in the database to prevent duplicate codes during simultaneous requests.
- **Auditing & Traceability**: Creation date must be stored as specified in payload, and transaction must be `@Transactional` to ensure rollback on failures.
- **Authorization Scoping**: Method security `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")` on the controller endpoint.
