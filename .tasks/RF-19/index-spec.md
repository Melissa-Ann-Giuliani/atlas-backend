# RF-19 — specification index

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Original requirement for subdividing a cargo into 2 or more subcargos (RF-19) |
| `spec-01-subdividir-cargo.md` | Formal specification in Gherkin for the subdivision endpoint, hour validation, persistence, and authorization |
| `open-questions.md` | Business and technical decisions log (parent post-division state, hour sum <= vs ==, REST route, scope) |
| `solution-diagrams.md` | Sequence, class, and lifecycle state diagrams for the cargo subdivision flow |

## Field mapping (contract ↔ Core)

### Request: `POST /api/cargos/subdividir` (`SubdividirCargoRequest`)

| JSON/Contract Field | DTO | Domain | Description |
| ------------------- | --- | ------ | ----------- |
| `cargoOrigenCodigo` | `SubdividirCargoRequest.cargoOrigenCodigo` | `Cargo.codigo` (lookup) | Public code of the parent cargo to be subdivided (`Long`) |
| `subcargos` | `SubdividirCargoRequest.subcargos` | `List<Cargo>` | List of $2 \le N \le 4$ subcargos to create |

### Subcargo item: `SubcargoItemRequest`

| JSON/Contract Field | DTO | Domain | Description |
| ------------------- | --- | ------ | ----------- |
| `codigo` | `SubcargoItemRequest.codigo` | `Cargo.codigo` | Unique public code for the new subcargo (`Long`) |
| `fechaCreacion` | `SubcargoItemRequest.fechaCreacion` | `Cargo.fechaCreacion` | Creation date for the new subcargo (`LocalDate`) |
| `origenId` | `SubcargoItemRequest.origenId` | `Cargo.origen.origenId` | Budget origin ID (`Integer`) |
| `caracterId` | `SubcargoItemRequest.caracterId` | `Cargo.caracter.caracterId` | Position character/nature ID (`Integer`) |
| `categoriaId` | `SubcargoItemRequest.categoriaId` | `Cargo.categoria.categoriaId` | Academic rank/category ID (`Integer`) |
| `dedicacionId` | `SubcargoItemRequest.dedicacionId` | `Cargo.dedicacion.dedicacionId` | Commitment / dedication ID (`Integer`) |
| `unidadId` | `SubcargoItemRequest.unidadId` | `Cargo.unidad.unidadId` | Academic unit ID (`Integer`) |
| `predecesorCodigo` | `SubcargoItemRequest.predecesorCodigo` | `Cargo.cargoPredecesor` | Optional predecessor cargo code (`Long`) |
| *(implicit)* | *From parent lookup* | `Cargo.cargoOrigenDivision` | Reference to parent cargo entity being subdivided (`Cargo`) |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-subdividir-cargo.md` | Endpoint `POST /api/cargos/subdividir`, hour budget validation, subcargos batch creation, atomic parent status/hour update | None |

## Suggested increments / Backbone

1. **Walking Skeleton / MVP**:
   - Create DTOs (`SubdividirCargoRequest`, `SubcargoItemRequest`, response DTO).
   - Expose endpoint `POST /api/cargos/subdividir` in `CargoController`.
   - Validate parent cargo existence and unit-level permissions (`ADMIN_UNIDAD` vs `ADMIN_GLOBAL`).
   - Validate parent cargo state is `"Libre"` or `"Licencia"`.
   - Validate subcargos list size ($2 \le N \le 4$) and code uniqueness.
   - Validate hours sum: $\sum \text{subcargo.horas} \le \text{parent.horas}$.
   - Persist subcargos with `cargoOrigenDivision = parentCargo`, `estado = "Libre"`, and set parent `horasOcupadas` to 100%.
2. **Make it Robust & Releasable**:
   - Ensure foreign key catalogs (origen, caracter, categoria, dedicacion, unidad) are checked and return 404 if invalid.
   - Comprehensive unit and integration tests across happy and unhappy scenarios in `CargoControllerTest` and `CargoServiceTest`.
