# RF-21 — specification index: Alta de una cátedra

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Requerimiento original para el alta de cátedra (RF-21) |
| `open-questions.md` | Registro de decisiones técnicas y de negocio (tipología, unicidad, exclusividad director, autorización) |
| `spec-01-alta-catedra.md` | Especificación formal en Gherkin para el endpoint de creación, validaciones y autorización |
| `solution-diagrams.md` | Diagramas de secuencia, clases y ciclo de vida de la solución |

## Field mapping (contract ↔ Core)

### Request: `POST /api/catedras` (`CatedraRequestDTO`)

| Contract Field | Java Field / Type | Domain Field | Validaciones |
| -------------- | ----------------- | ------------ | ------------ |
| `nombre` | `String nombre` | `Catedra.nombre` | `@NotBlank`, `@Size(max = 50)` |
| `tipologia` | `Integer tipologia` | `Catedra.tipologia` | `@NotNull`, `@Min(1)` |
| `directorId` | `Integer directorId` | `Catedra.director` (Docente) | `@NotNull` |
| `materiaId` | `Integer materiaId` | `Catedra.materia` (Materia) | `@NotNull` |

### Response: `201 Created` (`CatedraResponseDTO`)

| Contract Field | Domain Field | Descripción |
| -------------- | ------------ | ----------- |
| `id` | `Catedra.id` | Identificador único autogenerado |
| `nombre` | `Catedra.nombre` | Nombre de la cátedra |
| `tipologia` | `Catedra.tipologia` | Tipología asignada |
| `estado` | `Catedra.estado` | Estado inicial asignado (`"Activo"`) |
| `directorId` | `Catedra.director.id` | ID del docente director |
| `directorNombre` | `Catedra.director.nombre` + `apellido` | Nombre completo del director |
| `materiaId` | `Catedra.materia.id` | ID de la materia asociada |
| `materiaNombre` | `Catedra.materia.nombre` | Nombre de la materia |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-alta-catedra.md` | Endpoint `POST /api/catedras`, validación de roles (`ADMIN_GLOBAL`, `ADMIN_UNIDAD`), chequeo de unicidad de cátedra por materia, exclusividad de director de cátedra y persistencia con estado activo | None |

## Suggested increments / Backbone

1. **Walking Skeleton / Core Implementation**:
   - Crear DTOs de request y response (`CatedraRequestDTO`, `CatedraResponseDTO`) en `com.atlas.atlas_backend.catedra.dto`.
   - Agregar métodos en `CatedraRepository`: `existsByNombreIgnoreCaseAndMateriaId(String nombre, Integer materiaId)` y `existsByDirectorId(Integer directorId)`.
   - Implementar método `crearCatedra(CatedraRequestDTO request, String username)` en `CatedraService`:
     - Validar existencia de `Materia` y `Docente`.
     - Validar pertenencia de unidad si el usuario es `ADMIN_UNIDAD`.
     - Validar que la cátedra no exista previamente en esa materia.
     - Validar que el docente no sea ya director de otra cátedra.
     - Construir entidad `Catedra` con `estado = "Activo"` y persistir.
   - Exponer endpoint `POST /api/catedras` en `CatedraController` protegido con `@PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_UNIDAD')")`.
2. **Make it Robust & Automated Verification**:
   - Agregar tests unitarios en `CatedraServiceTest` cubriendo todos los caminos exitosos y fallidos (unicidad duplicada, director duplicado, materia inexistente, validación de unidad).
   - Agregar tests de integración en `CatedraControllerTest` con `MockMvc` verificando códigos HTTP (`201`, `400`, `403`, `404`).
