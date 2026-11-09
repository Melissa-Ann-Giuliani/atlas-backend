# Open Questions Document — RF-23: Baja de una cátedra

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Método y URI del Endpoint HTTP para la baja de cátedra

**Context:** En el proyecto existen dos convenciones aplicadas para operaciones de baja lógica:
1. Verbo de transición de ciclo de vida específico: `PATCH /api/materias/{id}/baja` (RF-26) y `PATCH /api/cargos/{id}/baja` (RF-20).
2. Endpoint estándar REST de eliminación lógica: `DELETE /api/catedras/{id}` o `PATCH /api/catedras/{id}/baja`.

**Options:**

- [x] **A)** `PATCH /api/catedras/{id}/baja`: Endpoint explícito de transición de ciclo de vida, sin payload en el cuerpo de la petición, alineado a `PATCH /api/materias/{id}/baja` y `PATCH /api/cargos/{id}/baja`. Retorna HTTP `200 OK` con `CatedraResponseDTO` con estado `"De baja"`.
- [ ] **B)** `DELETE /api/catedras/{id}`: Endpoint REST estándar para baja lógica, sin body. Retorna HTTP `200 OK` con `CatedraResponseDTO` (o HTTP `204 No Content`).
- [ ] **C)** Soportar ambos: `DELETE /api/catedras/{id}` y `PATCH /api/catedras/{id}/baja` mapeados a la misma operación en el servicio.

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Restricciones de integridad ante elementos asociados a la cátedra

**Context:** Una cátedra puede tener vinculados elementos académicos y docentes (por ejemplo, comisiones activas, inscripciones de cursada o cargos asignados a la cátedra). Debemos definir si se permite dar de baja una cátedra con vínculos activos o si la operación debe bloquearse.

**Options:**

- [ ] **A)** Bloqueo estricto si existen comisiones activas: La cátedra solo puede darse de baja si no posee comisiones en estado activo (`"Activa"` / cursada en curso). Si tiene comisiones activas, se rechaza con HTTP `409 Conflict` (o `400 Bad Request` `"No se puede dar de baja la cátedra porque tiene comisiones activas asociadas"`).
- [ ] **B)** Baja permitida sin bloqueo por comisiones: Se cambia el estado de la cátedra a `"De baja"` preservando los registros existentes para histórico, sin impedir la baja por relaciones existentes.
- [ ] **C)** Cascada lógica / Desactivación: Al dar de baja la cátedra, sus comisiones asociadas activas también pasan automáticamente a estado de baja.

- [x] **Other:** No aplica — no existen entidades dependientes de la cátedra (no hay comisiones en el dominio); la baja no se bloquea por dependencias.

**Notes:** El concepto de "comisiones" no figura en el requerimiento crudo (RF-23), en ningún otro RF, en el esquema de base de datos (`init-atlas-db.sql`) ni en el código; ninguna tabla referencia a `catedras`. Se elimina la validación `409 Conflict` y el Escenario 6. La única condición del requerimiento es que la cátedra exista previamente.

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Autorización y alcance por Unidad Académica para `ADMIN_UNIDAD`

**Context:** El requerimiento crudo indica como actores: *"Administrador Global o Administrador de Unidad"*. En RF-21 (alta), RF-22 (modificación) y RF-26 (baja de materia), los administradores de unidad tienen su ámbito restringido a su unidad académica.

**Options:**

- [x] **A)** Validación estricta por unidad académica: Si el usuario es `ADMIN_UNIDAD`, se valida que la materia de la cátedra pertenezca a la misma unidad académica del administrador. Si pertenece a otra, se rechaza con HTTP `403 Forbidden` (`"No tiene permisos para dar de baja cátedras en materias de otra unidad"`). `ADMIN_GLOBAL` puede dar de baja cátedras de cualquier unidad.
- [ ] **B)** Sin validación de unidad en Backend: Cualquier usuario con rol `ADMIN_GLOBAL` o `ADMIN_UNIDAD` puede dar de baja cualquier cátedra en el sistema, delegando el filtrado a la UI.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Manejo de cátedras que ya se encuentran en estado "De baja"

**Context:** Si un administrador solicita dar de baja una cátedra cuyo estado actual ya es `"De baja"`, debemos definir el comportamiento del backend.

**Options:**

- [x] **A)** Rechazo explícito con `400 Bad Request`: Si `catedra.getEstado()` ya es `"De baja"`, se retorna HTTP `400 Bad Request` (`"La cátedra ya se encuentra dada de baja"`), evitando transiciones redundantes (alineado con RF-26 y RF-20).
- [ ] **B)** Operación idempotente (`200 OK`): Si ya está dada de baja, se responde exitosamente con HTTP `200 OK` y los datos de la cátedra sin producir cambios.

- [ ] **Other:** ______

**Notes:**

---

### R3 — spec-01: Tratamiento del Director de Cátedra al dar de baja

**Context:** La cátedra tiene un docente asignado como director (`directorId`). Al dar de baja la cátedra:

**Options:**

- [x] **A)** Preservar la vinculación histórica: Se mantiene el valor de `directorId` en la entidad cátedra para fines de auditoría y consulta histórica. El docente simplemente deja de dirigir una cátedra activa.
- [ ] **B)** Desvincular director: Se setea `director = null` al momento de la baja.

- [ ] **Other:** ______

**Notes:** Quizás que sea que guarde el último jefe de cátedra al dar de baja 

---

## Minor inconsistencies

---

### M1 — spec-01: Formato de respuesta y mensaje de confirmación

**Context:** El requerimiento crudo indica: *"Queda registrado el cambio en el sistema en estado “De baja”, mostrando un mensaje de confirmación de operación."*

**Options:**

- [x] **A)** Respuesta DTO con mensaje o entidad actualizada: Retornar HTTP `200 OK` con `CatedraResponseDTO` (incluyendo `estado: "De baja"`). El mensaje de confirmación visual es gestionado por el frontend al recibir el código 200.
- [ ] **B)** Respuesta envuelta con mensaje explícito: Retornar HTTP `200 OK` con un payload JSON `{ "mensaje": "Cátedra dada de baja correctamente", "catedra": CatedraResponseDTO }`.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | spec-01 | Blocker | Método y URI del endpoint HTTP | Option A — `PATCH /api/catedras/{id}/baja`, sin body, retorna `200 OK` con `CatedraResponseDTO` |
| B2 | spec-01 | Blocker | Restricciones de integridad ante elementos asociados | Other — No aplica: no existen entidades dependientes de la cátedra; sin validación de dependencias (sin `409`) |
| R1 | spec-01 | Risk | Autorización por unidad académica para ADMIN_UNIDAD | Option A — Validación estricta por unidad (`403 Forbidden` si es de otra unidad) |
| R2 | spec-01 | Risk | Validación de cátedra ya dada de baja | Option A — Rechazo con `400 Bad Request` si ya está `"De baja"` |
| R3 | spec-01 | Risk | Tratamiento del director de cátedra al dar de baja | Option A — Preservar vinculación histórica del director/jefe de cátedra |
| M1 | spec-01 | Minor | Formato de respuesta HTTP | Option A — Retorna HTTP `200 OK` con `CatedraResponseDTO` |

