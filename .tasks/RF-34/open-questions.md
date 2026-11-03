# Open Questions Document — RF-34

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Scope of Modifiable Fields for Auditor

**Context:** The raw requirement states:
*"Selecciona sector de modificación (datos personales, datos de contacto)"*
*"Ingresa nuevo valor"*
In the domain model:
- `Auditor` extends `Usuario`: `nombre`, `apellido` (personal), `correo`, `username` (contact/account).
- `Auditor` also defines institutional position: `cargoNombre` (`CargoAuditor`: `DECANO`, `VICEDECANO`, `SECRETARIA_ACADEMICA`). Note that `Auditor` does not have an `Unidad` assigned (audits broadly/institutionally).
Should updating an Auditor allow updating `cargoNombre` in addition to personal (`nombre`, `apellido`) and contact (`correo`, `username`) data?

**Options:**

- [x] **A)** (Recommended) Include `cargoNombre` (`CargoAuditor`) alongside `nombre`, `apellido`, `correo`, and `username` in `AuditorUpdateRequest`, consistent with the update patterns for `AdminGlobal` (RF-32), `AdminUnidad` (RF-33), and `Docente` (RF-31).
- [ ] **B)** Restrict strictly to `nombre`, `apellido`, `correo`, and `username`, leaving `cargoNombre` immutable.

**Notes:**

---

### B2 — spec-01: Mutability of Username and Uniqueness Enforcement

**Context:** When modifying an Auditor, should `username` and `correo` be editable, and how should uniqueness conflicts be handled?

**Options:**

- [x] **A)** (Recommended) Allow updating `username` and `correo`, enforcing strict uniqueness checks against other users in `usuarios` (excluding the current auditor ID: `id != currentId`), exactly as done in RF-32 and RF-33.
- [ ] **B)** Keep `username` immutable and only permit updating `nombre`, `apellido`, `correo`, and `cargoNombre`.

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Scope of Authorization and Role Security

**Context:** The requirement establishes: *"Actor: Administrador Global"*. Can only a Global Administrator modify an Auditor, or should an Auditor be able to modify their own profile?

**Options:**

- [x] **A)** (Recommended) Restrict the modification endpoint strictly to Global Administrators (`ADMIN_GLOBAL` / `ADMINISTRADOR_GLOBAL`) via `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL')")`, conforming to the explicit actor requirement in RF-34.
- [ ] **B)** Allow `AUDITOR` to self-modify contact/personal fields, but disallow changing `cargoNombre`.

**Notes:**

---

### R2 — spec-01: Audit Log Delta Recording Granularity

**Context:** The requirement specifies: *"se genera un registro en el historial de cambios, indicando valor anterior, valor nuevo, usuario responsable y fecha/hora del cambio"*. We use the existing `cambios` table via `CambioService.registrarCambio(tabla, campo, valorAnterior, valorNuevo, usuarioResponsable)`.

**Options:**

- [x] **A)** (Recommended) Register individual change records in `cambios` table with `tablaAfectada = "auditores"` for each field that actually changed (`username`, `nombre`, `apellido`, `correo`, `cargo_au_nombre`). Only genuine deltas (`valorAnterior != valorNuevo`) will be stored.
- [ ] **B)** Register a single consolidated snapshot record in `cambios`.

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Modifiable fields scope (include cargoNombre) | Option A: Include `cargoNombre` (`CargoAuditor`) alongside personal (`nombre`, `apellido`) and contact (`correo`, `username`) data. |
| B2 | 01 | Blocker | Username mutability & collision validation | Option A: Allow modifying `username` and `correo` with strict collision checks excluding current auditor ID. |
| R1 | 01 | Risk | Endpoint authorization scope | Option A: Restrict exclusively to Global Administrators (`ADMIN_GLOBAL` / `ADMINISTRADOR_GLOBAL`). |
| R2 | 01 | Risk | Change audit logging delta persistence | Option A: Record individual delta entries in `cambios` (`tablaAfectada = "auditores"`). |
