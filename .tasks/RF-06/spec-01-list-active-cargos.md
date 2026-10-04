# User Story Description

As a user with administrative or audit roles, I need to view a list of active cargos (status "Asignado") so that I can see what positions are currently occupied by teachers, restricted by my access level.

---

# Story (INVEST)

**As** an Administrador Global, Administrador de Unidad, or Auditor  
**I want** to view a list of active cargos  
**So that** I can track assigned positions in the faculty within my authorization scope

---

# Analysis

#### Approach

- The endpoint will fetch `Cargo` entities that have the status "Asignado" (which means they are linked to a teacher).
- Authorization must be handled:
  - **Administrador Global**: No unit restriction.
  - **Administrador de Unidad**: Restricted to the unit (departamento/instituto/centro) they belong to.
  - **Auditor**: Read-only access (no modification actions should be returned/allowed). Unit restriction for Auditor needs clarification (see `open-questions.md`).
- Must support filtering capabilities.
- The UI will present this in a table.

---

# Acceptance Criteria

**Scenario 1: Global Admin views active cargos**
* **Given** the user is logged in as an Administrador Global
* **When** they navigate to the "Cargos" list
* **Then** they see a paginated list of all active cargos across all units
* **And** the cargos are filtered by status "Asignado"

**Scenario 2: Unit Admin views active cargos restricted to their unit**
* **Given** the user is logged in as an Administrador de Unidad assigned to "Departamento de Computación"
* **When** they navigate to the "Cargos" list
* **Then** they see only active cargos belonging to "Departamento de Computación"

**Scenario 3: Auditor views active cargos**
* **Given** the user is logged in as an Auditor
* **When** they view the list of cargos
* **Then** they see the list of active cargos
* **And** they do not have access to any modification or deletion actions in the UI or backend

**Scenario 4: Filtering the list**
* **Given** the user is on the "Cargos" list
* **When** they apply filters
* **Then** the list is updated to reflect only the records matching the filter criteria

**Scenario 5: Viewing the correct columns**
* **Given** the user is viewing the "Cargos" list
* **Then** the table must display the following columns: Número, FechaCreación, Estado, Apellido/Nombre, Categoría, Dedicación, Caracter.

**Scenario 6: Accessing Cargo Detail**
* **Given** the user is viewing the "Cargos" list
* **When** they click on a specific row
* **Then** they are navigated to the Cargo Detail view ("más información") displaying the full administrative, activity, and designation details.

---

# Non-functional notes

- **Security**: The backend must strictly enforce unit-level authorization in the query itself (e.g. `WHERE unit_id = ?`) to prevent unauthorized access.
- **Performance**: Pagination should be implemented at the database level to handle large volumes of cargos.
