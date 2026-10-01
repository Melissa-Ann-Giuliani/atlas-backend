# User Story – Enriched Template

## File Template — `spec-02-role-based-access.md`

### `# User Story Description`

This slice implements the data visibility rules based on the actor's role. Unit Administrators should only see teachers from their respective departments, while Auditors should only have read access.

### `# Story (INVEST)`

**As** an Administrator (Global or Unit) or Auditor  
**I want** the system to filter the teacher list according to my role  
**So that** I only see the information I am authorized to access.

### `# Analysis`

#### Approach

Modify the endpoint created in slice 01 to read the authenticated user's role and associated unit ID. If the user is a Unit Admin, append a filter condition to the query ensuring they can see teachers assigned to their unit (even if those teachers also belong to other units). Ensure the UI disables any modification actions (if they exist) for the Auditor role.

### `# Acceptance Criteria`

**Scenario 1: Unit Administrator views the list**
* **Given** a Unit Administrator from "Department A" is logged in
* **When** they access the "Docentes" list
* **Then** the table only displays active teachers assigned to "Department A".

**Scenario 2: Auditor views the list**
* **Given** an Auditor is logged in
* **When** they access the "Docentes" list
* **Then** they can view the full list of active teachers but cannot perform any write or modification actions on the data.
