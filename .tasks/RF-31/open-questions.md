# Open Questions Document — RF-31

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Scope of modification ("cargo" and "funciones")

**Context:** The raw requirement states: *"Selecciona sector de modificación (datos personales, datos de contacto, cargo, funciones)"*. In the current system, `Cargo` (assignments, designations, dedicated hours, category) and `Funcion` (teaching functions linked to subjects/materia) have independent lifecycle management, dedicated tables, and specific workflows/endpoints. If we bundle full Cargo and Function mutations into a single teacher update endpoint, it creates high architectural coupling and contradicts existing domain boundaries.

**Options:**

- [x] **A)** Separate concerns: RF-31 backend endpoint exclusively updates teacher profile data (Personal Data: nombre, apellido, DNI, CUIL, fechaIngreso, antigPrevia; and Contact Data: domicilio, teléfonos, email institucional). Modifications to Cargo/Funciones are delegated to their specific dedicated endpoints/RFs.
- [ ] **B)** All-in-one endpoint: Include endpoints or sub-endpoints under Docente to also reassign or edit existing Cargo and Funciones directly within RF-31.

- [ ] **Other:** ______

**Notes:**
Modifications to cargos and funciones related to the Docente, should be all managed in this RF, but can be in different endpoints.
---

### B2 — spec-01: Change History / Audit Log Mechanism

**Context:** The requirement specifies: *"se genera un registro en el historial de cambios, indicando valor anterior, valor nuevo, usuario responsable y fecha/hora del cambio"*. Currently, there is no generic teacher change history/audit log table in the database. Without defining the model, we cannot persist or query historical modifications.

**Options:**

- [ ] **A)** Create a dedicated audit entity/table `HistorialModificacionDocente` (storing `id`, `docente_id`, `campo_modificado`, `valor_anterior`, `valor_nuevo`, `usuario_responsable_id`, `fecha_hora`).
- [ ] **B)** Create a generic system audit entity `HistorialCambios` (storing `entidad`, `entidad_id`, `campo`, `valor_anterior`, `valor_nuevo`, `usuario_responsable_id`, `fecha_hora`) reusable across the entire platform.
- [ ] **C)** JSON-based delta audit: Store a single record per modification transaction containing a JSON payload with all modified fields (`{ "campo": { "anterior": "...", "nuevo": "..." } }`), timestamp, and responsible user.

- [x] **Other:** There is a tables called cambios in the database, that is which should be used to save any changes made within the system. 

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Unit Administrator Scope Validation

**Context:** The requirement says *"El Administrador de Unidad sólo puede modificar información de docentes de su propia unidad"*. A teacher can be associated with an academic unit through an active `Cargo` / `Designacion`. If a teacher has no active designation or belongs to a different unit, unauthorized modifications could occur if not properly restricted.

**Options:**

- [x] **A)** Verify that the teacher has at least one active `Designacion`/`Cargo` belonging to the `AdminUnidad`'s `Unidad`. If not, return HTTP 403 Forbidden.
- [ ] **B)** Same as option A, but if the teacher has no assigned unit at all, allow Unit Admins to claim/edit or restrict it only to Global Admins.

- [ ] **Other:** ______

**Notes:**
The teachers not linked to the unit of an Admin de Unidad CANNOT BE SEEN by the admin de unidad. 

---

### R2 — spec-01: Uniqueness of Modifiable Identification Fields

**Context:** When updating DNI, CUIL, or institutional email, if the user changes these values, they must remain unique across the system, excluding the current teacher being modified. If left unhandled, duplicate key database exceptions or identity collisions could occur.

**Options:**

- [ ] **A)** Allow editing DNI, CUIL, and Email with strict uniqueness validation excluding the current record (`id != docente.getId()`).
- [x] **B)** Make DNI and CUIL immutable (read-only after creation), only allowing modification of names, email, address, phones, etc.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01, 02, 03 | Blocker | Scope of modification for cargo/funciones | RESOLVED: All managed under RF-31 in separate focused endpoints (spec-01 for profile/contact, spec-02 for cargo/designacion, spec-03 for funciones). |
| B2 | 01 | Blocker | Architecture and table for change history / audit | RESOLVED: Use existing `cambios` table (`Cambio` entity / `CambioService`) to record all field changes. |
| R1 | 01 | Risk | Criteria to enforce Unit Admin ownership over a teacher | RESOLVED: Enforce that Unit Admin can only view and modify teachers linked to their unit via active designation/cargo. Others return 403 / are not visible. |
| R2 | 01 | Risk | Mutability and uniqueness of identification fields (DNI/CUIL) | RESOLVED: DNI and CUIL are immutable after registration; only modifiable fields (names, email, address, phones, etc.) can be updated. |

