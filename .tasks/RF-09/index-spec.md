# RF-09 — specification index: Registrar las designaciones de docentes

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Requerimiento original para el registro de designaciones de docentes (RF-09) |
| `open-questions.md` | Registro de decisiones acordadas (B1: Opción A, B2: Opción A, B3: Opción A, R1: Opción A, R2: Opción A, R3: Opción A, M1: Opción A) |
| `spec-01-registrar-designacion.md` | Especificación formal en Gherkin para el endpoint `POST /api/designaciones`, validaciones de negocio, estados, permisos y notificación |
| `solution-diagrams.md` | Diagramas de secuencia, clases y ciclo de vida de entidades |

## Field mapping (contract ↔ Core)

### Request: `POST /api/designaciones` (`DesignacionCreateRequest`)

| Contract Field | Java Field / Type | Domain Field | Validaciones |
| -------------- | ----------------- | ------------ | ------------ |
| `docenteId` | `Integer docenteId` | `Docente.usuarioId` | Obligatorio (`@NotNull`). Debe existir en `docentes` (`404 Not Found`). |
| `cargoId` | `Integer cargoId` | `Cargo.id` | Obligatorio (`@NotNull`, Decisión B1). Debe existir (`404 Not Found`), estar en estado `"Libre"` (`400 Bad Request`) y pertenecer a la unidad del `ADMIN_UNIDAD` (`403 Forbidden`, Decisión R1). |
| `nroResolucion` | `Integer nroResolucion` | `Designacion.designacionNumeroResolucion` | Obligatorio (`@NotNull`). No debe existir previamente para el mismo docente (`400 Bad Request`, Decisión R3). |
| `fechaInicio` | `LocalDate fechaInicio` | `Designacion.designacionFechaInicio` | Obligatorio (`@NotNull`). Formato ISO (`YYYY-MM-DD`). |
| `fechaFin` | `LocalDate fechaFin` | `Designacion.designacionFechaFin` | Opcional. Si se provee, no puede ser anterior a `fechaInicio` (`400 Bad Request`). |

### Response: `201 Created` (`DesignacionResponseDTO`)

| Contract Field | Domain Field | Descripción |
| -------------- | ------------ | ----------- |
| `id` | `Designacion.designacionId` | Identificador autogenerado de la designación |
| `nroResolucion` | `Designacion.designacionNumeroResolucion` | Número oficial de resolución administrativa |
| `fechaInicio` | `Designacion.designacionFechaInicio` | Fecha de inicio de la designación |
| `fechaFin` | `Designacion.designacionFechaFin` | Fecha de finalización (o `null` si es por tiempo indeterminado) |
| `estado` | `Designacion.designacionEstadoActual` | Estado inicial `"Activo"` |
| `docente` | `DocenteResumenDTO` | Datos del docente designado y activado (`id`, `nombre`, `apellido`, `dni`, `email`) |
| `cargo` | `CargoResumenDTO` | Datos del cargo asociado y activado (`id`, `codigo`, `categoria`, `dedicacion`, `unidad`) |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-registrar-designacion.md` | Endpoint `POST /api/designaciones`, validaciones de docente y cargo (estado `"Libre"`), validación de unicidad de resolución por docente, persistencia transaccional con activación simultánea (`Docente` a `"Activo"`, `Cargo` a `"Asignado"`), control de alcance para `ADMIN_UNIDAD` y notificación asíncrona fail-safe vía `EmailService`. | RF-17 (Cargos), RF-30 (Docentes) |

## Suggested increments / Backbone

1. **Walking Skeleton / Core Implementation**:
   - Crear DTOs: `DesignacionCreateRequest` y `DesignacionResponseDTO` (con sus DTOs anidados de resumen).
   - En `DesignacionRepository`:
     - Agregar método de verificación de resolución: `boolean existsByUsuarioIdAndDesignacionNumeroResolucion(Integer usuarioId, Integer designacionNumeroResolucion)`.
   - En `EmailService`:
     - Agregar método asíncrono `sendDesignacionNotification(String to, Integer resolucion, LocalDate fechaInicio, LocalDate fechaFin)`.
   - En `DesignacionService`:
     - Implementar `registrarDesignacion(DesignacionCreateRequest request, String username)`:
       - Validar existencia de docente (`404 Not Found`).
       - Validar unicidad de resolución para el docente (`400 Bad Request`).
       - Validar coherencia de fechas (`fechaFin >= fechaInicio`).
       - Validar existencia de cargo (`404 Not Found`).
       - Validar pertenencia a unidad para `ADMIN_UNIDAD` (`403 Forbidden`).
       - Validar que el cargo esté `"Libre"` (`400 Bad Request`).
       - Crear y persistir `Designacion` con estado `"Activo"`.
       - Asociar cargo: `cargo.setDesignacion(designacion)`, `cargo.setEstado("Asignado")`.
       - Actualizar docente: `docente.setEstado("Activo")`, `usuario.setActivo(true)`.
       - Enviar notificación asíncrona fail-safe.
       - Mapear y retornar `DesignacionResponseDTO`.
   - En `DesignacionController`:
     - Exponer `POST /api/designaciones` con `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")` retornando `201 Created`.
2. **Testing & Verification**:
   - Tests unitarios en `DesignacionServiceTest` cubriendo flujos exitosos, errores (400, 403, 404) y resiliencia de email.
   - Tests de integración en `DesignacionControllerTest` validando códigos HTTP y seguridad por roles.
