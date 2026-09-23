# RF-30 — specification index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Original requirements definition |
| `spec-01-register-teacher.md` | User story for registering a new teacher |
| `open-questions.md` | Open questions regarding business rules |
| `solution-diagrams.md` | Sequence and class diagrams |

## Execution order

| Order | Spec file | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | `spec-01-register-teacher.md` | Core logic to create a teacher user profile | None |

## Suggested increments / Backbone

1. **Walking Skeleton:** Create a minimal API endpoint to register a teacher with basic fields.
2. **MVP Narrative:** Include validations (DNI, email uniqueness) and handle "regular" category / seniority logic.
3. **Enhancements:** Trigger confirmation email with temporary password.
