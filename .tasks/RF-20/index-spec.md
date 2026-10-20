# RF-20 — specification index

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Original requirement for decommissioning a cargo (RF-20) |
| `open-questions.md` | Business and technical decisions log (designation validity, REST endpoint, role scope, status) |
| `spec-01-baja-cargo.md` | Formal specification in Gherkin for the decommission endpoint, business validations, and authorization |
| `solution-diagrams.md` | Sequence, class, and lifecycle state diagrams for cargo decommissioning |

## Field mapping (contract ↔ Core)

### Request: `PATCH /api/cargos/{id}/baja`

| Parameter / Path | DTO / Parameter | Domain | Description |
| ---------------- | --------------- | ------ | ----------- |
| `{id}` (Path Variable) | `Integer cargoId` | `Cargo.id` | Primary key identifier of the cargo to decommission |
| *(Request Body)* | *None* | *N/A* | No request body is required for this action |

### Response: `200 OK` (`CargoDetailDTO`)

| Contract Field | Domain Field | Description |
| -------------- | ------------ | ----------- |
| `id` | `Cargo.id` | Cargo identifier |
| `codigo` | `Cargo.codigo` | Public cargo code |
| `horasOcupadas` | `Cargo.horasOcupadas` | Reset to `0` upon decommission |
| `fechaCreacion` | `Cargo.fechaCreacion` | Creation date |
| `estado` | `Cargo.estado` | Updated to `"Dado de baja"` |
| `categoria` | `Cargo.categoria.categoriaNombre` | Cargo category name |
| `dedicacion` | `Cargo.dedicacion.dedicacionNombre` | Dedication name |
| `caracter` | `Cargo.caracter.caracterNombre` | Character name |
| `unidad` | `Cargo.unidad.unidadNombre` | Unit name |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-baja-cargo.md` | Endpoint `PATCH /api/cargos/{id}/baja`, authorization checks (`ADMIN_GLOBAL`), active designation validation, atomic status transition to `"Dado de baja"` | None |

## Suggested increments / Backbone

1. **Walking Skeleton / Core Implementation**:
   - Implement service method `darDeBajaCargo(Integer id, String username)` in `CargoService`.
   - Validate authorization: caller must possess `ADMIN_GLOBAL` role.
   - Validate cargo existence: throw `404 Not Found` if not present.
   - Validate current state: throw `400 Bad Request` if already `"Dado de baja"`.
   - Validate designation: throw `400 Bad Request` if linked to an active designation (`designacionFechaFin >= today` and not finalized).
   - Validate derived subcargos: check `cargoRepository.findByCargoOrigenDivisionId(id)` and throw `400 Bad Request` if any child subcargo has `estado != "Dado de baja"`.
   - Set `cargo.estado = "Dado de baja"`, reset `cargo.horasOcupadas = 0`, and persist.
   - Expose endpoint `PATCH /api/cargos/{id}/baja` in `CargoController`.
   - Update `CargoRepository.findActiveCargos` to exclude cargos with `estado = 'Dado de baja'` from general active search results when `estado` is not explicitly requested.
2. **Make it Robust & Automated Verification**:
   - Add unit tests in `CargoServiceTest` covering all happy and unhappy scenarios (active assignment, missing cargo, already decommissioned, unauthorized role, active child subcargos).
   - Add integration tests in `CargoControllerTest` with MockMvc verifying HTTP response codes (`200`, `400`, `403`, `404`).
