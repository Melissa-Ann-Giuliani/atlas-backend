# User Story Description

As a user with administrative or audit roles, I need to view the detailed information of a specific cargo so that I can see its administrative data, linked designations, associated activities, predecessors/subcargos, and attached documentation.

---

# Story (INVEST)

**As** an Administrador Global, Administrador de Unidad, or Auditor  
**I want** to view the full details of a specific cargo  
**So that** I have a complete picture of its status, assignments, and history

---

# Analysis

#### Approach

- The endpoint will fetch the complete `Cargo` entity along with all its related entities (Teacher, Designation, Activities, Predecessor/Subcargos, Documentation).
- Authorization must be handled exactly as in the list view (Admin de Unidad can only view cargos from their unit).
- The UI will present this in a highly structured layout with multiple cards/sections.

---

# Acceptance Criteria

**Scenario 1: Viewing Administrative Data**
* **Given** the user is on the Cargo Detail page
* **Then** they should see the Cargo Number, Role (e.g., JTP), and Status in the header
* **And** a "Datos Administrativos" section displaying: Asignado A (Name, DNI), N° Resolución, Fecha de Creación, Dedicación, and Carácter.

**Scenario 2: Viewing Linked Designation**
* **Given** the user is on the Cargo Detail page
* **Then** they should see a "Designación Vinculada" section
* **And** it should display the N° Resolución and Período of the designation.

**Scenario 3: Viewing Linked Activities**
* **Given** the user is on the Cargo Detail page
* **Then** they should see an "Actividades Vinculadas" table
* **And** the table should list Materia/Proyecto, Origen (e.g., Planta, Investigación), and Horas.

**Scenario 4: Viewing Predecessors and Subcargos**
* **Given** the user is on the Cargo Detail page
* **Then** they should see a section showing the "Cargo Predecesor" (if any) and a list of "Subcargos Activos" (if any), maintaining the visual hierarchy.

**Scenario 5: Viewing Attached Documentation**
* **Given** the user is on the Cargo Detail page
* **Then** they should see a "Documentación Adjunta" section listing any files.

**Scenario 6: Edit button visibility**
* **Given** the user is an Auditor
* **Then** the "Editar Cargo" and "Agregar Documento" buttons should be hidden.
