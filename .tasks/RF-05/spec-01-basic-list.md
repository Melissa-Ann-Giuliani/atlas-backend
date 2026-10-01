# User Story – Enriched Template

## File Template — `spec-01-basic-list.md`

### `# User Story Description`

This slice provides the fundamental capability to retrieve and display a list of all active teachers in the faculty. It establishes the base endpoint and UI table structure.

### `# Story (INVEST)`

**As** a Global Administrator  
**I want** to view a list of all active teachers  
**So that** I can have an overview of the current teaching staff in the system.

### `# Analysis`

#### Approach

Implement an endpoint that queries the database for all users with the "Teacher" role and an "Active" status. "Active" status is a derived condition, calculated by verifying that the teacher currently has at least one non-expired designation. The frontend will consume this endpoint and render a basic data table (e.g., paginated). Advanced filtering and role-based data restriction are out of scope for this slice.

### `# Acceptance Criteria`

**Scenario 1: Successful retrieval of active teachers (Happy Path)**
* **Given** there are teachers with an "Active" status in the system
* **When** the Global Administrator accesses the "Docentes" list
* **Then** the system displays a paginated table containing all active teachers with the following columns: Nombre, Origen, Unidad, Categoría, Dedicación, Caracter, and Estado.

**Scenario 2: No active teachers found**
* **Given** there are no active teachers in the system
* **When** the Global Administrator accesses the "Docentes" list
* **Then** the system displays an empty table and a message indicating "No active teachers found".

### `# Non-functional notes`

- Pagination should be implemented from the start to ensure performance with a large number of teachers.
