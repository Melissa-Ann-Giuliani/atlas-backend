# Open Questions Document — RF-32

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Scope of Modifiable Fields for Global Admin

**Context:** The raw requirement states: *"Selecciona sector de modificación (datos personales, datos de contacto, cargo, funciones)"*. However, in our system domain, `AdminGlobal` is a system user entity that only possesses `username`, `nombre`, `apellido`, `correo`, and `cargoNombre` (Enum `CargoAdminGlobal`: `DIRECCION_DE_PERSONAL`, `DESPACHO`). Global Admins do NOT have academic designations, dedicated hours, or teaching functions (those belong exclusively to `Docente`). Furthermore, `username` is typically immutable or used for login identity. If this scope is not clarified, we might design unnecessary tables or endpoints for functions that don't exist for Global Admins.

**Options:**

- [x] **A)** Adapt fields to `AdminGlobal` entity: Support Personal Data (`nombre`, `apellido`), Contact Data (`correo`), and Cargo (`cargoNombre` enum). Exclude "funciones", as Global Admins do not have teaching functions/hours.
- [ ] **B)** Extend `AdminGlobal` model: Add support for custom contact data (e.g. phones, addresses) and define a new concept of administrative functions/permissions table for Global Admins.

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Mutability of Username and Email Uniqueness

**Context:** When updating `AdminGlobal`, if `username` or `correo` are modified, we must prevent identity collisions and ensure uniqueness across the `usuarios` table. Alternatively, `username` could be made immutable (like DNI in Docente).

**Options:**

- [ ] **A)** Make `username` immutable (read-only); allow updating `nombre`, `apellido`, `correo` (with uniqueness check excluding current user id), and `cargoNombre`.
- [x] **B)** Allow updating `username` as well, enforcing uniqueness check on both `username` and `correo` excluding current user id.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Authorization and Self-Modification Prevention

**Context:** Global Admins modify other Global Admins. Should a Global Admin be allowed to modify their own profile, or should there be restrictions (e.g., cannot deactivate oneself or change one's own role/cargo to avoid locking out the system)?

**Options:**

- [x] **A)** Allow modifying any `AdminGlobal` including oneself, but prevent changing active status or role (only update personal data, email, and cargo).
- [ ] **B)** Restrict self-modification completely through this management endpoint (must use profile page instead).

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Change History Audit Log Granularity

**Context:** The requirement says: *"La información queda actualizada y se genera un registro en el historial de cambios, indicando valor anterior, valor nuevo, usuario responsable y fecha/hora del cambio"*. We have the existing `cambios` table (`CambioService.registrarCambio(tabla, campo, valorAnterior, valorNuevo, usuarioResponsable)`). 

**Options:**

- [x] **A)** Register one record per modified field in the `cambios` table with `tablaAfectada = "admin_globales"` (or `"usuarios"`), recording `campoAfectado`, `valorAnterior`, `valorNuevo`, `fechaHora`, and `usuario`.
- [ ] **B)** Store a combined snapshot representation.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Scope of modifiable fields for AdminGlobal | RESOLVED: Personal data (nombre, apellido), contact (correo), and cargo (cargoNombre enum). Exclude funciones. |
| B2 | 01 | Blocker | Mutability of username & email uniqueness | RESOLVED: Allow updating username and correo, enforcing strict uniqueness checks on both excluding current user ID. |
| R1 | 01 | Risk | Self-modification rules for AdminGlobal | RESOLVED: Allow modifying any AdminGlobal including self, restricted to personal data, username, email, and cargo (no role/status change). |
| R2 | 01 | Risk | Audit registration format in `cambios` | RESOLVED: Register one delta record per modified field in the `cambios` table via CambioService. |
