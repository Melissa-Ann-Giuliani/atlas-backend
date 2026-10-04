# RF-06 — Specification Index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Initial raw requirement definition |
| `open-questions.md` | Log of business decisions and missing details from the raw requirement |
| `solution-diagrams.md` | Sequence and Class diagrams |
| `spec-01-list-active-cargos.md` | Specification for the base listing functionality |

## Execution order

| Order | Spec File | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | `spec-01-list-active-cargos.md` | Base listing of active cargos with unit-based authorization | None |
| 2 | `spec-02-cargo-detail.md` | Detailed view of a cargo including relationships (activities, designation) | `spec-01` |

## Suggested increments / Backbone

1. **MVP**: Listing of active ("Asignado") cargos with basic columns and Unit-based filtering (Admin de Unidad sees only their unit).
2. **Enhancement 1**: Advanced filtering on the table.
3. **Enhancement 2**: Click-through to detailed view (`spec-02`), displaying full cargo relationships.
