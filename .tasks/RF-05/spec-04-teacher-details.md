# User Story – Enriched Template

## File Template — `spec-04-teacher-details.md`

### `# User Story Description`

This slice enables users to click on a specific teacher row in the list to navigate to a detailed profile view containing contact info, designation, role, and functions.

### `# Story (INVEST)`

**As** an authorized user  
**I want** to click on a teacher in the list  
**So that** I can view their complete profile, including contact details and current designations.

### `# Analysis`

#### Approach

Implement a new endpoint to fetch detailed teacher information by ID. Create a new frontend page route (e.g., `/docentes/:id`) for the profile. Add an onClick event or link to the table rows to trigger navigation to this page.

### `# Acceptance Criteria`

**Scenario 1: View teacher details**
* **Given** the user is viewing the teacher list
* **When** they click on a specific teacher's row
* **Then** the system navigates to the detailed profile view displaying the following sections:
  - **Datos Personales**: DNI, Email institucional, Teléfono.
  - **Designación y Cargo**: N° Legajo, N° Resolución, Fechas (inicio/fin), N° Cargo, Categoría, Dedicación, Caracter.
  - **Actividades Actuales**: Table with Materia/Proyecto, Origen, Horas, Estado.
  - **Licencias Actuales**.
  - **Documentación Adjunta**.
