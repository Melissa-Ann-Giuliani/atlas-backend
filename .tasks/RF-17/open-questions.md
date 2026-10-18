# Open Questions Document — RF-17: Alta de un cargo

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Identifier format for Predecessor Cargo (`número de predecesor`)

**Context:** The raw requirement states "Ingresa: ... número de predecesor (por si se vincula con otro cargo antiguo)". In the database, `Cargo` has `id` (`Integer`, PK) and `codigo` (`Long`, public number). We need to confirm whether the API accepts the public `codigo` or internal `id`.

**Options:**

- [x] **A)** Use `predecesorCodigo` (`Long`): The frontend sends the visible position number, and the backend resolves the entity via `cargoRepository.findByCodigo(...)`.
- [ ] **B)** Use `predecesorId` (`Integer`): The frontend selects from an autocomplete list and sends the internal database ID.
- [ ] **C)** Support both optionally in the DTO, prioritizing `codigo` if present.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Unit scoping policy for `ADMIN_UNIDAD`

**Context:** The raw requirement allows "Administrador Global o Administrador de Unidad" to create cargos. `AdminUnidad` has a fixed unit assigned (`unidad_id`). If an `ADMIN_UNIDAD` submits a cargo for a different unit, we must define the exact security behavior.

**Options:**

- [x] **A)** Strict rejection: If an `ADMIN_UNIDAD` sends a `unidadId` different from their assigned unit, return HTTP 403 Forbidden.
- [ ] **B)** Automatic override: For `ADMIN_UNIDAD`, ignore whatever `unidadId` was passed in the request body and force it to their assigned unit.
- [ ] **C)** Unrestricted: Allow `ADMIN_UNIDAD` to create cargos for any unit without restriction.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Predecessor status and unit validation

**Context:** If a predecessor is linked, should the system validate that the predecessor belongs to the same unit or has a specific state (e.g., must not be "Libre" or must be from the same unit)?

**Options:**

- [ ] **A)** Predecessor must belong to the same `unidad` as the new cargo; reject if different.
- [x] **B)** No unit restriction on predecessor (allows reorganization/transfers between units), but predecessor must exist.
- [ ] **C)** No validation beyond checking that the predecessor exists.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Predecessor identifier format (codigo vs id) | Option A: Use `predecesorCodigo` (`Long`) to resolve cargo entity |
| R1 | 01 | Risk | Admin de Unidad unit scoping policy | Option A: Strict rejection with HTTP 403 Forbidden if `unidadId` differs from assigned unit |
| M1 | 01 | Minor | Predecessor cross-unit validation rule | Option B: No unit restriction on predecessor; predecessor must exist (HTTP 404 if not found) |
