# RF-24 — specification index

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Original requirement for registering a new subject (alta de una materia) |
| `spec-01-alta-materia.md` | Formal specification in Gherkin for creating the subject and validations |
| `open-questions.md` | Business and technical decisions (plan de estudio, year format, unit scoping, etc.) |
| `solution-diagrams.md` | Sequence, class, and entity relationship diagrams for the subject registration flow |

## Field mapping (contract ↔ Core)

| JSON/Contract Field | DTO | Domain | Description |
| ------------------- | --- | ------ | ----------- |
| `nombre` | `MateriaCreateRequest.nombre` | `Materia.nombre` | Subject name (`String`, max 50) |
| `horas` | `MateriaCreateRequest.horas` | `Materia.horas` | Weekly hours workload (`Integer`, 1 to 40) |
| `dependenciaId` | `MateriaCreateRequest.dependenciaId` | `Unidad.tipoUnidad.id` | Academic dependency/unit type ID (`Integer`, optional/derived from unidad) |
| `unidadId` | `MateriaCreateRequest.unidadId` | `Materia.unidad.id` | Academic Unit ID (`Integer`) |
| `anio` | `MateriaCreateRequest.anio` | `Materia.anio` | Educational academic year level (`String`, e.g. `"1°"`, `"2°"`, `"3°"`, `"4°"`, `"5°"`) |
| `despliegue` | `MateriaCreateRequest.despliegue` | `Materia.despliegue` | Deployment schedule: `"Anual"`, `"Primer Semestre"`, `"Segundo Semestre"` |
| `planEstudio` | `MateriaCreateRequest.planEstudio` | `Materia.planEstudio` | Study plan year identifier (`Integer`, e.g. `2023`) |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-alta-materia.md` | Subject registration (Alta de materia), validation rules, composite uniqueness check `(nombre, unidadId, planEstudio)`, and unit scoping for Admin de Unidad | None |

## Suggested increments / Backbone

1. **Walking Skeleton / MVP**:
   - Create `POST /api/materias` endpoint accepting `MateriaCreateRequest`.
   - Update `Materia` domain entity and table `materias` to include `materia_plan_estudio` (Integer) and adjust `materia_anio` to String (varchar(10)).
   - Validate basic mandatory fields and value ranges (hours 1..40, valid year format and deployment values).
   - Persist entity in `MateriaRepository` linked to target `Unidad`.
   - Return HTTP 201 Created with saved `MateriaDetailDTO`.
2. **Make it Robust & Releasable**:
   - Enforce composite duplicate check (`existsByNombreIgnoreCaseAndUnidadIdAndPlanEstudio`) returning HTTP 409 Conflict.
   - Enforce strict unit authorization for `ADMIN_UNIDAD` (matching the user's assigned `unidadId`, returning HTTP 403 Forbidden on mismatch).
   - Verify that `dependenciaId` (if sent) corresponds to the `tipoUnidad` of the selected `Unidad`.
