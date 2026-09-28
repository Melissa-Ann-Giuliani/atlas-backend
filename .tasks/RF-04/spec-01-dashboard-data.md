# User Story Description
As an Administrator or Auditor, I need an API endpoint that provides aggregated data about the teaching staff across different academic units (Centros, Departamentos, Institutos) so that the frontend can render the teaching map dashboard.

# Story (INVEST)
**As** a Global Administrator, Unit Administrator, or Auditor
**I want** an endpoint to retrieve the count of teachers grouped by unit type and specific units
**So that** I can visualize the teaching map of the faculty on the home page.

# Analysis
#### Approach
Create a GET endpoint (e.g., `/api/inicio/mapa-docente`) that returns a DTO containing the total count of teachers, the breakdown by unit type (Centro, Departamento, Instituto), and the further breakdown within each unit type. The aggregation should be done efficiently at the database level.

# Context
Based on the `raw-requirement.md` and the frontend UI mockup.

# Acceptance Criteria
**Scenario 1: Successful data retrieval (Global Admin or Auditor)**
* **Given** there are teachers assigned to various units in the database
* **And** the user is authenticated with the role of Global Admin or Auditor
* **When** the user requests the teaching map data
* **Then** the system returns a 200 OK status
* **And** the payload contains the total number of teachers for the entire faculty
* **And** the payload contains the count of teachers grouped by all unit types (Centros, Departamentos, Institutos)
* **And** the payload contains the count of teachers grouped by each specific unit within those types.

**Scenario 2: Successful data retrieval (Unit Admin)**
* **Given** there are teachers assigned to various units in the database
* **And** the user is authenticated with the role of Unit Admin (e.g., assigned to a specific `unidad_id`)
* **When** the user requests the teaching map data
* **Then** the system returns a 200 OK status
* **And** the payload contains the total number of teachers belonging *only* to their specific unit (e.g., "Departamento" of "Música")
* **And** the unit type charts (e.g., "Centros" and "Institutos") will be empty or zero, and the "Departamentos" chart will only contain their specific unit ("Música")
* **And** the data for any other units is returned as empty or zero.

**Scenario 3: Unauthorized access**
* **Given** the user is not authenticated or lacks the required roles
* **When** the user requests the teaching map data
* **Then** the system returns a 401 or 403 status.

# Test Plan
#### Scenarios
1. Success: Valid user retrieves correct aggregations.
2. Error: Unauthenticated user is denied access.
3. Edge Case: Database has no teachers (should return 0s).
