# RF-39 — specification index: Mantener un historial actualizado de todo cambio generado sobre la información del sistema

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Requerimiento original de historial de cambios del sistema (RF-39) |
| `open-questions.md` | Registro de decisiones acordadas (B1: A/CRUD completo, B2: A+Nota/`ADMIN_UNIDAD` por unidad y `AUDITOR` global, B3: A/Alterar `cambios`, R1: A/Mapeo desde `tablaAfectada`, R2: A/`200 OK` vacío, M1: B/`ALTA`/`MODIFICACION`/`BAJA`, M2: A/Paginado) |
| `spec-01-modelo-registro-cambios.md` | Evolución del modelo `Cambio`, esquema BD (`timestamp`, `tipo_operacion`, `usuario_afectado_id`, `unidad_id`) y ampliación de `CambioService` en todas las entidades principales |
| `spec-02-consulta-historial-cambios.md` | Endpoint `GET /api/cambios` con filtrado combinado, ordenamiento cronológico descendente, paginación y control de acceso por rol/unidad |
| `solution-diagrams.md` | Diagramas de secuencia, clases y arquitectura/flujo de datos para la solución |

---

## Field mapping (contract ↔ Core)

### Query Parameters: `GET /api/cambios` (`spec-02`)

| Contract Parameter | Java Type | Default | Descripción |
| ------------------ | --------- | ------- | ----------- |
| `usuarioAfectado` | `String` | `null` | Búsqueda parcial (case-insensitive) por apellido o nombre del usuario afectado (incluye docentes, admins y auditores por herencia de `Usuario`). |
| `fechaDesde` | `LocalDate` | `null` | Inicio del rango de fechas (inclusive, desde `00:00:00`). |
| `fechaHasta` | `LocalDate` | `null` | Fin del rango de fechas (inclusive, hasta `23:59:59`). |
| `tipoOperacion` | `TipoOperacion` | `null` | Filtro por tipo de cambio (`ALTA`, `MODIFICACION`, `BAJA`). |
| `usuarioId` | `Integer` | `null` | ID del usuario administrador que realizó el cambio. |
| `categoria` | `String` | `null` | Filtro opcional por categoría/módulo afectado (ej. `Docente`, `Designación`, `Cargo`, `Usuario`). |
| `page` / `size` | `Integer` | `0` / `20` | Parámetros de paginación estándar de Spring Data (`Pageable`). |

### Response: `200 OK` (`Page<CambioResponseDTO>` en `spec-02`)

| Contract Field | Java Type | Domain Field | Descripción |
| -------------- | --------- | ------------ | ----------- |
| `id` | `Integer` | `Cambio.id` | Identificador único del registro de cambio. |
| `fechaHora` | `LocalDateTime` | `Cambio.fechaHora` | Fecha y hora exacta del cambio (ISO-8601). |
| `usuarioResponsable` | `UsuarioResumenDTO` | `Cambio.usuario` | ID, username, nombre y apellido del administrador que realizó el cambio. |
| `categoria` | `String` | Derivado de `Cambio.tablaAfectada` | Categoría legible del módulo/entidad afectada (ej. `docentes` → `"Docente"`). |
| `campoAfectado` | `String` | `Cambio.campoAfectado` | Nombre del campo modificado (o resumen en alta/baja). |
| `valorAnterior` | `String` | `Cambio.valorAnterior` | Valor previo al cambio (`null` o `"-"` en alta). |
| `valorNuevo` | `String` | `Cambio.valorNuevo` | Valor posterior al cambio (`null` o `"-"` en baja). |
| `tipoOperacion` | `String` | `Cambio.tipoOperacion` | `ALTA`, `MODIFICACION` o `BAJA`. |
| `usuarioAfectadoId` | `Integer` | `Cambio.usuarioAfectado.id` | ID del usuario afectado (docente, admin, auditor; nullable si es un cambio de catálogo como Unidad/Materia). |
| `usuarioAfectadoNombreCompleto` | `String` | `Cambio.usuarioAfectado` | Apellido y nombre del usuario afectado (si aplica). |
| `unidadId` | `Integer` | `Cambio.unidad.id` | ID de la unidad académica asociada al cambio (nullable si es global). |

---

## Execution Order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-modelo-registro-cambios.md` | Actualizar tabla `cambios`, entidad `Cambio`, enum `TipoOperacion` y `CambioService` + instrumentar servicios mutadores | `open-questions.md` (B1, B2, B3, R1) |
| 2 | `spec-02-consulta-historial-cambios.md` | Implementar `CambioController`, `CambioResponseDTO`, `CambioSpecification` / queries en `CambioRepository` y reglas RBAC por unidad | `spec-01-modelo-registro-cambios.md`, `open-questions.md` (B2, R2, M1) |

---

## Suggested Increments / Backbone

1. **Slice 1 (Walking Skeleton — `spec-01`):** Evolucionar `Cambio` (`LocalDateTime`, `tipoOperacion`, `usuarioAfectado`, `unidad`, `categoria`) y adaptar `CambioService` conservando compatibilidad con las llamadas existentes.
2. **Slice 2 (Consulta y RBAC — `spec-02`):** Exponer `GET /api/cambios` con ordenamiento cronológico descendente, filtrado dinámico por usuario afectado y aislamiento estricto por unidad para `ADMIN_UNIDAD` y `AUDITOR`.
3. **Slice 3 (Cobertura Total — `spec-01` extensión):** Instrumentar operaciones de alta, modificación y baja en el resto de los módulos definidos en `open-questions.md` (B1).
