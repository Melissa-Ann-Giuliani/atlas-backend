# RF-10 — specification index: Vincular un cargo a una designación de un docente

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Requerimiento original para vincular un cargo a una designación de un docente (RF-10) |
| `open-questions.md` | Registro de decisiones acordadas (B1: Opción A, B2: Opción A, R1: Other — diferir email a RF-11, R2: Opción A, R3: Opción A, M1: Opción A) |
| `spec-01-vincular-cargo-designacion.md` | Especificación formal en Gherkin para la vinculación de un cargo en estado `"Libre"` a una designación (`PATCH /api/designaciones/{id}/cargo`), validaciones de alcance y transición a `"Asignado"` |
| `solution-diagrams.md` | Diagramas de secuencia, clases y flujo de trabajo entre RF-09, RF-17, RF-10 y RF-11 |

## Field mapping (contract ↔ Core)

### Request: `PATCH /api/designaciones/{id}/cargo` (`DesignacionVincularCargoRequest`)

| Contract Field | Java Field / Type | Domain Field | Validaciones |
| -------------- | ----------------- | ------------ | ------------ |
| `id` (Path) | `Integer id` | `Designacion.designacionId` | Obligatorio. Debe existir en `designaciones` (`404 Not Found`) y no poseer ya un cargo vinculado (`400 Bad Request`, Decisión B2). |
| `cargoId` (Body) | `Integer cargoId` | `Cargo.id` | Obligatorio (`@NotNull`). Debe existir en `cargos` (`404 Not Found`), estar en estado `"Libre"` (`400 Bad Request`) y pertenecer a la unidad del `ADMIN_UNIDAD` (`403 Forbidden`, Decisión R3). |

### Response: `200 OK` (`DesignacionResponseDTO`)

| Contract Field | Domain Field | Descripción |
| -------------- | ------------ | ----------- |
| `id` | `Designacion.designacionId` | Identificador de la designación |
| `nroResolucion` | `Designacion.designacionNumeroResolucion` | Número oficial de resolución administrativa (`String`) |
| `fechaInicio` | `Designacion.designacionFechaInicio` | Fecha de inicio de la designación |
| `fechaFin` | `Designacion.designacionFechaFin` | Fecha de finalización (o `null` si es abierta) |
| `estado` | `Designacion.designacionEstadoActual` | Estado de la designación (`"Activo"`) |
| `docente` | `DocenteResumenDTO` | Datos del docente designado (`id`, `legajo`, `nombre`, `apellido`, `dni`, `email`) |
| `cargo` | `CargoResumenDTO` | Datos del cargo vinculado en estado `"Asignado"` (`id`, `codigo`, `categoria`, `dedicacion`, `unidad`) |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-vincular-cargo-designacion.md` | Endpoint `PATCH /api/designaciones/{id}/cargo` (y soporte en `POST /api/designaciones`), validación de existencia de designación y cargo en estado `"Libre"`, rechazo si la designación ya posee cargo (`400`), control de alcance por unidad para `ADMIN_UNIDAD` (`403`), transición del cargo a `"Asignado"` y diferimiento del envío de email hasta RF-11. | RF-09 (Designaciones), RF-17 (Alta de Cargo), RF-06 (Listado de Cargos) |

## Suggested increments / Backbone

1. **Walking Skeleton / Core Implementation**:
   - Crear DTO `DesignacionVincularCargoRequest` con `@NotNull Integer cargoId`.
   - En `DesignacionService`:
     - Implementar `vincularCargo(Integer designacionId, Integer cargoId, String username)` bajo `@Transactional`:
       - Verificar existencia de `Designacion` (`404 Not Found`).
       - Verificar que la designación no tenga ya un cargo vinculado mediante `cargoRepository.findByDesignacionDesignacionIdIn(List.of(designacionId))` (`400 Bad Request`: `"La designación ya posee un cargo vinculado"`).
       - Verificar existencia de `Cargo` (`404 Not Found`).
       - Validar alcance de unidad para `ADMIN_UNIDAD` (`403 Forbidden`: `"No tiene permisos para vincular cargos de otra unidad"`).
       - Validar que `cargo.getEstado()` sea `"Libre"` (`400 Bad Request`: `"El cargo ya se encuentra asignado o en licencia"`).
       - Vincular `cargo.setDesignacion(designacion)` y cambiar `cargo.setEstado("Asignado")`.
       - Omitir el envío de notificación por correo electrónico en RF-09 y RF-10 (queda diferido a RF-11 según Decisión R1).
       - Retornar `DesignacionResponseDTO`.
   - En `DesignacionController`:
     - Exponer `PATCH /api/designaciones/{id}/cargo` con `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")`.
2. **Testing & Verification**:
   - Agregar pruebas unitarias en `DesignacionServiceTest` y de controlador en `DesignacionControllerTest` para `vincularCargo` cubriendo los escenarios T1–T9 y actualizando la expectativa de envío de email conforme a la Decisión R1.
