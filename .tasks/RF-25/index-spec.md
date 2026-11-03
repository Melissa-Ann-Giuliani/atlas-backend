# RF-25 — Specification Index: Modificar los datos de una materia

## Source artifacts

| File | Purpose |
|---|---|
| [raw-requirement.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-25/raw-requirement.md) | Initial raw requirement: modification of academic subjects. |
| [open-questions.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-25/open-questions.md) | Business decisions, blockers, risks, and resolved questions log. |
| [solution-diagrams.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-25/solution-diagrams.md) | Technical architecture and sequence diagrams for the solution. |
| [spec-01-modificar-materia.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-25/spec-01-modificar-materia.md) | Vertical slice specification for subject modification endpoint. |

---

## Field mapping (Contract ↔ Domain)

| Field | JSON Request (`PATCH /api/materias/{id}`) | DB Column (`materias`) | Entity Type | Modifiable? | Validation Rules |
|---|---|---|---|---|---|
| Identifier | Path variable `{id}` | `materia_id` | `Integer` | Inmutable | Required, existing entity ID |
| Name | `nombre` | `materia_nombre` | `String` (max 50) | **Yes** (Optional in PATCH) | Max 50 chars, non-blank if provided, unique per `(unidad, planEstudio)` excluding self ID |
| Weekly Hours | `horas` | `materia_horas` | `Integer` | **Yes** (Optional in PATCH) | `1 <= horas <= 40`. Restringido si existen cátedras activas asociadas |
| Academic Year | `anio` | `materia_anio` | `String` (max 10) | **Yes** (Optional in PATCH) | Format ordinal string (e.g. `"1°"`, `"2°"`) |
| Deployment | `despliegue` | `materia_despliegue` | `String` (max 50) | **Yes** (Optional in PATCH) | Must be `"Anual"`, `"Primer Semestre"`, or `"Segundo Semestre"`. Restringido si existen cátedras activas |
| Study Plan | N/A | `materia_plan_estudio` | `Integer` | **Inmutable** | Preserved from original record (cannot be modified) |
| Academic Unit | N/A | `unidad_id` | `Integer` | **Inmutable** | Preserved from original record (scoped to `ADMIN_UNIDAD`) |

---

## Execution order

| Order | Spec file | Summary | Depends on |
|---|---|---|---|
| 1 | [spec-01-modificar-materia.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-25/spec-01-modificar-materia.md) | Subject modification endpoint (`PATCH /api/materias/{id}`) with partial payload, impact validation, and scoping | RF-24 (`Materia` entity & base repo), `CatedraRepository` |

---

## Suggested increments / Backbone

1. **Walking Skeleton / Core Slice (Spec 01)**:
   - Provide `PATCH /api/materias/{id}` in [MateriaController.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/materia/MateriaController.java).
   - Create `MateriaPatchRequest` with optional/validable fields (`nombre`, `horas`, `anio`, `despliegue`).
   - Implement `modificarMateria(Integer id, MateriaPatchRequest request, String username)` in [MateriaService.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/materia/MateriaService.java).
   - Add query `existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot(String nombre, Integer unidadId, Integer planEstudio, Integer id)` in [MateriaRepository.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/materia/MateriaRepository.java).
   - Query `CatedraRepository.existsByMateria_IdAndEstadoIgnoreCase(id, "ACTIVA")` (or equivalent active check) before mutating `horas` or `despliegue`.
   - Return updated [MateriaDetailDTO.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/materia/MateriaDetailDTO.java).
