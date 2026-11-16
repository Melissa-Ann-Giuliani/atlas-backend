# RF-11 — specification index: Asignar funciones a un cargo perteneciente a una designación

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Requerimiento original para asignar funciones a un cargo perteneciente a una designación (RF-11) |
| `open-questions.md` | Registro de decisiones acordadas (B1: Opción C, B2: Opción A, R1: Opción A, R2: Opción A, R3: Opción A) |
| `spec-01-asignar-funciones-cargo.md` | Especificación formal en Gherkin para la asignación de funciones, validación de horas, cálculo de horas ocupadas, generación de documento, activación del docente y notificación |
| `solution-diagrams.md` | Diagramas de secuencia y flujo de estados del proceso de asignación y cierre de designación |

## Field mapping (contract ↔ Core)

### Database Schema Updates
- **Tabla `funciones`**:
  - Se añade la columna `funcion_tipo VARCHAR(50) NOT NULL` (Decisión B1 - Opción C), permitiendo registrar el tipo de función docente: `"Planta"`, `"Investigación"`, `"Gestión"`, `"Extensión"`, etc.

### Request: `POST /api/cargos/{cargoId}/funciones` (`AsignarFuncionesCargoRequest`)

| Contract Field | Java Field / Type | Domain Field | Validaciones |
| -------------- | ----------------- | ------------ | ------------ |
| `cargoId` (Path) | `Integer cargoId` | `Cargo.id` | Obligatorio. Debe existir en `cargos` (`404 Not Found`), estar en estado `"Asignado"` (`400 Bad Request`) y tener una designación vinculada. |
| `funciones` (Body) | `List<FuncionItemRequest>` | `List<Funcion>` | Lista obligatoria no vacía (`@NotEmpty`). |
| `funciones[].nombre` | `String nombre` | `Funcion.nombre` | Obligatorio (`@NotBlank`, max 100). |
| `funciones[].tipo` | `String tipo` | `Funcion.tipo` | Obligatorio (`@NotBlank`, max 50). Categoría de actividad (Decisión B1). |
| `funciones[].horas` | `Integer horas` | `Funcion.horas` | Obligatorio (`@NotNull`, min 1). $\sum \text{horas} \le \text{cargo.dedicacion.horas}$ (Decisión B2). |
| `funciones[].materiaId` | `Integer materiaId` | `Funcion.materia.id` | Obligatorio (`@NotNull`). Materia existente y perteneciente a la misma unidad del cargo (Decisión R3). |

### Response: `200 OK` (`DesignacionCompletaResponseDTO`)

| Contract Field | Domain Field | Descripción |
| -------------- | ------------ | ----------- |
| `cargoId` | `Cargo.id` | ID del cargo asignado |
| `horasDedicacion` | `Cargo.dedicacion.dedicacionHoras` | Total de horas de dedicación asignadas al cargo |
| `horasOcupadas` | `Cargo.horasOcupadas` | Sumatoria actualizada de horas de las funciones asignadas |
| `funciones` | `List<FuncionDTO>` | Listado de funciones registradas (`id`, `nombre`, `tipo`, `horas`, `materia`) |
| `designacionId` | `Designacion.designacionId` | Identificador de la designación vinculada |
| `documentoId` | `Archivo.archivoId` | ID del PDF oficial generado y almacenado (Decisión R1) |
| `docenteEstado` | `Docente.usuario.activo` | `"Activo"` (Decisión R2) |
| `notificacionEnviada` | `Boolean` | `true` si el email fue despachado al docente (Decisión R2) |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-asignar-funciones-cargo.md` | Endpoint `POST /api/cargos/{cargoId}/funciones`: validación del cargo en estado `"Asignado"` con designación, control de unidad para `ADMIN_UNIDAD` (`403`), validación de pertenencia de materias a la misma unidad (`400`), control de tope $\sum \text{horas} \le \text{dedicación}$ (`400`), persistencia con `funcion_tipo`, actualización de `cargo_horas_ocupadas`, generación de PDF en `archivos`, activación del docente a `"Activo"` y envío de correo vía `EmailService`. | RF-09 (Designación creada), RF-10 (Cargo vinculado a la designación) |

## Suggested increments / Backbone

1. **Database & Entity Evolution**:
   - Agregar columna `funcion_tipo VARCHAR(50)` en `init-atlas-db.sql` y campo `private String tipo` en `Funcion.java`.
2. **Core Service & Logic (`FuncionService`)**:
   - Implementar método transaccional `@Transactional asignarFuncionesACargo(...)`:
     - Verificar existencia y estado del `Cargo` y vinculación con `Designacion`.
     - Validar alcance de unidad (`ADMIN_UNIDAD` vs. `cargo.unidad`).
     - Validar que cada `Materia` pertenezca a la misma unidad del cargo.
     - Validar tope $\sum \text{horas} \le \text{cargo.dedicacion.horas}$.
     - Persistir entidades `Funcion` y actualizar `cargo.setHorasOcupadas(...)`.
3. **Document Generation & Archivo Persistence**:
   - Generar documento PDF con el resumen de la designación/cargo/funciones y almacenarlo en `ArchivoService` (`tipoTramite = "DESIGNACION"`, `tramiteId = designacion.id`).
4. **Docente Activation & Email Notification**:
   - Activar el docente (`docente.getUsuario().setActivo(true)`).
   - Enviar correo al docente con la confirmación de la designación completa mediante `EmailService`.
