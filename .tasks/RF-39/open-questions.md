# Open Questions Document — RF-39

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Alcance de entidades y operaciones auditadas

**Context:** El título de RF-39 menciona *"todo cambio generado sobre la información del sistema"*, y al usar `usuario_afectado_id` (FK a `usuarios`, aprovechando la herencia `JOINED` de `Docente`, `AdminGlobal`, `AdminUnidad` y `Auditor`) podemos auditar cambios sobre cualquier usuario. Actualmente `CambioService.registrarCambio` solo cubre algunas actualizaciones en `DocenteService`, `AdminGlobalService`, `AdminUnidadService`, `AuditorService` y desactivaciones en `UsuarioService`.

**Options:**

- [x] **A)** **Cobertura completa del sistema (CRUD):** Registrar `CREACION`, `ACTUALIZACION` y `ELIMINACION` en todas las entidades principales (`usuarios`, `docentes`, `designaciones`, `cargos`, `funciones`, `licencias`, `renuncias`, `subrogancias`, `unidades`, `materias`, `catedras`).
- [ ] **B)** **Enfoque en usuarios y ciclo de vida docente (MVP):** Registrar `CREACION`, `ACTUALIZACION` y `ELIMINACION` en entidades ligadas a usuarios/docentes (`usuarios`, `docentes`, `designaciones`, `cargos`, `funciones`, `licencias`, `renuncias`, `subrogancias`).
- [ ] **C)** **Solo mantener los servicios ya instrumentados** y agregar únicamente los campos faltantes (`tipoOperacion`, `usuarioAfectadoId`, `unidadId`, `LocalDateTime`).

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01 / spec-02: Visibilidad de cambios para `ADMIN_UNIDAD` y `AUDITOR`

**Context:** Con `usuario_afectado_id` y `unidad_id`, un cambio puede corresponder a un usuario perteneciente a una unidad (docente, admin de unidad, auditor) o a una entidad global (`unidad_id = null`, ej. `AdminGlobal`, `Unidad`). Debe definirse qué ve `ADMIN_UNIDAD` y `AUDITOR`.

**Options:**

- [x] **A)** **Solo usuarios de su propia unidad:** `ADMIN_UNIDAD` y `AUDITOR` ven únicamente registros donde `usuario_afectado_id IS NOT NULL` y `unidad_id = suUnidadId`. Los cambios globales (`unidad_id = null`) solo los ve `ADMIN_GLOBAL`.
- [ ] **B)** **Todo cambio de su unidad:** `ADMIN_UNIDAD` y `AUDITOR` ven cualquier registro con `unidad_id = suUnidadId` (incluyendo catálogos de su unidad como `Materia` o `Cátedra`). Los globales (`unidad_id = null`) solo los ve `ADMIN_GLOBAL`.

- [ ] **Other:** ______

**Notes:** Admin de Unidad solo puede ver de su unidad, pero un auditor puede ver de todos como admin global

---

### B3 — spec-01: Migración del esquema de la tabla `cambios`

**Context:** Actualmente `public.cambios` tiene `cambio_fecha_hora date NOT NULL` (sin hora), `cambio_valor_anterior varchar(100)`, `cambio_valor_nuevo varchar(100)`, y carece de `tipo_operacion`, `usuario_afectado_id` y `unidad_id`. Para cumplir RF-39 es necesario alterar la tabla.

**Options:**

- [x] **A)** **Alterar tabla `cambios` directamente:** Cambiar `cambio_fecha_hora` a `TIMESTAMP`, ampliar `valor_anterior`/`valor_nuevo` (ej. `TEXT` o `VARCHAR(255)`), y agregar `cambio_tipo_operacion VARCHAR(20) NOT NULL DEFAULT 'ACTUALIZACION'`, `usuario_afectado_id INTEGER NULL` (FK a `usuarios`), `unidad_id INTEGER NULL` (FK a `unidades`), `cambio_categoria VARCHAR(50) NULL`. Actualizar `init-atlas-db.sql` y proveer script de migración `ALTER TABLE`.
- [ ] **B)** **Crear una nueva tabla de auditoría** (ej. `historial_cambios`) y deprecar `cambios`.

- [ ] **Other:** ______

**Notes:** Hay algun otro documento específico que tenga referencia a tabla cambios? Para que sea modificado la información correspondiente a lo que suceda en este RF

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Definición del campo "Categoría" en la salida

