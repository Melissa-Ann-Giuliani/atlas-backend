# RF-22 — specification index: Modificar los datos de una cátedra

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Requerimiento original para la modificación de cátedra (RF-22) |
| `open-questions.md` | Registro de decisiones cerradas (B1: Opción A, B2: Opción A, R1: Opción A, R2: Opción A, M1: Opción A) |
| `spec-01-modificar-catedra.md` | Especificación formal en Gherkin para el endpoint `PUT /api/catedras/{id}`, validaciones y autorización |
| `solution-diagrams.md` | Diagramas de secuencia, clases y flujo de validación de la solución |

## Field mapping (contract ↔ Core)

### Request: `PUT /api/catedras/{id}` (`CatedraUpdateRequestDTO`)

| Contract Field | Java Field / Type | Domain Field | Validaciones |
| -------------- | ----------------- | ------------ | ------------ |
| `id` (PathVariable) | `Integer id` | `Catedra.id` | Debe existir en BD (`404 Not Found`) y estar en estado `"Activa"` (`400 Bad Request`) |
| `nombre` | `String nombre` | `Catedra.nombre` | `@NotBlank`, `@Size(max = 50)`, único en la materia (`id != actual`) |
| `tipologia` | `Integer tipologia` | `Catedra.tipologia` | `@NotNull`, `@Min(1)`, `@Max(4)` (Tipologías I a IV) |
| `directorId` | `Integer directorId` | `Catedra.director` (`Docente`) | `@NotNull`, debe existir (`404`) y tener estado `"Activo"` (`400`) |

> **Campos inmutables:** `Catedra.materia` y `Catedra.estado` no se modifican a través de este endpoint.

### Response: `200 OK` (`CatedraResponseDTO`)

| Contract Field | Domain Field | Descripción |
| -------------- | ------------ | ----------- |
| `id` | `Catedra.id` | Identificador único de la cátedra |
| `nombre` | `Catedra.nombre` | Nombre actualizado de la cátedra |
| `tipologia` | `Catedra.tipologia` | Tipología actualizada (1 a 4) |
| `estado` | `Catedra.estado` | Estado actual de la cátedra (`"Activa"`) |
| `directorId` | `Catedra.director.id` | ID del docente director asignado |
| `directorNombre` | `Catedra.director.nombre` + `apellido` | Nombre completo del director |
| `materiaId` | `Catedra.materia.id` | ID de la materia asociada (inmutable) |
| `materiaNombre` | `Catedra.materia.nombre` | Nombre de la materia |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-modificar-catedra.md` | Endpoint `PUT /api/catedras/{id}` con `CatedraUpdateRequestDTO`, validación de existencia previa y estado `"Activa"`, control de unidad académica para `ADMIN_UNIDAD`, validación de unicidad excluyendo el propio ID y validación de docente activo | RF-21 (`Catedra`, `CatedraService`, `CatedraController`) |

## Suggested increments / Backbone

1. **Walking Skeleton / Core Implementation**:
   - Crear `CatedraUpdateRequestDTO` en `com.atlas.atlas_backend.catedra.dto` con `nombre`, `tipologia` y `directorId`.
   - Agregar método en `CatedraRepository`: `existsByNombreIgnoreCaseAndMateria_IdAndIdNot(String nombre, Integer materiaId, Integer id)`.
   - Implementar método `modificarCatedra(Integer id, CatedraUpdateRequestDTO request, String username)` en `CatedraService`:
     - Verificar existencia de `Catedra` por `id` (`404 Not Found`).
     - Verificar pertenencia a la unidad académica si el usuario es `ADMIN_UNIDAD` (`403 Forbidden`).
     - Verificar que la cátedra esté en estado `"Activa"` (`400 Bad Request`).
     - Verificar existencia y estado `"Activo"` del nuevo `Docente` director (`404` / `400`).
     - Verificar unicidad de nombre dentro de la materia excluyendo el propio `id` (`400 Bad Request`).
     - Actualizar `nombre`, `tipologia` y `director`, persistir y retornar `CatedraResponseDTO`.
   - Exponer el endpoint `PUT /api/catedras/{id}` en `CatedraController` devolviendo `200 OK`.
2. **Make it Robust & Automated Verification**:
   - Agregar tests unitarios en `CatedraServiceTest` para modificación exitosa, conservación del mismo nombre, colisión de nombre con otra cátedra, cátedra inexistente, cátedra de baja, docente inactivo y acceso denegado para `ADMIN_UNIDAD` de otra unidad.
   - Agregar tests de integración en `CatedraControllerTest` con `MockMvc` verificando códigos HTTP (`200`, `400`, `403`, `404`).
