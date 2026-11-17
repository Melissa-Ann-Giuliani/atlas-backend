# RF-37 — specification index: Permitir la re-designación para el docente con su designación por vencer de forma simple

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Requerimiento original de re-designación ágil para docentes con designación próxima a vencer (RF-37) |
| `open-questions.md` | Registro de decisiones acordadas (B1: Opción A, B2: Other/Form precargado, R1: Opción B + Notas R1 sobre `dias`/`sinLimite`, R2: Opción A/RBAC estricto, M1: Opción A) |
| `spec-01-redesignacion-simple.md` | Especificación funcional y de contrato del endpoint `POST /api/designaciones/{id}/redesignar` para crear nueva designación y archivar la anterior como `"Renovada"` |
| `spec-02-flexibilidad-filtro-vencimientos.md` | Especificación detallada de los parámetros opcionales (`dias`, `sinLimite`) en `GET /api/designaciones/vencimientos` y su lógica en `DesignacionService` y `DesignacionRepository` |
| `solution-diagrams.md` | Diagramas de secuencia, ciclo de vida de estados y arquitectura técnica para la solución |

---

## Field mapping (contract ↔ Core)

### Request: `POST /api/designaciones/{id}/redesignar` (`spec-01`)

| Contract Parameter | Java Type | Domain Field | Validaciones |
| ------------------ | --------- | ------------ | ------------ |
| `{id}` (Path) | `Integer` | `Designacion.designacionId` | Obligatorio. La designación debe existir y estar `"Activa"`. |
| `nroResolucion` | `String` | `Designacion.designacionNumeroResolucion` | Obligatorio (`@NotBlank`). Único por docente. |
| `fechaInicio` | `LocalDate` | `Designacion.designacionFechaInicio` | Obligatorio (`@NotNull`). |
| `fechaFin` | `LocalDate` | `Designacion.designacionFechaFin` | Opcional. Si se provee, `fechaFin >= fechaInicio`. |
| `cargoId` | `Integer` | `Cargo.id` | Opcional. Si no viene, se conserva el cargo asociado previo. Debe pertenecer a la unidad del admin si es `ADMIN_UNIDAD`. |
| `funciones` | `List<FuncionItemRequest>` | `List<Funcion>` | Opcional. Permite registrar las funciones del cargo para la nueva designación. |

### Query Parameters: `GET /api/designaciones/vencimientos` (`spec-02`)

| Contract Parameter | Java Type | Default | Descripción |
| ------------------ | --------- | ------- | ----------- |
| `incluirVencidas` | `Boolean` | `false` | Si incluye registros con `fechaFin < CURRENT_DATE`. |
| `unidadId` | `Integer` | `null` | Filtrado por unidad académica (solo `ADMIN_GLOBAL` o propia de `ADMIN_UNIDAD`). |
| `dias` | `Integer` | `null` | Ventana temporal personalizada de días hacia adelante (`CURRENT_DATE <= fechaFin <= CURRENT_DATE + dias`). |
| `sinLimite` | `Boolean` | `false` | Si es `true`, anula el filtro superior (`fechaHasta = null`), devolviendo todas las designaciones activas. |

---

## Execution Order

1. **`spec-02-flexibilidad-filtro-vencimientos.md`**:
   - Ajustar `DesignacionController.obtenerVencimientos` para recibir `@RequestParam(required = false) Integer dias` y `@RequestParam(required = false, defaultValue = "false") Boolean sinLimite`.
   - Modificar `DesignacionService.obtenerVencimientos` para calcular `fechaHasta` condicionalmente (aprovechando que `DesignacionRepository` ya soporta `fechaHasta = null`) y ajustar el filtrado en memoria de `diasRestantes`.
2. **`spec-01-redesignacion-simple.md`**:
   - Crear DTO `DesignacionRedesignarRequest`.
   - Implementar método `redesignarDocente` en `DesignacionService` aplicando validación de rol/unidad, creación de la nueva `Designacion`, clonación/asignación de cargos y funciones, y cambio de estado de la anterior a `"Renovada"`.
   - Exponer `POST /api/designaciones/{id}/redesignar` en `DesignacionController`.
