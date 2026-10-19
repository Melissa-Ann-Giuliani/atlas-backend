# spec-01-subdividir-cargo: Subdividir un cargo en 2 o más subcargos

## User Story Description

As an Academic Administrator (Global or Unit Admin), I need to subdivide an existing parent position (`Cargo`) into two or more sub-positions (`Subcargos`), so that academic tasks and teaching responsibilities can be distributed without exceeding the original allocated hours/dedication budget of the parent position while preserving historical linkage.

---

## Story (INVEST)

```
**As an** Academic Administrator (Global or Unit Admin)  
**I want to** divide an existing position into two or more sub-positions with defined categorizations and commitments  
**So that** multiple teaching roles can be assigned within the position's hour budget while retaining clear traceability back to the origin position
```

---

## Analysis

### Approach
1. **Vertical slice**: Expose a dedicated transactional endpoint `POST /api/cargos/subdividir` taking a request body with the parent cargo code/id and a list of subcargo definitions ($2 \le N \le 10$).
2. **Business Rules**:
   - **Parent Existence**: The parent cargo identified by `cargoOrigenCodigo` (or ID) must exist (404 Not Found if missing).
   - **Parent Eligible State (Decision R1)**: The parent cargo must currently be in state `"Libre"` or `"Licencia"`. If it is in `"Asignado"` or `"Ocupado"` without license, reject with HTTP 400 Bad Request / 409 Conflict.
   - **Authorization & Unit Scoping (Decision R2)**:
     - `ADMIN_GLOBAL` can subdivide any cargo and assign subcargos to any unit.
     - `ADMIN_UNIDAD` can only subdivide cargos belonging to their assigned academic unit, and all created subcargos must also belong to their assigned academic unit (403 Forbidden otherwise).
   - **Subcargo Count (Decision M1)**: The payload must contain between 2 and 10 subcargos inclusive ($2 \le \text{subcargos.size()} \le 10$).
   - **Hour Budget Validation (Decision B2)**: The total sum of hours of all generated subcargos cannot exceed the hours of the parent cargo (`sum(subcargo.dedicacion.horas) <= parent.dedicacion.horas`). It is not required to consume 100% of the parent hours.
   - **Unique Cargo Codes**: Each subcargo's `codigo` must be unique and cannot already exist in the database, nor be repeated within the payload (409 Conflict if duplicate).
   - **Parent Linkage & Predecessor (Decision M2)**:
     - Each created subcargo automatically sets its `cargoOrigenDivision` to the parent cargo entity.
     - `cargoPredecesor` is optional and independent: it is set only if explicitly provided in the subcargo payload.
   - **Parent State Post-Division (Decision B1)**: The parent cargo keeps its current status (e.g. `"Licencia"` or `"Libre"`), but its `horasOcupadas` are set to 100% (equal to its dedication hours: `parent.setHorasOcupadas(parent.getDedicacion().getDedicacionHoras())`) to prevent direct assignment of further teachers. Newly created subcargos start with state `"Libre"` and `horasOcupadas = 0`.
   - **Academic Functions (Decision R3)**: The endpoint only creates and links positions in `cargos`. Academic functions and subjects are managed independently.
   - **Atomicity**: The entire operation (updating parent cargo + inserting all subcargos) executes within a single `@Transactional` boundary; any failure rolls back all changes.

---

## Starting Point

| Component | Detail |
| --------- | ------ |
| **Project** | `atlas-backend` |
| **Classes** | `com.atlas.atlas_backend.cargo.CargoController`<br>`com.atlas.atlas_backend.cargo.CargoService`<br>`com.atlas.atlas_backend.cargo.CargoRepository` |
| **New DTOs** | `SubdividirCargoRequest`, `SubcargoItemRequest`, `CargoSubdivisionResponseDTO` |

---

## Acceptance Criteria

### Scenario 1: Subdividing an existing cargo successfully (Happy Path)
* **Given** an existing parent cargo with code `8001` having dedication "Semiexclusiva" (20 hours) in unit "Matemática", in state "Licencia" with 0 or partial occupied hours
* **When** an authorized administrator submits a request to `POST /api/cargos/subdividir` with:
  * `cargoOrigenCodigo`: `8001`
  * `subcargos`:
    * Subcargo 1: code `8002`, dedication "Simple" (10 hours), unit "Matemática"
    * Subcargo 2: code `8003`, dedication "Simple" (10 hours), unit "Matemática"
