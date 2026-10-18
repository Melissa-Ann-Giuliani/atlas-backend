# RF-17 — specification index

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Original requirement for registering a new position (alta de un cargo) |
| `spec-01-alta-cargo.md` | Formal specification in Gherkin for creating the cargo and initial state validations |
| `open-questions.md` | Business and technical decisions (unit restrictions for Admin de Unidad, predecessor identifier, etc.) |
| `solution-diagrams.md` | Sequence, class, and entity relationship diagrams for the cargo creation flow |

## Field mapping (contract ↔ Core)

| JSON/Contract Field | DTO | Domain | Description |
| ------------------- | --- | ------ | ----------- |
| `codigo` | `CargoCreateRequest.codigo` | `Cargo.codigo` | Unique cargo identifier/number (`Long`) |
| `fechaCreacion` | `CargoCreateRequest.fechaCreacion` | `Cargo.fechaCreacion` | Date when the position is created (`LocalDate`) |
| `origenId` | `CargoCreateRequest.origenId` | `Cargo.origen.origenId` | ID of the funding/origin source (`Integer`) |
| `caracterId` | `CargoCreateRequest.caracterId` | `Cargo.caracter.caracterId` | ID of the character/tenure type (`Integer`) |
| `categoriaId` | `CargoCreateRequest.categoriaId` | `Cargo.categoria.categoriaId` | ID of the academic rank/category (`Integer`) |
| `dedicacionId` | `CargoCreateRequest.dedicacionId` | `Cargo.dedicacion.dedicacionId` | ID of work commitment / dedication (`Integer`) |
| `unidadId` | `CargoCreateRequest.unidadId` | `Cargo.unidad.unidadId` | ID of the organizational academic unit (`Integer`) |
| `predecesorCodigo` | `CargoCreateRequest.predecesorCodigo` | `Cargo.cargoPredecesor` | Optional predecessor cargo code to link former position (`Long`) |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-alta-cargo.md` | Position registration (Alta de cargo), initial default state, uniqueness checks, and authorization scoping | None |

## Suggested increments / Backbone

1. **Walking Skeleton / MVP**:
   - Receive payload in `POST /api/cargos`.
   - Validate mandatory fields and unique `codigo`.
   - Persist entity with default `estado = "Libre"`, `horasOcupadas = 0`, and `designacion = null`.
   - Return HTTP 201 Created with the saved cargo ID/details.
2. **Make it Robust & Releasable**:
   - Link predecessor cargo if provided (handling not-found error).
   - Enforce unit scope validation for `ADMIN_UNIDAD` (cannot register cargos for unauthorized units).
   - Ensure transaction rollback on unexpected validation failures.
