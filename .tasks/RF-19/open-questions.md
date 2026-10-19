# Open Questions Document — RF-19: Subdividir un cargo en 2 o más cargos

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Estado del cargo padre luego de la subdivisión

**Context:** El requerimiento dice: *"Queda registrado en el sistema la división del cargo: se generan nuevos sobrecargos indicados, cada uno indicando su cargo origen, manteniendo sus datos históricos en el sistema."* Sin embargo, no se explicita qué ocurre con el `cargo_estado` del cargo padre una vez que se efectúa la división. Si permanece "Libre" o "Activo", podría volver a asignarse o subdividirse indebidamente, duplicando horas/presupuesto. En los datos iniciales de la base (`init-atlas-db.sql`), el cargo origen (8001) tiene estado `'Licencia'`.

**Options:**

- [ ] **A)** Cambiar el estado del cargo padre a `"Subdividido"` (o `"Inactivo"`), impidiendo nuevas asignaciones de docentes o nuevas subdivisiones sobre el mismo.
- [x] **B)** Mantener el estado actual del cargo padre (por ejemplo, si estaba en "Licencia" o "Libre") pero marcar sus `horas_ocupadas` al 100% (o bloquear su asignación directa).
- [ ] **C)** Permitir parametrizar o mantener el estado del cargo padre si la suma de horas de los subcargos no consume la totalidad de las horas del padre (ej. subdivisión parcial dejando horas remanentes).

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Validación de horas y dedicación (Suma vs Padre)

**Context:** La condición del requerimiento establece: *"La suma de horas/dedicación de los subcargos generados no puede superar la del cargo original"*. Cada `Dedicacion` tiene `dedicacion_horas` (ej. Simple = 10hs, Semiexclusiva = 20hs, Exclusiva = 40hs). Si la suma de las horas de los subcargos es menor que las del padre (ej. padre de 40hs dividido en dos subcargos de 10hs = 20hs en total), o si debe ser exactamente igual, impacta en la validación de negocio.

**Options:**

- [x] **A)** Suma menor o igual: La suma de horas de los subcargos debe ser menor o igual a las horas de dedicación del cargo padre (`sum(subcargos.horas) <= padre.horas`).
- [ ] **B)** Suma estrictamente exacta: La suma de horas de los subcargos debe ser exactamente igual a las horas del cargo padre (`sum(subcargos.horas) == padre.horas`).

- [ ] **Other:** ______

**Notes:**

---

### B3 — spec-01: Contrato del Endpoint REST (Ruta y Semántica HTTP)

**Context:** La acción consiste en tomar un cargo padre existente y crear $N \ge 2$ nuevos subcargos asociados a él en una transacción atómica. Debemos definir la ruta y verbo HTTP estándar para esta operación.

**Options:**

- [ ] **A)** `POST /api/cargos/{id}/subdividir` recibiendo en el cuerpo una lista de subcargos a generar (`List<SubcargoCreateRequest>`), devolviendo la lista de cargos creados o el detalle del padre con sus subcargos.
- [x] **B)** `POST /api/cargos/subdividir` recibiendo en el cuerpo el identificador del cargo padre (`cargoOrigenId` o `cargoOrigenCodigo`) y la lista de subcargos.
- [ ] **C)** Reutilizar `POST /api/cargos` permitiendo enviar un payload múltiple con el campo `cargoOrigenDivisionId`.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Restricción de estado previo del cargo padre para permitir la subdivisión

**Context:** ¿Puede subdividirse un cargo padre si actualmente tiene un docente designado y en ejercicio activo (`horas_ocupadas > 0`, estado `"Asignado"` o `"Ocupado"`), o debe estar en `"Libre"` / `"Licencia"`? En la práctica académica real, a menudo se subdivide un cargo cuando el titular se toma licencia o cuando el cargo está vacante ("Libre").

**Options:**

- [x] **A)** Permitir subdivisión únicamente si el cargo padre está en estado `"Libre"` o `"Licencia"`. Si está asignado y activo sin licencia, rechazar con HTTP 400 Bad Request / 409 Conflict.
- [ ] **B)** Permitir subdivisión en cualquier estado, desvinculando o manteniendo la designación previa según la normativa.
- [ ] **C)** Permitir subdivisión solo si el cargo padre está `"Libre"` (sin ningún docente ni designación asociada).

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Unidad académica de los subcargos vs cargo padre

**Context:** Un `ADMIN_UNIDAD` solo tiene permisos sobre su unidad académica asignada. En el requerimiento dice: *"Por cada subcargo, se completan los datos pertinentes al cargo: ... unidad, ..."*. ¿Pueden los subcargos pertenecer a una unidad distinta a la del cargo padre, o deben heredar/mantener forzosamente la misma unidad?

**Options:**

