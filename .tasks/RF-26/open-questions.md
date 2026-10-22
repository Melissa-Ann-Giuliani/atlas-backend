# Open Questions Document — RF-26: Baja de una materia

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Estrategia de baja (Borrado físico vs. Baja lógica)

**Context:** El requerimiento crudo indica: *"El sistema debe ser capaz de generar la baja de una materia"*. En la base de datos actual, la entidad `Materia` no posee campo de estado ni bandera lógica (`activo` o `estado`), y la entidad `Catedra` referencia a `Materia` mediante `@JoinColumn(name = "materia_id", nullable = false)`. Si se realiza un borrado físico (`DELETE` en BD), fallará por violación de clave foránea si la materia tiene cátedras vinculadas o se perderá la trazabilidad académica histórica.

**Options:**

- [ ] **A)** Borrado físico (`DELETE` en BD) condicionado: Solo se permite eliminar físicamente la materia si NO tiene ninguna cátedra vinculada (ni activa ni inactiva/histórica). Si tiene cátedras, se rechaza con `409 Conflict`.
- [x] **B)** Baja lógica mediante incorporación de estado: Se añade una columna/atributo `materia_estado` o `materia_activo` a `Materia` (ej. `"Activa"` / `"Dada de baja"` o `Boolean activo`), preservando el registro histórico e impidiendo la asignación a nuevas cátedras.
- [ ] **C)** Borrado físico en cascada: Se eliminan la materia y todas sus cátedras asociadas (generalmente no recomendado en sistemas académicos por pérdida de historial).

- [ ] **Other:** ______

**Notes:**  Me gusta la idea de añadir una columna para estado de materia, pero entonces deberíamos modificar Alta y Modificación de Materia para acomodar ese nuevo atributo?

---

### B2 — spec-01: Verbo y Ruta del Endpoint REST

**Context:** Debemos acordar la convención RESTful del endpoint para dar de baja la materia en el backend Spring Boot.

**Options:**

- [ ] **A)** `DELETE /api/materias/{id}`: Convención REST estándar para eliminación (retornando `204 No Content` o `200 OK`).
- [x] **B)** `PATCH /api/materias/{id}/baja`: Endpoint explícito para cambio de ciclo de vida (similar a `PATCH /api/cargos/{id}/baja` en RF-20).
- [ ] **C)** `POST /api/materias/{id}/baja`: Acción RPC sobre el recurso.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Validación de impacto con Cátedras asociadas

**Context:** En caso de que se implemente borrado físico o baja lógica, debe definirse qué ocurre cuando la materia posee cátedras asociadas (activas o inactivas). En RF-25 se utilizó `CatedraRepository.existsByMateria_IdAndEstadoIgnoreCase(id, "ACTIVA")`.

**Options:**

- [ ] **A)** Bloqueo estricto ante cualquier cátedra: No se puede dar de baja si tiene cualquier cátedra registrada (activa o inactiva).
- [x] **B)** Bloqueo solo ante cátedras activas: Solo se bloquea si tiene cátedras en estado `"ACTIVA"`. Si todas sus cátedras están inactivas o finalizadas, se permite la baja.
- [ ] **C)** Baja sin restricción de cátedras (si es baja lógica): Se marca la materia como dada de baja sin importar el estado de sus cátedras, deshabilitando la creación de futuras cátedras para esa materia.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Alcance y Autorización de `ADMIN_UNIDAD`

**Context:** El requerimiento crudo indica que el actor es *"Administrador Global o Administrador de Unidad"*. Siguiendo la regla de seguridad establecida en RF-24 y RF-25, un `ADMIN_UNIDAD` no debe poder dar de baja materias de otras unidades académicas.

**Options:**

- [ ] **A)** Validación estricta con HTTP 403 Forbidden: Se valida que la materia pertenezca a la unidad asignada al `ADMIN_UNIDAD`. Si pertenece a otra unidad, se rechaza con `403 Forbidden`. Los `ADMIN_GLOBAL` pueden dar de baja materias de cualquier unidad.
- [ ] **B)** Validación con HTTP 404 Not Found: Si la materia no pertenece a su unidad académica, se responde `404 Not Found` para no filtrar la existencia de recursos entre unidades.

- [x] **Other:** Tal como en Alta y Modificación de Materia, un administrador de unidad se e queda cargado ya su tipo de unidad y unidad, y no tiene acceso a ninguna otra unidad.

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Mensaje y formato de respuesta tras la baja

**Context:** El requerimiento crudo especifica: *"Queda registrado el cambio en el sistema, mostrando un mensaje de confirmación"*. Debemos definir la respuesta del backend para facilitar la confirmación en el frontend.

**Options:**

- [ ] **A)** HTTP `204 No Content` (cuerpo vacío), y el frontend muestra su propio mensaje de confirmación estandarizado.
- [ ] **B)** HTTP `200 OK` con objeto de confirmación `{ "message": "Materia dada de baja exitosamente", "id": 123 }`.
- [ ] **C)** HTTP `200 OK` retornando el detalle de la materia dada de baja (`MateriaDetailDTO`).

- [x] **Other:** Abrodar tal como en RF-25 o RF-24 

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Estrategia de baja (física vs lógica) | Opción B: Baja lógica incorporando atributo/columna `materia_estado` (`"Activa"` / `"Dada de baja"`). En alta nace como `"Activa"`, en modificación el estado solo se altera mediante la baja, y las materias dadas de baja se excluyen de la oferta activa. |
| B2 | 01 | Blocker | Verbo y ruta del endpoint REST | Opción B: Endpoint `PATCH /api/materias/{id}/baja` para transición explícita de ciclo de vida. |
| R1 | 01 | Risk | Validación de impacto ante cátedras asociadas | Opción B: Bloqueo únicamente si la materia posee cátedras en estado `"ACTIVA"`. Si todas sus cátedras están inactivas/finalizadas o no tiene cátedras, se permite la baja. |
| R2 | 01 | Risk | Alcance y autorización para ADMIN_UNIDAD | Other: El Administrador de Unidad solo tiene acceso a su propia unidad académica asignada (dependencia y unidad fija/cargada). En backend se valida que la materia pertenezca a su unidad (rechazando con HTTP 403 si intenta operar fuera). ADMIN_GLOBAL opera sobre cualquier unidad. |
| M1 | 01 | Minor | Formato de respuesta tras la baja | Other: Consistente con RF-24 y RF-25: HTTP 200 OK retornando el detalle actualizado de la materia (`MateriaDetailDTO`, reflejando su estado `"Dada de baja"`). |

---
