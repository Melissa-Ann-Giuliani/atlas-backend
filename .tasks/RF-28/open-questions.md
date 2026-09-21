# Open Questions Document — RF-28

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Password policy for temporary password

**Context:** The requirement states a password is automatically generated, but it doesn't specify the complexity requirements for this temporary password. A weak password is a security risk, while overly complex ones might cause issues for initial login.

**Options:**

- [ ] **A)** Use an 8-character alphanumeric random string.
- [x] **B)** Use a 12-character strong password (letters, numbers, symbols).
- [ ] **C)** Generate a secure URL token link (magic link) instead of sending the password in plain text via email (more secure).

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Asynchronous Email Sending

**Context:** If the email server is down or slow, and email sending is synchronous, the registration request might timeout or fail, leaving the system in an inconsistent state (user created but no email sent).

**Options:**

- [x] **A)** Use a background job/queue (e.g., BullMQ, RabbitMQ) for sending the email.
- [ ] **B)** Send it synchronously for now, but handle the timeout and show a warning message on the UI.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec  | Impact  | Short description                  | Decision |
|----|-------|---------|------------------------------------|----------|
| B1 | 01    | Blocker | Password policy for temp password  | Option B |
| R1 | 01    | Risk    | Asynchronous Email Sending         | Option A |

---

## Authoring rules

- **ID convention:** `B<n>` = blocker, `R<n>` = risk, `M<n>` = minor inconsistency.
- **Always include `Other:`** as an open field — never force a choice between inadequate options.
- **Max 4 predefined options** per item; if more exist, group the least likely ones.
- **Context must state the consequence** of not resolving, not just describe the problem.
- **Options must be mutually exclusive and actionable** — avoid vague options like "ask the TL" as the only choice.
- **Update the spec and summary table**: Once the user checks an option with `[x]`, the agent will automatically update the corresponding spec and fill in the decision in the Summary table.
