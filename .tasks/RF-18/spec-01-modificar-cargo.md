# User Story Description

The system must allow a Global Administrator or Unit Administrator to modify the attributes of an existing academic position ("cargo") in the institution. The administrator can update attributes such as creation date, source of funding (origen), character/tenure type (caracter), academic category, work commitment (dedicacion), and optionally update or decouple the predecessor cargo. This ensures that position details reflect up-to-date administrative records while maintaining referential and unit integrity.

---

# Story (INVEST)

**As a** Global Administrator or Unit Administrator  
**I want** to edit the attributes of an existing academic position (cargo)  
**So that** its institutional classification, category, character, and predecessor linkages remain accurate and up-to-date.

---

# Analysis

#### Approach

This slice introduces the backend API endpoint (`PUT /api/cargos/{id}`) to update an existing `Cargo` entity. The controller receives the internal cargo ID in the path and a `CargoUpdateRequest` payload in the request body. 
The service layer:
1. Validates that the cargo exists (HTTP 404 if not found).
2. Performs unit-level authorization checks: `ADMIN_GLOBAL` can edit any cargo, while `ADMIN_UNIDAD` can only edit cargos that belong to their assigned academic unit (HTTP 403 Forbidden otherwise).
3. Validates referenced catalog entities (`Origen`, `Caracter`, `Categoria`, `Dedicacion`), throwing HTTP 404 if any reference is missing.
4. Validates work commitment (`dedicacion`): If the cargo has `horasOcupadas > 0`, the new dedication hours (`dedicacion.dedicacionHoras`) cannot be less than the already assigned/occupied hours (`horasOcupadas`). If it is less, the update is rejected with HTTP 400 Bad Request.
5. Manages predecessor cargo (Option B): A predecessor can be updated only by providing another valid cargo code (`predecesorCodigo`). Once set, a predecessor cannot be removed (if `predecesorCodigo` is omitted or null in the payload, the existing predecessor remains unchanged).
6. Preserves invariant fields: `id`, `codigo`, `unidad`, `estado`, `horasOcupadas`, `cargoOrigenDivision`, and `designacion` are protected from arbitrary overwrite.
7. Persists the changes within a transaction and returns the updated `CargoDetailDTO`.

---

# Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` |
| **Classes** | `src/main/java/com/atlas/atlas_backend/cargo/CargoController.java`<br>`src/main/java/com/atlas/atlas_backend/cargo/CargoService.java`<br>`src/main/java/com/atlas/atlas_backend/cargo/CargoUpdateRequest.java` |
| **Method / mapping** | `PUT /api/cargos/{id}` (`CargoController.modificarCargo`)<br>`CargoService.modificarCargo` |

---

# Context

See `raw-requirement.md` for the original requirement and `open-questions.md` for design questions and decisions.

---

# Acceptance Criteria

**Scenario 1: Successful modification of a cargo by Global Administrator (Happy path)**
* **Given** the authenticated user has the role `ADMIN_GLOBAL`
* **And** an existing cargo exists with `id` = `10`
* **And** valid entities exist for `origenId` = `2`, `caracterId` = `2`, `categoriaId` = `3`, `dedicacionId` = `2`
* **When** a `PUT` request is sent to `/api/cargos/10` with:
  | Field | Value |
  | `fechaCreacion` | `"2026-10-19"` |
  | `origenId` | `2` |
  | `caracterId` | `2` |
  | `categoriaId` | `3` |
  | `dedicacionId` | `2` |
* **Then** the system updates the cargo in the database with the new values
* **And** leaves `codigo`, `unidad`, `estado`, and `horasOcupadas` intact
* **And** returns HTTP status `200 OK` with the updated cargo details.

**Scenario 2: Successful modification updating predecessor cargo**
* **Given** an existing cargo exists with `id` = `10` and `codigo` = `5001`
* **And** another cargo exists with `codigo` = `4001`
* **When** a `PUT` request is sent to `/api/cargos/10` with valid fields and `predecesorCodigo` = `4001`
* **Then** the cargo with `id` = `10` is updated to link `cargoPredecesor` to the cargo with `codigo` `4001`
* **And** the system responds with HTTP status `200 OK`.

**Scenario 3: Preserving existing predecessor when omitted in payload**
* **Given** an existing cargo with `id` = `10` has a linked predecessor
* **When** a `PUT` request is sent to `/api/cargos/10` with `predecesorCodigo` omitted or null
* **Then** the existing predecessor remains linked to the cargo (predecessor is not removed)
* **And** the system responds with HTTP status `200 OK`.