**Context:** La salida requiere mostrar `Categoría`, pero no se define el catálogo de valores permitidos. Actualmente `Cambio` almacena `tablaAfectada` (ej. `"docentes"`, `"designaciones"`, `"cargos"`, `"funciones"`, `"usuarios"`).

**Options:**

- [x] **A)** **Mapeo legible desde `tablaAfectada`:** Derivar la categoría desde la tabla/entidad afectada (ej. `docentes` → `"Docente"`, `designaciones` → `"Designación"`, `cargos` → `"Cargo"`, `funciones` → `"Función"`, `licencias` → `"Licencia"`, `usuarios` → `"Usuario"`).
- [ ] **B)** **Columna explícita `cambio_categoria`:** Guardar la categoría explícitamente al registrar el cambio con un enum o string normalizado.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-02: Comportamiento cuando no existen registros de cambio

**Context:** La condición dice: *"Debe existir al menos un registro de cambio sobre la información consultada"*. En APIs REST, cuando una búsqueda con filtros no arroja resultados, suele devolverse `200 OK` con lista vacía.

**Options:**

- [x] **A)** **Lista vacía estándar (`200 OK`):** Retornar `200 OK` con `content: []` y `totalElements: 0` para que el frontend muestre el estado vacío ("No se encontraron cambios").
- [ ] **B)** **Error `404 Not Found`:** Retornar `404` cuando no existan registros para los filtros aplicados.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01 / spec-02: Terminología de Tipo de Operación (Filtros vs Salida)

**Context:** En **Condición (filtros)** el requerimiento dice `alta, modificación, baja`, mientras que en **Salida** dice `creación, actualización, eliminación`.

**Options:**

- [ ] **A)** **Unificar en enum `CREACION`, `ACTUALIZACION`, `ELIMINACION`:** El backend usa estos valores canónicos y el frontend muestra las etiquetas correspondientes.
- [x] **B)** **Unificar en enum `ALTA`, `MODIFICACION`, `BAJA`:** Usar terminología administrativa en todo el contrato.

- [ ] **Other:** ______

**Notes:**

---

### M2 — spec-02: Paginación del endpoint `GET /api/cambios`

**Context:** El historial de cambios crece indefinidamente. Devolver todos los registros sin paginación puede degradar el rendimiento.

**Options:**

- [x] **A)** **Paginado (`Page<CambioResponseDTO>`):** Soportar `page` (default `0`), `size` (default `20`), ordenado por `fechaHora DESC, id DESC`.
- [ ] **B)** **Listado completo (`List<CambioResponseDTO>`):** Sin paginación (solo recomendado si el volumen esperado es muy bajo).

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Alcance de entidades y operaciones auditadas | **Opción A:** Cobertura completa CRUD (`ALTA`, `MODIFICACION`, `BAJA`) en todas las entidades principales (`usuarios`, `docentes`, `designaciones`, `cargos`, `funciones`, `licencias`, `renuncias`, `subrogancias`, `unidades`, `materias`, `catedras`). |
| B2 | 01, 02 | Blocker | Visibilidad de cambios para `ADMIN_UNIDAD` y `AUDITOR` | **Opción A + Nota:** `ADMIN_UNIDAD` solo ve cambios de usuarios de su propia unidad (`usuario_afectado_id IS NOT NULL` y `unidad_id = suUnidadId`). `AUDITOR` tiene visibilidad global de todos los cambios, igual que `ADMIN_GLOBAL`. |
| B3 | 01 | Blocker | Migración del esquema de la tabla `cambios` | **Opción A:** Alterar tabla `cambios` directamente (`TIMESTAMP`, ampliar `valor_anterior`/`valor_nuevo`, agregar `cambio_tipo_operacion`, `usuario_afectado_id`, `unidad_id`), actualizar `init-atlas-db.sql` y proveer script `ALTER TABLE`. |
| R1 | 01 | Risk | Definición del campo "Categoría" | **Opción A:** Mapeo legible derivado desde `tablaAfectada` (sin columna extra en BD). |
| R2 | 02 | Risk | Comportamiento cuando no hay resultados | **Opción A:** Retornar `200 OK` con página vacía (`content: []`, `totalElements: 0`). |
| M1 | 01, 02 | Minor | Terminología de `TipoOperacion` | **Opción B:** Unificar en enum `ALTA`, `MODIFICACION`, `BAJA`. |
| M2 | 02 | Minor | Paginación en `GET /api/cambios` | **Opción A:** Paginado (`Page<CambioResponseDTO>`, default `page=0`, `size=20`, ordenado `fechaHora DESC, id DESC`). |
