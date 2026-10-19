# RF-18 — specification index

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Original requirement for updating position data (modificación de datos de un cargo) |
| `spec-01-modificar-cargo.md` | Formal specification in Gherkin for updating cargo fields, validations, and authorization |
| `open-questions.md` | Business and technical decisions (editable fields, identifier convention, occupied cargo rules, scoping) |
| `solution-diagrams.md` | Sequence, class, and supporting diagrams for the cargo update flow |

## Field mapping (contract ↔ Core)

| JSON/Contract Field | DTO | Domain | Description |
| ------------------- | --- | ------ | ----------- |
| `fechaCreacion` | `CargoUpdateRequest.fechaCreacion` | `Cargo.fechaCreacion` | Date when the position was created (`LocalDate`) |
| `origenId` | `CargoUpdateRequest.origenId` | `Cargo.origen.origenId` | ID of the funding/origin source (`Integer`) |
| `caracterId` | `CargoUpdateRequest.caracterId` | `Cargo.caracter.caracterId` | ID of the character/tenure type (`Integer`) |
| `categoriaId` | `CargoUpdateRequest.categoriaId` | `Cargo.categoria.categoriaId` | ID of the academic rank/category (`Integer`) |
| `dedicacionId` | `CargoUpdateRequest.dedicacionId` | `Cargo.dedicacion.dedicacionId` | ID of work commitment / dedication (`Integer`) |
| `predecesorCodigo` | `CargoUpdateRequest.predecesorCodigo` | `Cargo.cargoPredecesor` | Optional predecessor cargo code to link former position (`Long`) |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-modificar-cargo.md` | Cargo update endpoint, field validations, unit-level authorization checks, and predecessor management | None |

## Suggested increments / Backbone

1. **Walking Skeleton / MVP**:
   - Expose `PUT /api/cargos/{id}`.
   - Verify cargo existence (return 404 if not found).
   - Update classifications (`origen`, `caracter`, `categoria`, `dedicacion`, `fechaCreacion`).
   - Return updated `CargoDetailDTO`.
2. **Make it Robust & Releasable**:
   - Enforce unit scope validation: verify that an `ADMIN_UNIDAD` can only update cargos from their own unit.
   - Support updating or clearing `cargoPredecesor` via `predecesorCodigo`.
   - Validate catalog foreign keys (return 404 if referenced ID does not exist).
