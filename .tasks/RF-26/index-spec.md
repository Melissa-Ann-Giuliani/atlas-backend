# RF-26 — Specification Index: Baja de una materia

## Source artifacts

| File | Purpose |
|---|---|
| [raw-requirement.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-26/raw-requirement.md) | Initial raw requirement: decommissioning / deletion of academic subjects. |
| [open-questions.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-26/open-questions.md) | Business decisions, blockers, risks, and resolved questions log. |
| [solution-diagrams.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-26/solution-diagrams.md) | Technical architecture, sequence, class, and state transition diagrams. |
| [spec-01-baja-materia.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-26/spec-01-baja-materia.md) | Vertical slice specification for subject decommission endpoint. |

---

## Field mapping (Contract ↔ Domain)

| Field | JSON / HTTP Parameter | DB Column (`materias`) | Entity Type | Role in Decommission |
|---|---|---|---|---|
| Identifier | Path variable `{id}` | `materia_id` | `Integer` | Identifies target subject to decommission |
| Subject Status | `MateriaDetailDTO.estado` | `materia_estado` | `String` (max 20) | Updated from `"Activa"` to `"Dada de baja"` upon decommission |
| Subject Name | `MateriaDetailDTO.nombre` | `materia_nombre` | `String` (max 50) | Inmutable / Read-only detail in response |
| Academic Unit | `MateriaDetailDTO.unidadId` | `unidad_id` | `Integer` | Scoping authorization against `ADMIN_UNIDAD` |
| Active Cátedras | N/A | Table `catedras` (`materia_id`) | Relation | Integrity validation: must not have cátedras in status `"ACTIVA"` |

---

## Execution order

| Order | Spec file | Summary | Depends on |
|---|---|---|---|
| 1 | [spec-01-baja-materia.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-26/spec-01-baja-materia.md) | Subject decommission endpoint (`PATCH /api/materias/{id}/baja`) with lifecycle state update, security scoping, active cátedra validation, and unit/integration tests | RF-24 (`Materia` entity & repo), RF-25, `CatedraRepository` |

---

## Suggested increments / Backbone

1. **Domain & DTO updates**:
   - Add `materia_estado` (default `"Activa"`) in [Materia.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/materia/Materia.java).
   - In [MateriaDetailDTO.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/materia/MateriaDetailDTO.java), expose field `private String estado;`.
   - In `crearMateria` ([MateriaService.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/materia/MateriaService.java)), initialize `materia.setEstado("Activa")`.
2. **Decommission Endpoint Slice (Spec 01)**:
   - Expose `PATCH /api/materias/{id}/baja` in [MateriaController.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/materia/MateriaController.java).
   - Implement `darDeBajaMateria(Integer id, String username)` in [MateriaService.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/materia/MateriaService.java).
   - Validate existence (`404`), prior decommission state (`400`), unit scoping (`403`), and active cátedras (`409`).
   - Persist state `"Dada de baja"` and return `MateriaDetailDTO`.
3. **Automated Testing**:
   - Add unit tests in `MateriaServiceTest.java`.
   - Add integration tests in `MateriaControllerTest.java`.