- [ ] **A)** Los subcargos deben pertenecer obligatoriamente a la misma unidad académica del cargo padre (`subcargo.unidadId == padre.unidad.id`). Un `ADMIN_UNIDAD` solo puede subdividir cargos de su propia unidad.
- [x] **B)** `ADMIN_GLOBAL` puede asignar los subcargos a diferentes unidades, pero un `ADMIN_UNIDAD` solo puede crearlos en su propia unidad asignada.

- [ ] **Other:** ______

**Notes:**

---

### R3 — spec-01: Funciones y materias del cargo padre

**Context:** La base de datos tiene una tabla `funciones` asociada a `cargos` (`cargo_id`, `materia_id`, `funcion_horas`). En `init-atlas-db.sql`, cuando se crearon los cargos 8002 y 8003 como división de 8001, se crearon nuevas funciones de 10hs para la misma materia. ¿El endpoint de subdivisión de cargo crea/asigna funciones o las funciones se gestionan en otro módulo/endpoint posterior?

**Options:**

- [x] **A)** El endpoint solo crea los cargos (`cargos`) con su respectiva información base y vinculación `cargo_origen_division`. Las funciones académicas se definen posteriormente en la gestión de materias/funciones.
- [ ] **B)** Cada subcargo en el payload puede incluir opcionalmente una lista de funciones a insertar en la misma transacción.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Cantidad mínima y máxima de subcargos

**Context:** El requerimiento estipula *"subdividir un cargo en 2 o más cargos"*. Debe validarse que la lista contenga al menos 2 subcargos. ¿Existe una cota máxima técnica recomendada o límite de subcargos por operación?

**Options:**

- [ ] **A)** Validar mínimo 2 subcargos (`size >= 2`). Sin límite superior explícito más allá del límite impuesto por las horas del cargo padre y validaciones estándar de payload.
- [x] **B)** Validar mínimo 2 y máximo configurable (ej. 10 subcargos).

- [ ] **Other:** ______

**Notes:**

---

### M2 — spec-01: Predecesor de los subcargos generados

**Context:** El requerimiento dice: *"Por cada subcargo, se completan los datos pertinentes al cargo: ... número de predecesor posible y número de origen de división, colocando allí el código del cargo padre"*. ¿El cargo padre actúa automáticamente como predecesor si no se especifica uno, o son dos campos independientes (`cargo_predecesor_id` vs `cargo_origen_division`)?

**Options:**

- [x] **A)** Son independientes: `cargo_origen_division` apunta automáticamente al cargo padre subdividido. `cargo_predecesor_id` es opcional y solo se asigna si el usuario lo indica explícitamente en el subcargo.
- [ ] **B)** El cargo padre se asigna tanto en `cargo_origen_division` como en `cargo_predecesor_id` a menos que se indique otro predecesor.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec  | Impact  | Short description                  | Decision |
|----|-------|---------|------------------------------------|----------|
| B1 | 01    | Blocker | Estado del cargo padre post-división | Option B: Mantener el estado actual del cargo padre (ej. "Licencia" o "Libre") pero marcar sus `horasOcupadas` al 100% (igual a sus horas de dedicación) para bloquear asignaciones directas. |
| B2 | 01    | Blocker | Validación horas subcargos (<= vs ==) | Option A: Suma menor o igual (`sum(subcargos.horas) <= padre.horas`). No requiere consumir exactamente el 100% de las horas. |
| B3 | 01    | Blocker | Contrato y endpoint REST de subdivisión | Option B: `POST /api/cargos/subdividir` recibiendo en el cuerpo el identificador del padre (`cargoOrigenCodigo` o `cargoOrigenId`) y la lista de subcargos. |
| R1 | 01    | Risk    | Estado previo requerido en cargo padre | Option A: Permitir subdivisión únicamente si el cargo padre está en estado `"Libre"` o `"Licencia"`. Si está `"Asignado"` u `"Ocupado"` sin licencia, rechazar con 400 Bad Request / 409 Conflict. |
| R2 | 01    | Risk    | Unidad de subcargos vs unidad del padre | Option B: `ADMIN_GLOBAL` puede asignar subcargos a diferentes unidades; `ADMIN_UNIDAD` solo puede crearlos en su propia unidad asignada. |
| R3 | 01    | Risk    | Manejo de funciones académicas en la división | Option A: El endpoint solo crea los cargos (`cargos`) con su respectiva información base y vinculación `cargo_origen_division`. Las funciones se gestionan de forma independiente. |
| M1 | 01    | Minor   | Validación de cantidad mín/máx subcargos | Option B: Validar mínimo 2 y máximo configurable (10 subcargos). |
| M2 | 01    | Minor   | Asignación de predecesor vs origen división | Option A: Son independientes: `cargoOrigenDivision` apunta al padre; `cargoPredecesor` es opcional y solo se asigna si se indica explícitamente en el subcargo. |
