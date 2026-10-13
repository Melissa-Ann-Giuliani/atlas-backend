# Open Questions Document — RF-07

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Technical definition of "Licencia Activa"

**Context:** The `licencias` table does not have an `estado` column. The requirement specifies that licenses must be "Activa" ("vinculados a un docente actualmente en licencia"). Without a precise query rule, the backend cannot accurately filter active records.

**Options:**

- [ ] **A)** Date range check: `CURRENT_DATE BETWEEN licencia_fecha_inicio AND COALESCE(licencia_fecha_fin, licencia_fecha_fin_estim)`
- [ ] **B)** Designation state check: `designacion.designacion_estado_actual = 'Licencia'`
- [x] **C)** Combined check: `designacion.designacion_estado_actual = 'Licencia'` AND `CURRENT_DATE BETWEEN licencia_fecha_inicio AND COALESCE(licencia_fecha_fin, licencia_fecha_fin_estim)`
- [ ] **D)** Add an explicit `licencia_estado VARCHAR(20) DEFAULT 'Activa'` column to the `licencias` table

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Columns to display in the list

**Context:** The raw requirement does not enumerate the specific columns to show in the licenses table. Without this, the `LicenciaListDTO` cannot be modeled.

**Options:**

- [ ] **A)** N° Resolución, Docente (Apellido y Nombre, DNI), Motivo, Fecha Inicio, Fecha Fin/Estimada, Unidad
- [ ] **B)** N° Resolución, Docente (Apellido y Nombre), Motivo, Fecha Inicio, Días
- [ ] **C)** N° Resolución, Docente, Cargo Vinculado, Motivo, Estado, Acciones

- [x] **Other:** N° Resolución, Motivo, Docente, Fecha Inicio, Fecha Fin, Días Usados, Estado

**Notes:**

---

### B3 — spec-01: Available filters

**Context:** The requirement specifies "Debe ser capaz de aplicar filtros sobre la información en la tabla" without detailing the filter parameters. Without knowing this, backend filter criteria cannot be implemented.

**Options:**

- [ ] **A)** Filter by: Teacher Name/DNI search, Motivo de Licencia, Date range (Desde / Hasta), and Unidad (for Global Admin)
- [ ] **B)** Filter by: Text search across all fields only
- [ ] **C)** Filter by: Motivo de Licencia only

- [x] **Other:** Start by accepting filters based on the columns, but it may accept new filters in the future

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01 / spec-02: Relationship path from Licencia to Unidad

**Context:** Unit Admins can only view records for their unit. In the DB, `licencias` links to `designaciones`, which links to `cargos` (where `unidad_id` resides). What if a `Designacion` is linked to multiple cargos or has no cargo assigned?

**Options:**

- [ ] **A)** Filter by joining `licencias -> designaciones -> cargos -> unidad` (cargo's unit defines the license unit)
- [ ] **B)** A teacher (Docente) belongs to a primary unit or user-level unit
- [x] **C)** Require that every designation with active license has at least one associated cargo in that unit

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-02: Scope of "Más Información" / Detail View

**Context:** The requirement mentions clicking on a license to view cargo, docente, designacion, etc. It needs to be clear whether this is an in-place modal, a dedicated detail page (like CargoDetalle), or a separate ticket.

**Options:**

- [x] **A)** Dedicated detail page (`/licencias/:id`) showing administrative data, docente data, designación, cargo vinculado, and attached documents
- [ ] **B)** Expandable row or slide-over drawer/modal within the listing page
- [ ] **C)** Out of scope for RF-07 (leave click-through for a separate RF)

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec  | Impact  | Short description                  | Decision |
|----|-------|---------|------------------------------------|----------|
| B1 | 01    | Blocker | Technical definition of "Licencia Activa" | Combined: designacion.designacion_estado_actual = 'Licencia' AND CURRENT_DATE BETWEEN licencia_fecha_inicio AND COALESCE(licencia_fecha_fin, licencia_fecha_fin_estim) |
| B2 | 01    | Blocker | Columns to display in the list     | N° Resolución, Motivo, Docente, Fecha Inicio, Fecha Fin, Días Usados, Estado |
| B3 | 01    | Blocker | Available filters                  | Filters matching columns (search docente/resolución, motivo, date range, unit), extensible for future filters |
| R1 | 01    | Risk    | Relationship path from Licencia to Unidad | Require designation with active license has associated cargo in unit (join via designacion -> cargo -> unidad) |
| R2 | 02    | Risk    | Scope of "Más Información" / Detail View | Dedicated detail page (/licencias/:id) |

---
