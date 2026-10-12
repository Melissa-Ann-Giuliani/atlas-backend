# RF-07 — Specification Index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Initial raw requirement definition |
| `open-questions.md` | Log of business decisions and missing details from the raw requirement |
| `solution-diagrams.md` | Sequence and Class diagrams |
| `spec-01-list-active-licencias.md` | Specification for the base listing functionality and authorization |
| `spec-02-licencia-detail.md` | Specification for the detailed view of a license ("más información") |

## Execution order

| Order | Spec File | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | `spec-01-list-active-licencias.md` | Base listing of active licenses with unit-based authorization and filters | None |
| 2 | `spec-02-licencia-detail.md` | Detailed view of a license (teacher details, designation, linked cargo, documentation) | `spec-01` |

## Suggested increments / Backbone

1. **MVP (Walking Skeleton)**: Listing of active licenses with basic columns, paginated endpoint, and Unit-based filtering (Admin de Unidad restricted to their unit).
2. **Enhancement 1**: Advanced filtering (teacher name/DNI, date range, motive, unit selection for Global Admin).
3. **Enhancement 2**: Click-through to detailed view (`spec-02`), displaying full license relationships and documentation.