* **Then** the system responds with HTTP 201 Created
* **And** 2 new cargos (`8002` and `8003`) are persisted in the database with state `"Libre"`, each with `cargoOrigenDivision` referencing parent cargo `8001`
* **And** the parent cargo `8001` retains state `"Licencia"` with `horasOcupadas = 20` (100% occupied)
* **And** the response returns the details of the created subcargos.

### Scenario 2: Error when total hours exceed parent cargo hours (Unhappy Path)
* **Given** an existing parent cargo with dedication "Semiexclusiva" (20 hours)
* **When** the administrator submits a request with two subcargos:
  * Subcargo 1: dedication "Semiexclusiva" (20 hours)
  * Subcargo 2: dedication "Simple" (10 hours)
* **Then** the total hours (30 hs) exceed the parent cargo (20 hs)
* **And** the system rejects the operation with HTTP 400 Bad Request
* **And** an error message indicates that the sum of subcargo hours exceeds the original position hours
* **And** no subcargos are saved and parent cargo remains unchanged.

### Scenario 3: Error when cargo count is out of bounds (< 2 or > 4)
* **Given** an existing parent cargo
* **When** the administrator sends a subdivision request containing 1 subcargo or more than 10 subcargos
* **Then** the system rejects the request with HTTP 400 Bad Request
* **And** indicates that the number of subcargos must be between 2 and 4.

### Scenario 4: Error when parent cargo is in an invalid state (e.g. Asignado without Licencia)
* **Given** an existing parent cargo with state `"Asignado"` (active assignment without license)
* **When** the administrator attempts to subdivide this cargo
* **Then** the system rejects the request with HTTP 400 Bad Request
* **And** indicates that only positions in state "Libre" or "Licencia" can be subdivided.

### Scenario 5: Error when a subcargo code already exists (Conflict)
* **Given** an existing parent cargo and an already registered position with code `5555`
* **When** the administrator attempts to subdivide specifying subcargo code `5555`
* **Then** the system aborts the transaction and returns HTTP 409 Conflict
* **And** indicates that position code `5555` already exists.

### Scenario 6: Access denied for Unit Admin outside their assigned unit
* **Given** an authenticated user with role `ADMIN_UNIDAD` assigned to unit "Física"
* **And** a parent cargo belonging to unit "Matemática"
* **When** the user attempts to subdivide this cargo
* **Then** the system rejects the request with HTTP 403 Forbidden.

### Scenario 7: Access denied for Unit Admin specifying a subcargo in another unit
* **Given** an authenticated user with role `ADMIN_UNIDAD` assigned to unit "Matemática"
* **And** a parent cargo belonging to unit "Matemática"
* **When** the user submits a subcargo with `unidadId` corresponding to "Física"
* **Then** the system rejects the request with HTTP 403 Forbidden.

### Scenario 8: Parent cargo not found
* **Given** an administrator attempts to subdivide a cargo with code `999999` which does not exist
* **When** the request is received at `POST /api/cargos/subdividir`
* **Then** the system responds with HTTP 404 Not Found.

---

## Test Plan

| ID | Type | Scenario | Expected Result |
| -- | ---- | -------- | --------------- |
| T1 | Unit / Integration | Successful division into 2 subcargos with equal hour sum | 201 Created, parent `horasOcupadas` maxed out, subcargos linked |
| T2 | Unit / Integration | Successful division into 3 subcargos with less hour sum | 201 Created, subcargos created with state "Libre" |
| T3 | Unit / Validation | Division with sum of hours > parent cargo | 400 Bad Request, transaction rolled back |
| T4 | Unit / Validation | Division with only 1 subcargo or > 10 subcargos | 400 Bad Request |
| T5 | Unit / Validation | Parent cargo in state "Asignado" | 400 Bad Request |
| T6 | Unit / Integrity | Collision on subcargo code (already taken) | 409 Conflict, rollback |
| T7 | Security / Auth | Admin de Unidad attempting division on another unit's cargo | 403 Forbidden |
| T8 | Security / Auth | Admin de Unidad creating a subcargo for another unit | 403 Forbidden |
| T9 | Unit / Integration | Non-existent parent cargo code | 404 Not Found |

---

## Non-Functional Notes

- **Transactional integrity**: The subdivision must execute under `@Transactional(rollbackFor = Exception.class)` ensuring either all subcargos are created and parent is updated, or none.
- **Traceability**: The foreign key `cargo_origen_division` in `cargos` ensures complete historical origin tracking, queried directly via `findByCargoOrigenDivisionId`.
- **Response performance**: Execution time should be $< 200\text{ ms}$ for standard batches ($N \le 10$).
