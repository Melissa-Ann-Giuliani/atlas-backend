# Open Questions Document — RF-06

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Columns to display in the list

**Context:** The raw requirement states "mostrar un listado de todos los cargos activos" but doesn't specify the data points/columns to show in the table. Without this, the DTOs cannot be modeled properly.

**Options:**

- [ ] **A)** Show: Cargo Name, Teacher Name, Unit Name, Start Date.
- [ ] **B)** Show: Cargo Name, Teacher Name, Dedication Type.
- [ ] **C)** Replicate the columns from the legacy system's active cargo report.

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Available filters

**Context:** The requirement says "Debe ser capaz de aplicar filtros sobre la información en la tabla" but does not specify which fields can be filtered. Without knowing this, we cannot build the dynamic query or UI inputs.

**Options:**

- [ ] **A)** Filter by: Teacher Name, Unit (if Global Admin), Cargo Type.
- [ ] **B)** Text-based search across all columns.
- [ ] **C)** Filter by: Teacher Name only.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Auditor unit restriction

**Context:** The requirement says "Un auditor no puede modificar información", but does not specify if they are restricted to a specific Unit or if they have global read access.

**Options:**

- [ ] **A)** Auditors have global read access (they can see all units).
- [ ] **B)** Auditors belong to a specific unit and can only see that unit's data.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: What is "más información"?

**Context:** The requirement says "con la posibilidad de hacer clic sobre un cargo para acceder a más información". This implies a detail view or modal, but its contents and whether it is part of this task or a separate one is unclear.

**Options:**

- [ ] **A)** Treat the detail view as out-of-scope for RF-06 (create a new RF for the detail view).
- [ ] **B)** The detail view is in scope and should simply show all fields of the Cargo entity.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec  | Impact  | Short description                  | Decision |
|----|-------|---------|------------------------------------|----------|
| B1 | 01    | Blocker | Columns to display in the list     | Based on mockup: Número, FechaCreación, Estado, Apellido Nombre, Categoría, Dedicación, Caracter |
| B2 | 01    | Blocker | Available filters                  | Pending, assuming standard filters for now |
| R1 | 01    | Risk    | Auditor unit restriction           | Pending clarification |
| R2 | 01    | Risk    | What is "más información"?         | Detail view screen with admin data, activities, designacion, etc. |

---
