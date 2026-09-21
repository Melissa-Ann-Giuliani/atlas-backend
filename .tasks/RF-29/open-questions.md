# Open Questions Document — RF-29

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Definition of "Previously Registered"

**Context:** The requirement states the auditor must not be previously registered. It is ambiguous whether this uniqueness check should be on the username, the email, or both. If not defined, we might allow duplicate emails for different usernames, causing login or notification issues.

**Options:**

- [x] **A)** Uniqueness checked on BOTH username and email address.
- [ ] **B)** Uniqueness checked on username only.
- [ ] **C)** Uniqueness checked on email only (username is derived from email).

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Faculty Position Source

**Context:** The admin must choose a corresponding position within the faculty. It's unclear if these positions are a hardcoded enum/list or if they come from a dynamic database table of positions. If dynamic, we need an endpoint to fetch them.

**Options:**

- [ ] **A)** Positions are a fixed Enum (e.g., Lead Auditor, Junior Auditor).
- [ ] **B)** Positions are fetched from a dynamic database table.
- [ ] **C)** It's a free-text field.

- [x] **Other:** It would be Enum, but the values would be Director, Vicedirector, Secretaría Académica

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Password Generation Complexity

**Context:** The system generates a temporary password. There are no specified complexity rules (e.g., length, special characters), which might lead to insecure temporary passwords or clash with existing password policies.

**Options:**

- [ ] **A)** Alphanumeric, 8 characters.
- [ ] **B)** Alphanumeric + 1 special character, 12 characters.
- [ ] **C)** Use a standard library for secure random string generation with default strong settings.

- [x] **Other:** Use the same as Administrador Global and Administrador de Unidad.

**Notes:**

---

### R2 — spec-01: Email Delivery Failure Handling

**Context:** The system sends a confirmation email. If the SMTP server fails, it's unclear if the user registration should be rolled back or if the user is saved but without the email being sent (requiring a manual resend).

**Options:**

- [ ] **A)** Fail the registration entirely (rollback) if email fails.
- [ ] **B)** Save the user, but show a warning to the admin that the email failed.
- [ ] **C)** Save the user and queue the email for asynchronous retry.

- [x] **Other:** Use the same as Administrador Global and Administrador de Unidad.

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Password visibility to admin

**Context:** The requirement says the password "Sólo será visible para el usuario creado". But since it's generated on the server, we assume the admin NEVER sees it on the success screen, and it goes straight to the email.

**Options:**

- [x] **A)** Admin only sees a success message, password is ONLY sent in the email.
- [ ] **B)** Admin sees a one-time link or the password to copy-paste (contradicts email flow).

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec  | Impact  | Short description                  | Decision |
|----|-------|---------|------------------------------------|----------|
| B1 | 01    | Blocker | Uniqueness criteria                | Option A |
| B2 | 01    | Blocker | Faculty position source            | Enum (Director, Vicedirector, Secretaría Académica) |
| R1 | 01    | Risk    | Password generation complexity     | Same as Global/Unidad Admin |
| R2 | 01    | Risk    | Email failure handling             | Same as Global/Unidad Admin |
| M1 | 01    | Minor   | Password visibility to admin       | Option A |

---

## Authoring rules

- **ID convention:** `B<n>` = blocker, `R<n>` = risk, `M<n>` = minor inconsistency.
- **Always include `Other:`** as an open field — never force a choice between inadequate options.
- **Max 4 predefined options** per item; if more exist, group the least likely ones.
- **Context must state the consequence** of not resolving, not just describe the problem.
- **Options must be mutually exclusive and actionable** — avoid vague options like "ask the TL" as the only choice.
- **Update the spec and summary table**: Once the user checks an option with `[x]`, the agent will automatically update the corresponding spec and fill in the decision in the Summary table.