**Scenario 4: Modification rejected when new dedication hours are lower than occupied hours (Risk R1)**
* **Given** an existing cargo with `id` = `10` has `horasOcupadas` = `20`
* **And** dedication `3` specifies only `10` hours
* **When** a `PUT` request is sent to `/api/cargos/10` with `dedicacionId` = `3`
* **Then** the cargo is not updated
* **And** the system responds with HTTP status `400 Bad Request` explaining that dedication hours cannot be lower than currently occupied hours.

**Scenario 5: Modification rejected when cargo does not exist (Unhappy path)**
* **Given** no cargo exists with `id` = `9999`
* **When** a `PUT` request is sent to `/api/cargos/9999` with valid payload
* **Then** the system responds with HTTP status `404 Not Found`.

**Scenario 6: Modification rejected when mandatory fields are missing (Validation error)**
* **Given** an authenticated user
* **When** a `PUT` request is sent to `/api/cargos/10` missing any mandatory field (`fechaCreacion`, `origenId`, `caracterId`, `categoriaId`, `dedicacionId`)
* **Then** the system responds with HTTP status `400 Bad Request`.

**Scenario 7: Modification rejected when referenced catalogue entity does not exist**
* **Given** an existing cargo with `id` = `10`
* **When** a `PUT` request is sent referencing a non-existent `origenId`, `caracterId`, `categoriaId`, or `dedicacionId`
* **Then** the system responds with HTTP status `404 Not Found` indicating the foreign entity was not found.

**Scenario 8: Modification rejected when specified predecessor does not exist or is self-referencing**
* **Given** an existing cargo with `id` = `10` and `codigo` = `5001`
* **When** a `PUT` request is sent with `predecesorCodigo` = `9999` (non-existent) or `predecesorCodigo` = `5001` (self-referencing)
* **Then** the system does not update the cargo
* **And** responds with HTTP status `404 Not Found` or `400 Bad Request`.

**Scenario 9: Unit Administrator restricted to their assigned unit**
* **Given** the authenticated user has the role `ADMIN_UNIDAD` assigned to unit ID `1`
* **And** cargo `10` belongs to unit ID `2`
* **When** the user sends a `PUT` request to `/api/cargos/10`
* **Then** the cargo is not modified
* **And** the system responds with HTTP status `403 Forbidden`.

---

# Test Plan

#### Context

Ensure the test database has seeded data for `Cargo`, `Origen`, `Caracter`, `Categoria`, `Dedicacion`, and `Unidad`. Authenticate with JWT tokens corresponding to `ADMIN_GLOBAL` and `ADMIN_UNIDAD`.

#### Scenarios

| # | Scenario | Role | Payload Details | Expected Status |
| - | -------- | ---- | --------------- | --------------- |
| 1 | Success (Update classifications) | `ADMIN_GLOBAL` | Valid fields, without predecessor | `200 OK` |
| 2 | Success (Update with new predecessor) | `ADMIN_GLOBAL` | Valid fields, valid `predecesorCodigo` | `200 OK` |
| 3 | Success (Preserve existing predecessor) | `ADMIN_GLOBAL` | Omit `predecesorCodigo` | `200 OK` |
| 4 | Success (Admin Unidad in own unit) | `ADMIN_UNIDAD` | Valid fields for cargo in user's unit | `200 OK` |
| 5 | Conflict/Bad Request (Dedicacion hours < occupied hours) | `ADMIN_GLOBAL` | Dedicacion hours < `horasOcupadas` | `400 Bad Request` |
| 6 | Not Found (Non-existent cargo) | `ADMIN_GLOBAL` | Non-existent cargo `id` = `9999` | `404 Not Found` |
| 7 | Validation (Missing required field) | `ADMIN_GLOBAL` | Missing `categoriaId` | `400 Bad Request` |
| 8 | Error (Invalid foreign catalog ID) | `ADMIN_GLOBAL` | Non-existent `origenId` | `404 Not Found` |
| 9 | Error (Invalid predecessor) | `ADMIN_GLOBAL` | Non-existent `predecesorCodigo` | `404 Not Found` |
| 10 | Forbidden (Admin Unidad cross-unit) | `ADMIN_UNIDAD` | Cargo belongs to another unit | `403 Forbidden` |

---

# Post-implementation Validation

1. `mvn test -Dtest=CargoControllerTest,CargoServiceTest`
2. `mvn clean install`
3. Execute PUT request against running backend and verify updated entity attributes in the database.

---

# Non-functional notes

- **Data Integrity**: Concurrency control and transactional safety (`@Transactional`) to guarantee atomicity during updates.
- **Authorization Scoping**: Method security `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")` on the update endpoint.
