# RF-31 — specification index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Original requirement definition |
| `open-questions.md` | Decision log on architectural boundaries and business rules |
| `spec-01-modify-teacher-profile.md` | Modifying personal data and contact details with audit history |
| `spec-02-modify-teacher-cargo-designacion.md` | Modifying/updating teacher cargo and designation details |
| `spec-03-modify-teacher-funciones.md` | Modifying/updating teacher assigned teaching functions |
| `solution-diagrams.md` | Sequence, class, and architectural diagrams |

## Execution order

| Order | Spec file | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | `spec-01-modify-teacher-profile.md` | Updating teacher personal & contact information, unit scoping, audit logging in `cambios` | None |
| 2 | `spec-02-modify-teacher-cargo-designacion.md` | Updating teacher cargo & designation attributes with audit in `cambios` | spec-01 |
| 3 | `spec-03-modify-teacher-funciones.md` | Updating teacher teaching functions (hours, subject) with audit in `cambios` | spec-01, spec-02 |

## Suggested increments / Backbone

1. **Walking Skeleton (Spec 01):** Profile modification endpoint `PUT /docentes/{id}/perfil` (or `PUT /docentes/{id}`), verifying Unit Admin permissions, immutable DNI/CUIL, uniqueness of email, and persisting field changes to `cambios`.
2. **Cargo & Designation Lifecycle (Spec 02):** Dedicated endpoint to modify designation/cargo attributes belonging to the teacher within the caller's unit, recording deltas in `cambios`.
3. **Teaching Functions (Spec 03):** Dedicated endpoint to update hours, subject or function metadata associated with the teacher's cargo, recording changes in `cambios`.
