# User Story – Enriched Template

## File Template — `spec-03-table-filters.md`

### `# User Story Description`

This slice adds custom filtering capabilities to the data table, allowing users to search for specific teachers by name, ID, or other columns.

### `# Story (INVEST)`

**As** a system user with access to the teacher list  
**I want** to apply filters to the table columns  
**So that** I can quickly locate specific teachers within a large dataset.

### `# Analysis`

#### Approach

Add a general search input and an advanced filters button to the frontend table controls. The backend endpoint needs to be updated to accept dynamic query parameters (e.g., a general search query and specific column filters) and apply them to the database query.

### `# Acceptance Criteria`

**Scenario 1: Apply general text search**
* **Given** the teacher list is populated with data
* **When** the user types a value into the general search input
* **Then** the table updates to display only teachers whose data (e.g., Nombre, Unidad) matches the search term.

**Scenario 2: Apply advanced filters**
* **Given** the user clicks the "Filtros" button
* **When** the user selects specific values for columns like "Origen" or "Dedicación"
* **Then** the table updates to reflect the filtered dataset.
