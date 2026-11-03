# Open Questions Document — RF-33

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Scope of Modifiable Fields & Interpretation of "Funciones"

**Context:** The raw requirement specifies: *"Selecciona sector de modificación (datos personales, datos de contacto, cargo, funciones)"*. In the existing domain model, `AdminUnidad` has `nombre`, `apellido`, `username`, `correo`, `cargoNombre` (`CargoAdminUnidad`: `SECRETARIA_ACADEMICA`, `DEPARTAMENTO_DOCENTE`, `DIRECTOR_DEPARTAMENTO`), and `unidad` (`Unidad`). Unlike `Docente`, `AdminUnidad` does NOT possess academic teaching functions, dedicated hours, or research designations. 

**Options:**

- [ ] **A)** Map "cargo y funciones" to `cargoNombre` (`CargoAdminUnidad`) and academic `unidad` (`Unidad`). `AdminUnidad` does not have teaching functions/designations; personal data covers `nombre` and `apellido`, and contact data covers `correo` and `username`.
- [ ] **B)** Extend the domain model to add custom administrative responsibilities/functions table or field for `AdminUnidad`.

- [x] **Other:** Here i might need help, since sometimes, an Admin de Unidad can be a Docente at the same time, but i do not want that Usuario to have two different accounts. Just the same account. So maybe if you search user A with Admin de Unidad, it only shows their Admin de Unidad information. and if you search user A with Docente role, it shows that information. 

**Notes:**

---

### B2 — spec-01: Mutability of Username and Uniqueness Enforcement

**Context:** When modifying `AdminUnidad`, `username` and `correo` can be edited or might be locked. If edited, uniqueness must be enforced across all `usuarios` excluding the current user ID.

**Options:**

- [x] **A)** Allow updating `username` and `correo`, enforcing strict uniqueness checks against other users in `usuarios` (matching current backend behavior in `AdminUnidadService.modificarAdminUnidad` and identically to `AdminGlobal` in RF-32).
- [ ] **B)** Make `username` read-only/immutable (identity login key), only allowing changes to `nombre`, `apellido`, `correo`, `cargoNombre`, and `unidad`.

**Notes:** Applied identically to Administrador Global in RF-32: both `username` and `correo` can be modified, with strict validation ensuring neither is already taken by another user (`id != currentId`).

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Scope of Authorization and Self-Modification

**Context:** The requirement establishes: *"Actor: Administrador Global"*. Should a Global Admin be the sole actor permitted to modify an Admin de Unidad, or can an Admin de Unidad modify their own profile?

**Options:**

- [x] **A)** Restrict this management endpoint exclusively to Global Admins (`ADMIN_GLOBAL` / `ADMINISTRADOR_GLOBAL`) as mandated by the RF-33 requirement.
- [ ] **B)** Allow Admin de Unidad to modify their own profile via this endpoint, but restrict changing `cargoNombre` and `unidadId`.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Audit Log Delta Recording

**Context:** The requirement specifies: *"se genera un registro en el historial de cambios, indicando valor anterior, valor nuevo, usuario responsable y fecha/hora del cambio"*. We have the existing `cambios` table and `CambioService.registrarCambio(tabla, campo, valorAnterior, valorNuevo, usuarioResponsable)`.

**Options:**

- [x] **A)** Register individual change records in `cambios` table with `tablaAfectada = "admin_de_unidad"` for each field that actually changed (`username`, `nombre`, `apellido`, `correo`, `cargo_ad_nombre`, `unidad_id`). Only changes where `valorAnterior != valorNuevo` are saved.
- [ ] **B)** Register a single consolidated snapshot record in `cambios`.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Scope of modifiable fields & "funciones" vs Docente dual-profile | RESOLVED: Under current entity model (single role per Usuario via `rol_id` and joined inheritance), an Admin de Unidad profile modifies personal data (`nombre`, `apellido`), contact (`correo`, `username`), cargo (`cargoNombre`), and `unidadId`. Multi-role dual-profile support for a single account requires an architectural migration of `Usuario.rol` to many-to-many. For RF-33, when modifying an Admin de Unidad, the system operates specifically on their `AdminUnidad` record without affecting teaching data. |
| B2 | 01 | Blocker | Mutability of username & email uniqueness | RESOLVED: Applied identically to RF-32 (Admin Global): `username` and `correo` are mutable, enforcing uniqueness against other users excluding the current user ID. |
| R1 | 01 | Risk | Scope of authorization | RESOLVED: Endpoint restricted exclusively to Administrador Global as defined in RF-33. |
| R2 | 01 | Risk | Audit log recording granularity | RESOLVED: Register individual delta records per modified field in `cambios` via CambioService. |
