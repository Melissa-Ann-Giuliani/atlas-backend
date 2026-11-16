# RF-36 — specification index: Mantener un control sobre los próximos vencimientos de designaciones

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Requerimiento original para el control y alerta de próximos vencimientos de designaciones (RF-36) |
| `open-questions.md` | Registro de preguntas abiertas y decisiones de diseño acordadas (B1: Opción B, B2: Opción B, R1: Opción A, R2: Opción A, M1: Opción A) |
| `spec-01-consulta-vencimientos-gestion.md` | Especificación de endpoint y pantalla de listado detallado en *Gestión > Vencimientos* con control RBAC multi-tenant, umbral estático de 30 días y opción para listar vencidas recientes |
| `spec-02-alerta-vencimientos-dashboard.md` | Especificación de endpoint y tarjeta/alerta en Dashboard al iniciar sesión para notificar conteo de designaciones por vencer |
| `solution-diagrams.md` | Diagramas de secuencia y arquitectura para la consulta de vencimientos y alertas |

## Field mapping (contract ↔ Core)

### Request: `GET /api/designaciones/vencimientos` & `GET /api/designaciones/vencimientos/resumen`

| Contract Parameter | Java Type | Domain Field | Validaciones |
| ------------------ | --------- | ------------ | ------------ |
| `incluirVencidas` (Query, solo en listado) | `Boolean` | Rango de fechas | Opcional, por defecto `false`. Si es `true`, incluye designaciones donde `fechaFin < CURRENT_DATE`. (Decisión B2) |
| `unidadId` (Query) | `Integer` | `Cargo.unidad.id` | Opcional. Permitido solo para `ADMIN_GLOBAL`. Si un `ADMIN_UNIDAD` envía un ID ajeno, retorna `403 Forbidden`. |
| *Umbral de evaluación* | Constante (30 días) | Ventana temporal relativa a `now()` | Fijo en el sistema: `CURRENT_DATE + 30 días` (Decisión B1). |
| *Estado auditado* | Constante ("Activa") | `Designacion.designacionEstadoActual` | Filtro estricto por `designacion_estado_actual = 'Activa'` y `fechaFin IS NOT NULL` (Decisión R2). |

### Response: `DesignacionVencimientoDTO` (`spec-01`) — DTO Enriquecido (Decisión M1)

| Contract Field | Java Type | Domain / Source Field | Descripción |
| -------------- | --------- | --------------------- | ----------- |
| `designacionId` | `Integer` | `Designacion.designacionId` | ID de la designación |
| `numeroResolucion` | `String` | `Designacion.designacionNumeroResolucion` | Número oficial de resolución |
| `fechaInicio` | `LocalDate` | `Designacion.designacionFechaInicio` | Fecha de inicio |
| `fechaFin` | `LocalDate` | `Designacion.designacionFechaFin` | Fecha límite de la designación |
| `diasRestantes` | `Long` | `ChronoUnit.DAYS.between(now, fechaFin)` | Días restantes antes de vencer (negativo si está vencida) |
| `estadoActual` | `String` | `Designacion.designacionEstadoActual` | Estado actual (`"Activa"`) |
| `docente.docenteId` | `Integer` | `Docente.id` / `Usuario.id` | Identificador del docente asignado |
| `docente.nombre` | `String` | `Usuario.nombre` | Nombre del docente |
| `docente.apellido` | `String` | `Usuario.apellido` | Apellido del docente |
| `docente.documento` | `String` | `Usuario.documento` | DNI / Documento del docente |
| `docente.legajo` | `String` | `Docente.legajo` | Legajo del docente |
| `unidad.unidadId` | `Integer` | `Unidad.id` | ID de la unidad académica |
| `unidad.nombre` | `String` | `Unidad.nombre` | Nombre de la unidad académica |
| `cargos` | `List<CargoResumenDTO>` | `cargos` asociados a la designación | Códigos, categoría, dedicación y carácter |

### Response: `DesignacionVencimientosResumenDTO` (`spec-02`) (Decisión R1)

| Contract Field | Java Type | Descripción |
| -------------- | --------- | ----------- |
| `totalPorVencer` | `Long` | Cantidad total de designaciones a vencer dentro de los 30 días |
| `umbralDias` | `Integer` | Fijo en 30 |
| `unidadId` | `Integer` | ID de la unidad (null si es consulta global) |
| `unidadNombre` | `String` | Nombre de la unidad del usuario (null si es global) |
| `tieneVencimientosCriticos` | `Boolean` | `true` si hay designaciones a vencer en $\le 7$ días |
| `totalCriticos` | `Long` | Cantidad de designaciones con vencimiento en $\le 7$ días |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-consulta-vencimientos-gestion.md` | Endpoint `GET /api/designaciones/vencimientos` con filtrado estricto por unidad para administradores de unidad, umbral estático de 30 días, flag opcional `incluirVencidas`, filtrado por estado `"Activa"`, cálculo de días restantes y DTO enriquecido. | RF-09 (Designación), RF-10 (Cargos vinculados) |
| 2 | `spec-02-alerta-vencimientos-dashboard.md` | Endpoint `GET /api/designaciones/vencimientos/resumen` para tarjeta de alerta de entrada en el Dashboard con totales a vencer y alertas críticas. | `spec-01` (Queries base en repositorio) |

## Suggested increments / Backbone

1. **Repository Queries (`DesignacionRepository`)**:
   - Métodos JPQL o Specifications para consultar designaciones con `designacionEstadoActual = 'Activa'` y `designacionFechaFin BETWEEN CURRENT_DATE AND (CURRENT_DATE + 30 DAYS)` (o `fechaFin < CURRENT_DATE` cuando se soliciten vencidas), filtrando por `unidadId` vía los cargos vinculados.
2. **Service Layer (`DesignacionService`)**:
   - Métodos `obtenerVencimientos(...)` y `obtenerResumenVencimientos(...)` con cálculo de `ChronoUnit.DAYS.between(hoy, fechaFin)` y categorización de días críticos ($\le 7$).
3. **Controller Layer (`DesignacionController`)**:
   - Endpoints `/api/designaciones/vencimientos` y `/api/designaciones/vencimientos/resumen` securizados con `@PreAuthorize`.
4. **Integration & Security Tests**:
   - Verificación de aislamiento estricto de unidades para `ADMIN_UNIDAD` (`403 Forbidden` al solicitar otra unidad) y cálculo de días restantes.
