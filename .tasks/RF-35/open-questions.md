# Open Questions Document — RF-35

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Definition and Scope of "Cargos Vigentes" Condition

**Context:** The requirement establishes: *"El usuario a dar de baja no puede tener cargos vigentes"*. In the domain model, only `Docente` has direct associations with academic teaching positions (`cargos` linked through `designaciones`). For other user roles (`ADMIN_GLOBAL`, `ADMIN_UNIDAD`, `AUDITOR`), users do not have teaching appointments (`Designacion`), but hold institutional roles/cargos (`CargoAdminGlobal`, `CargoAdminUnidad`, `CargoAuditor`). If a `Docente` has an active designation on a cargo, decommissioning the user leaves teaching positions in an inconsistent state.
How should the condition *"no puede tener cargos vigentes"* be validated across different user types?

**Options:**

- [x] **A)** (Recommended) For `Docente` users, query `Designacion` and `Cargo` to verify they have no designations in an active state (`designacion_estado_actual` not in `'Finalizada'`, `'Cancelada'`, `'Baja'`) with end dates not expired (`designacion_fecha_fin == null` or `>= today`). For administrative/auditor roles (`ADMIN_GLOBAL`, `ADMIN_UNIDAD`, `AUDITOR`), they have no `Designacion` records, so this condition is automatically satisfied.
- [ ] **B)** Strictly forbid decommissioning any user whose role is `ADMIN_GLOBAL` or `ADMIN_UNIDAD` if they are the sole active administrator of a unit or system, in addition to checking active `Designacion` for `Docente`.
- [ ] **C)** Prevent decommissioning if the user has *any* historical cargo assignment ever recorded, regardless of whether it is currently active or finalized.

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Method Placement and Service Architecture

**Context:** Unlike other domain modules that have dedicated services per entity (`DocenteService`, `AdminGlobalService`, etc.), `RF-35` applies across *any* registered user (`Usuario`). There is currently no `UsuarioService` (only `UsuarioController` and `UsuarioRepository`), while `DocenteService` handles docente operations.
Where should the `darDeBajaUsuario` logic be located?

**Options:**

- [x] **A)** (Recommended) Create a new `UsuarioService` in `com.atlas.atlas_backend.usuario` dedicated to cross-cutting user lifecycle management (including `darDeBajaUsuario`), keeping `UsuarioController` clean and adhering to Single Responsibility and existing service layer standards.
- [ ] **B)** Add `darDeBajaUsuario` directly into `DocenteService` and handle non-docente users there.
- [ ] **C)** Implement the logic directly inside `UsuarioController` without a service layer.

- [ ] **Other:** ______

**Notes:**

---

### B3 — spec-01: Updating Sub-entity State for `Docente`

**Context:** For a base `Usuario`, deactivation sets `usuario_activo = false`. However, `Docente` has its own distinct column `docente_estado` (values: `'Activo'`, `'Inactivo'`, `'Licencia'`). If only `usuario.activo` is updated to `false` without updating `docente.estado = 'Inactivo'`, search and listing queries that filter `docente_estado` (such as `DocenteRepository.findActiveCargos` or `DocenteService`) will still treat the docente as active.
What state changes should occur when decommissioning a `Docente`?

**Options:**

- [x] **A)** (Recommended) Set `usuario.activo = false` for the base `Usuario`, and if the entity is an instance of `Docente` (or present in `DocenteRepository`), additionally set `docente.estado = "Inactivo"`.
- [ ] **B)** Only update `usuario.activo = false`, leaving `docente.estado` unchanged.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Decommissioning Self (Logged-in Global Admin)

**Context:** The actor is `Administrador Global`. If a Global Admin is allowed to deactivate their own account, they would immediately lock themselves out and risk leaving the system with no active Global Administrators.
Should self-deactivation be explicitly blocked?

**Options:**

- [x] **A)** (Recommended) Forbid self-decommissioning: throw `400 Bad Request` if `targetUser.getUsername().equalsIgnoreCase(authenticatedUsername)`.
- [ ] **B)** Allow self-decommissioning without restriction.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Audit Log Registration

**Context:** Similar to previous user management operations (e.g., RF-32, RF-33, RF-34), changes and status transitions are audited in the `cambios` table.
Should decommissioning a user register an audit entry in `cambios`?

**Options:**

- [x] **A)** (Recommended) Register an audit record via `CambioService.registrarCambio("usuarios", "usuario_activo", "true", "false", usuarioResponsable)` (and for Docentes, `"docentes"`, `"docente_estado"`, `"Activo"`, `"Inactivo"`, `usuarioResponsable`).
- [ ] **B)** Do not record in `cambios` table, only log via application logger.

- [ ] **Other:** ______

**Notes:**
Remember that the user will have their estado changed to "Dado de baja" 

---

## Minor inconsistencies

---

### M1 — spec-01: HTTP Method and Route Endpoint

**Context:** The operation deactivates a user resource by changing its state to inactive (`usuario_activo = false`).
What should be the endpoint definition?

**Options:**

- [x] **A)** (Recommended) `PATCH /api/usuarios/{id}/baja` with response returning status `200 OK` and a summary confirmation payload.
- [ ] **B)** `DELETE /api/usuarios/{id}` (soft-delete returning `200 OK`).
- [ ] **C)** `PUT /api/usuarios/{id}/estado` with body `{ "activo": false }`.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Definition and scope of "cargos vigentes" validation | Option A: For Docente users, verify no active or non-expired Designacion/Cargo exists. Other roles have no teaching designaciones. |
| B2 | 01 | Blocker | Service layer architecture and placement of decommission logic | Option A: Create a new `UsuarioService` in `com.atlas.atlas_backend.usuario` dedicated to cross-cutting user lifecycle management. |
| B3 | 01 | Blocker | Synchronizing sub-entity state for Docente | Option A: Set `usuario.activo = false` and for Docente set `docente.estado = "Dado de baja"` / `"Inactivo"`. |
| R1 | 01 | Risk | Prevention of self-decommission by authenticated admin | Option A: Forbid self-decommissioning (400 Bad Request if target is current authenticated user). |
| R2 | 01 | Risk | Audit log recording in cambios table | Option A: Register audit records in `cambios` for `usuario_activo` (and `docente_estado` if Docente). User note: state changes to "Dado de baja". |
| M1 | 01 | Minor | Endpoint HTTP verb and URL path convention | Option A: `PATCH /api/usuarios/{id}/baja` returning status 200 OK. |
