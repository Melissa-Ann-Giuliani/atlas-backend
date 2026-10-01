# RF-05 — specification index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Initial raw feature requirements. |

## Execution order

| Order | Spec file | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | `spec-01-basic-list.md` | Basic data grid of active teachers (Global Admin). | |
| 2 | `spec-02-role-based-access.md` | Unit Admin restrictions and Auditor read-only access. | 1 |
| 3 | `spec-03-table-filters.md` | Custom table filtering functionality. | 1 |
| 4 | `spec-04-teacher-details.md` | Navigation and detailed profile view. | 1 |

## Suggested increments / Backbone

1. MVP Backbone (Walking Skeleton): Global Admin can see a simple list of active teachers.
2. Enhancements: Add authorization constraints based on user roles (Unit Admin, Auditor).
3. Enhancements: Add custom filtering to the data table columns.
4. Enhancements: Allow drill-down to a specific teacher's profile view.
