# RF-29 — specification index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Initial requirement description |
| `spec-01-register-auditor.md` | Core use case for registering an auditor |
| `solution-diagrams.md` | Sequence and Class diagrams |
| `open-questions.md` | Pending decisions on business logic and technical risks |

## Execution order

| Order | Spec file | Summary | Depends on |
|---|---|---|---|
| 1 | `spec-01-register-auditor.md` | Registering a new auditor and sending confirmation email | - |

## Suggested increments / Backbone

1. **Slice 1 (MVP)**: Implement the form to register the user, assign the Auditor role, generate password, and save to DB.
2. **Slice 2 (Enhancement)**: Email notification with temporary password and password reset flow on first login.
