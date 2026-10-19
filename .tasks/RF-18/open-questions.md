# Open Questions Document — RF-18: Modificar los datos de un cargo

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Editable fields vs immutable fields

**Context:** The raw requirement states "Cambia los valores que desee" (Changes the values they want). In `Cargo`, some fields carry structural or integrity implications (e.g., `codigo`, `unidadId`, `estado`, `horasOcupadas`, `cargoOrigenDivision`). We need to establish which fields can actually be edited via this endpoint.

**Options:**

- [x] **A)** Edit core classification attributes only: `fechaCreacion`, `origenId`, `caracterId`, `categoriaId`, `dedicacionId`, and optional `predecesorCodigo`. `codigo`, `unidadId`, `estado`, and `horasOcupadas` remain immutable through this update endpoint.
- [ ] **B)** Allow editing `codigo` and classification attributes (`fechaCreacion`, `origenId`, `caracterId`, `categoriaId`, `dedicacionId`, `predecesorCodigo`), validating that the new `codigo` does not collide with another existing cargo.
- [ ] **C)** Allow editing all fields including `unidadId` (subject to admin authorization) and `codigo`.

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Identifier in the Update Endpoint URL and DTO

**Context:** The system already provides `GET /api/cargos/{id}` using the internal primary key `id` (`Integer`), but users/UI often search cargos by public `codigo` (`Long`). We must decide the path and resource identifier for the update operation.

**Options:**

- [x] **A)** `PUT /api/cargos/{id}` using internal primary key `id` (`Integer`), consistent with REST conventions and existing `GET /api/cargos/{id}`.
- [ ] **B)** `PUT /api/cargos/{codigo}` using public position code `codigo` (`Long`).
- [ ] **C)** Support `PUT /api/cargos/{id}` while accepting the public `codigo` inside the request body (or query) for verification.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Modifying a cargo with active designations or assigned hours

**Context:** If a cargo already has `estado != "Libre"` (e.g. "Ocupado", active `designacion`, or `horasOcupadas > 0`), modifying its `dedicacion` might conflict with the hours already occupied or assigned.

**Options:**

- [x] **A)** Restrict modification of `dedicacion` if `horasOcupadas > 0` or if the new dedication hours are lower than the currently occupied hours. Allow modifying other metadata fields regardless of state.
- [ ] **B)** Allow modification only when `estado` is `"Libre"` (and 0 hours occupied); reject modification if cargo is already assigned/occupied.
- [ ] **C)** Allow modifying all allowed fields without state restrictions (relying on business/admin discretion).

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Unit scoping policy for `ADMIN_UNIDAD`

**Context:** Following RF-17 policy, an `ADMIN_UNIDAD` only has authority within their assigned unit. We must confirm whether an `ADMIN_UNIDAD` can only modify cargos that belong to their own unit.

**Options:**

- [x] **A)** Strict unit boundary: An `ADMIN_UNIDAD` can only update cargos belonging to their assigned unit; attempts to edit a cargo from another unit return HTTP 403 Forbidden. `ADMIN_GLOBAL` can edit any cargo.
- [ ] **B)** Unrestricted: Any administrator (`ADMIN_GLOBAL` or `ADMIN_UNIDAD`) can edit any cargo regardless of unit.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Predecessor modification / decoupling

**Context:** In RF-17, `predecesorCodigo` can be specified. Can an edit operation update `predecesorCodigo` to link a different predecessor or set it to `null` to unlink a predecessor?

**Options:**

- [ ] **A)** Yes, allow updating predecessor: passing a valid code links it, passing `null`/omitted unlinks the predecessor.
- [x] **B)** Allow updating predecessor only to another valid cargo, cannot remove once set.
- [ ] **C)** Predecessor is immutable once set during creation (cannot be modified via update).

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec  | Impact  | Short description                  | Decision |
|----|-------|---------|------------------------------------|----------|
| B1 | 01    | Blocker | Editable vs immutable cargo fields | Option A: Edit core classification attributes only (`fechaCreacion`, `origenId`, `caracterId`, `categoriaId`, `dedicacionId`, optional `predecesorCodigo`). `codigo`, `unidadId`, `estado`, and `horasOcupadas` are immutable. |
| B2 | 01    | Blocker | Resource identifier in PUT endpoint (id vs codigo) | Option A: `PUT /api/cargos/{id}` using internal primary key `id` (`Integer`), matching REST conventions and `GET /api/cargos/{id}`. |
| R1 | 01    | Risk    | Modification rules when cargo is occupied/assigned | Option A: Restrict modification of `dedicacion` if `horasOcupadas > 0` or if the new dedication hours are lower than the currently occupied hours. Allow modifying other metadata fields regardless of state. |
| R2 | 01    | Risk    | Admin de Unidad unit scoping policy on update | Option A: Strict unit boundary: `ADMIN_UNIDAD` can only update cargos from their assigned unit (HTTP 403 Forbidden otherwise). `ADMIN_GLOBAL` can update any cargo. |
| M1 | 01    | Minor   | Predecessor linking / unlinking during update | Option B: Allow updating predecessor only to another valid cargo; cannot remove predecessor once set (if existing predecessor is present and null/omitted in payload, previous predecessor is preserved). |
