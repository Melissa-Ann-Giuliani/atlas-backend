# Open Questions Document — RF-01

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Missing User Persona and Job-To-Be-Done (JTBD)

**Context:** The raw requirement lists the actor simply as "Usuario". We need to define a specific persona and the concrete benefit (Why?) to apply Pattern 3.

**Options:**

- [ ] **A)** General Customer/End-user wanting to access their personal dashboard securely.
- [ ] **B)** Backoffice Employee needing to access the administration panel.

- [x] **Other:** Usuario is a parent class of 4 different roles/sub-classes: Administrador Global, Administrador de Unidad, Docente & Auditor

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Unhappy Flow - Incorrect Credentials

**Context:** We need to define the behavior when the user enters an invalid username or password (Pattern 12).

**Options:**

- [x] **A)** Show a generic "Invalid username or password" message without locking the account.
- [ ] **B)** Lock the account after N failed attempts.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Unhappy Flow - Inactive User

**Context:** The condition states the user must be "Active". What should happen if they are inactive?

**Options:**

- [x] **A)** Show "Account is inactive, please contact support."
- [ ] **B)** Show a generic error message for security.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec  | Impact  | Short description                  | Decision |
|----|-------|---------|------------------------------------|----------|
| B1 | 01    | Blocker | Missing User Persona and JTBD      | Other (4 Roles) |
| R1 | 01    | Risk    | Unhappy Flow - Incorrect Creds     | A (Generic Msg) |
| R2 | 01    | Risk    | Unhappy Flow - Inactive User       | A (Inactive Msg) |
