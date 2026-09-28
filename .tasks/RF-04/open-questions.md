# Open Questions Document — RF-04

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Unit Administrator Data Scope

**Context:** The requirement lists "Administrador de Unidad" as an actor. If a Unit Administrator requests the dashboard data, should they see the data for the *entire* faculty (all Centros, Departamentos, Institutos) or only the data pertaining to their specific unit?

**Options:**

- [ ] **A)** They see the data for the entire faculty (same as Global Admin/Auditor).
- [x] **B)** They only see the data for their specific unit (the charts for other units would be empty or hidden).

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec  | Impact  | Short description                  | Decision |
|----|-------|---------|------------------------------------|----------|
| B1 | 01    | Blocker | Scope of data for Unit Admin       | B        |
