# User Story Description

The system must allow Global Administrators and Unit Administrators to modify and update teacher profile information (personal and contact details). DNI and CUIL are strictly immutable after creation. The operation ensures that the teacher exists and that Unit Administrators can only modify teachers belonging to their academic unit (via active designations). Every modified field generates a record in the existing `cambios` table, storing previous value, new value, responsible user, and date.

---

### # Story (INVEST)

**As a** Global Administrator or Unit Administrator  
**I want** to modify the personal and contact details of an existing teacher  
**So that** teacher records remain accurate and all changes are strictly audited in the `cambios` log.

---

### # Analysis

#### Approach

- The modification is exposed via a REST endpoint `PUT /docentes/{id}`.
- The system verifies the authenticated user's role:
  - If `AdminGlobal`: full access to update any teacher.
  - If `AdminUnidad`: verifies that the target teacher is assigned to the unit of the administrator (via active designations/cargos in that unit). Otherwise, access is denied (HTTP 403 Forbidden). Teachers not belonging to the unit cannot be modified or viewed.
- Immutability and Validations:
  - `dni` and `cuil` are **immutable** and cannot be altered. If sent in the request, they must either match the existing values or be ignored/rejected.
  - `emailInstitucional`: if updated, must be unique across all users (`Usuario` and `Docente`), excluding the current teacher.
  - Modifiable fields: `nombre`, `apellido`, `fechaIngreso`, `antigPrevia`, `domicilio`, `telefonos`, `emailInstitucional`, `tipoDni`.
- Audit in `cambios` table:
  - For each field that changed value:
    - `cambio_tabla_afectada`: `"docentes"` or `"usuarios"`
    - `cambio_campo_afectado`: name of the modified field (e.g. `"emailInstitucional"`, `"domicilio"`, `"nombre"`)
    - `cambio_valor_anterior`: string representation of the previous value (or `"null"`)
    - `cambio_valor_nuevo`: string representation of the new value
    - `cambio_fecha_hora`: current date (`LocalDate.now()`)
    - `usuario_id`: authenticated user ID performing the update.

---

### # Starting Point

| | |
|---|---|
| **Project** | `atlas-backend` |
| **Classes** | `com.atlas.atlas_backend.usuario.DocenteController`, `com.atlas.atlas_backend.usuario.DocenteService`, `com.atlas.atlas_backend.cambio.CambioService`, `com.atlas.atlas_backend.cambio.Cambio` |
| **Method / mapping** | `PUT /docentes/{id}` |

---

### # Acceptance Criteria

**Scenario 1: Successful teacher profile modification by Global Administrator (Happy Path)**
* **Given** an existing teacher with ID `{id}` in the system
* **And** the authenticated user is a Global Administrator
* **When** they submit updated personal and contact details (`nombre`, `apellido`, `domicilio`, `telefonos`)
* **Then** the teacher's profile is updated in the database
* **And** for each changed field, a row is inserted in `cambios` with the old value, new value, responsible user ID, and current date
* **And** the system returns HTTP 200 with the updated `DocenteDetailsDTO`.

**Scenario 2: Successful teacher modification by Unit Administrator (Happy Path)**
* **Given** an existing teacher with an active designation in Unit A
* **And** the authenticated user is an Administrator of Unit A
* **When** they submit updated personal/contact details for this teacher
* **Then** the teacher's profile is updated
* **And** audit entries are recorded in `cambios`
* **And** the system returns HTTP 200 with the updated profile.

**Scenario 3: Unit Administrator attempts to modify teacher from another unit (Unhappy Path - Forbidden)**
* **Given** an existing teacher who has no active designation in Unit B
* **And** the authenticated user is an Administrator of Unit B
* **When** they attempt to modify this teacher
* **Then** the system rejects the request
* **And** returns HTTP 403 Forbidden with an error message indicating insufficient permissions.

**Scenario 4: Attempt to modify immutable DNI or CUIL (Unhappy Path)**
* **Given** an existing teacher with DNI `12345678`
* **When** the administrator attempts to change DNI to `87654321`
* **Then** the system rejects the change or preserves the original DNI/CUIL without modifying it, returning a 400 Bad Request error.

**Scenario 5: Conflict on updated institutional email (Unhappy Path)**
* **Given** another user in the system already has email `profesor@faculty.edu`
* **When** the administrator updates the current teacher's email to `profesor@faculty.edu`
* **Then** the system rejects the update
* **And** returns HTTP 400/409 Conflict indicating that the email is already in use.

**Scenario 6: Teacher not found (Unhappy Path)**
* **Given** no teacher exists with the specified ID
* **When** the administrator submits a modification request for that ID
* **Then** the system returns HTTP 404 Not Found.

---

### # Test Plan

#### Context

- Authenticated users with roles `AdminGlobal` and `AdminUnidad`.
- Existing seed data for Docente, Cargo, Designacion, and Unidad.

#### Scenarios

| # | Type | Description |
|---|---|---|
| 1 | Success | Global Admin modifies teacher personal/contact info; verify DB update and audit entries in `cambios`. |
| 2 | Success | Unit Admin modifies a teacher of their unit; verify DB update and audit rows in `cambios`. |
| 3 | Error | Unit Admin attempts to modify a teacher outside their unit; verify HTTP 403 Forbidden. |
| 4 | Error | Attempt to alter immutable DNI/CUIL; verify HTTP 400 rejection. |
| 5 | Error | Attempt to update institutional email to an already existing email; verify 400/409 error. |
| 6 | Error | Non-existent teacher ID; verify HTTP 404 Not Found. |

---

### # Post-implementation Validation

1. Run unit and integration tests: `.\mvnw test -Dtest=DocenteControllerTest,DocenteServiceTest`
2. Verify table `cambios` populated with corresponding audit records.

---

### # Non-functional notes

- **Atomic Audit:** The updates to `usuarios`/`docentes` and the inserts into `cambios` must occur within the same `@Transactional` boundary.
- **Security:** Unit isolation must be strictly enforced so that teachers belonging to other units cannot be modified.
