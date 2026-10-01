# Open Questions Document — RF-05

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-02: Multi-unit assignment visibility

**Context:** It is not specified what happens if an active teacher belongs to multiple departments/units. Does a Unit Administrator see them if they share at least one unit?

**Options:**

- [x] **A)** Yes, a Unit Admin can see any teacher assigned to their unit, even if the teacher also belongs to other units.
- [ ] **B)** No, the teacher must exclusively belong to the Unit Admin's unit to be visible.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Definition of "Active" Status

**Context:** The requirement states the teacher must have an "Active" status (currently assigned to a position). It's unclear if this is a derived status (e.g., checking active date ranges on designations) or a simple boolean flag on the user entity.

**Options:**

- [ ] **A)** It is a simple string/enum flag (`status == 'ACTIVE'`) on the Teacher/User entity.
- [x] **B)** It is a derived status, calculated by checking if the teacher has any current, non-expired designations.

- [ ] **Other:** 

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-04: Detail View Modality

**Context:** The spec doesn't clarify if the detail view should be a separate page route or a modal/drawer over the list.

**Options:**

- [x] **A)** Implement as a separate page route (e.g., `/docentes/:id`).
- [ ] **B)** Implement as a side drawer or modal on the same page.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec  | Impact  | Short description                  | Decision |
|----|-------|---------|------------------------------------|----------|
| B1 | 02    | Blocker | Multi-unit assignment visibility   | A        |
| R1 | 01    | Risk    | Definition of "Active" Status      | B        |
| M1 | 04    | Minor   | Detail View Modality               | A        |
