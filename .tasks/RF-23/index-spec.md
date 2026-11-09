# RF-23 — specification index: Baja de una cátedra

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Requerimiento original para la baja de cátedra (RF-23) |
| `open-questions.md` | Registro de decisiones cerradas (B1: Opción A, B2: No aplica, R1: Opción A, R2: Opción A, R3: Opción A, M1: Opción A) |
| `spec-01-baja-catedra.md` | Especificación formal en Gherkin para el endpoint `PATCH /api/catedras/{id}/baja`, validaciones de existencia, estado y alcance |
| `solution-diagrams.md` | Diagramas de secuencia, flujo de decisiones y clases de la solución |

## Field mapping (contract ↔ Core)

### Request: `PATCH /api/catedras/{id}/baja`

| Contract Field | Java Field / Type | Domain Field | Validaciones |
| -------------- | ----------------- | ------------ | ------------ |
| `id` (PathVariable) | `Integer id` | `Catedra.id` | Debe existir en BD (`404 Not Found`), no encontrarse ya `"De baja"` (`400 Bad Request`) y pertenecer a la unidad del `ADMIN_UNIDAD` (`403 Forbidden`). |

> **Request Body:** Vacío (sin payload).

### Response: `200 OK` (`CatedraResponseDTO`)

| Contract Field | Domain Field | Descripción |
| -------------- | ------------ | ----------- |
| `id` | `Catedra.id` | Identificador único de la cátedra |
| `nombre` | `Catedra.nombre` | Nombre de la cátedra preservado |
| `tipologia` | `Catedra.tipologia` | Tipología preservada |
| `estado` | `Catedra.estado` | Estado actualizado a `"De baja"` |
| `directorId` | `Catedra.director.id` | ID del último docente director (preservado como histórico) |
| `directorNombre` | `Catedra.director.nombre` + `apellido` | Nombre completo del último director |
| `materiaId` | `Catedra.materia.id` | ID de la materia asociada (preservado) |
| `materiaNombre` | `Catedra.materia.nombre` | Nombre de la materia asociada |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-baja-catedra.md` | Endpoint de ciclo de vida `PATCH /api/catedras/{id}/baja`, validación de existencia previa, control de unidad académica para `ADMIN_UNIDAD`, rechazo si ya está `"De baja"` y preservación de director histórico con retorno `200 OK` (`CatedraResponseDTO`). | RF-21 (`Catedra`, `CatedraService`, `CatedraController`) |

## Suggested increments / Backbone

1. **Walking Skeleton / Core Implementation**:
   - En `CatedraService`:
     - Implementar método `darDeBajaCatedra(Integer id, String username)`:
       - Buscar cátedra por `id` (`404 Not Found` si no existe).
       - Si el usuario es `ADMIN_UNIDAD`, validar pertenencia de la materia a su unidad (`403 Forbidden` si difiere).
       - Verificar que el estado no sea `"De baja"` (`400 Bad Request` si ya está de baja).
       - Actualizar `catedra.setEstado("De baja")` manteniendo intactos el director y demás atributos.
       - Persistir y mapear a `CatedraResponseDTO`.
   - En `CatedraController`:
     - Exponer endpoint `PATCH /api/catedras/{id}/baja` restringido a roles `ADMIN_GLOBAL` y `ADMIN_UNIDAD`.
2. **Testing & Verification**:
   - Tests unitarios en `CatedraServiceTest` cubriendo:
     - Baja exitosa por `ADMIN_GLOBAL`.
     - Baja exitosa por `ADMIN_UNIDAD` para materia de su unidad.
     - Error 403 para `ADMIN_UNIDAD` sobre materia de otra unidad.
     - Error 404 para ID inexistente.
     - Error 400 cuando la cátedra ya está `"De baja"`.
     - Verificación de que el director histórico no es removido (`director != null`).
   - Tests de integración en `CatedraControllerTest` validando códigos HTTP y seguridad.

