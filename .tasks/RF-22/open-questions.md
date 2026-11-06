# Open Questions Document — RF-22: Modificar los datos de una cátedra

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Campos editables vs. inmutables al modificar una cátedra

**Context:** El requerimiento crudo establece: *"El sistema debe ser capaz de modificar los datos de una cátedra"*. En `Catedra` (RF-21) los atributos registrados son `nombre`, `tipologia`, `director` (`directorId`), `materia` (`materiaId`) y `estado`. Debemos definir si la materia a la que pertenece la cátedra puede cambiarse o si permanece fija una vez creada la cátedra.

**Options:**

- [x] **A)** Solo datos propios de la cátedra modificables: Se permite editar `nombre`, `tipologia` (1 a 4) y `directorId`. Tanto la `materia` (`materiaId`) como el `estado` permanecen inmutables en este endpoint.
- [ ] **B)** Todos los campos del alta modificables: Se permite editar `nombre`, `tipologia`, `directorId` y `materiaId` (validando permisos sobre la nueva materia y unicidad en ella). El `estado` permanece inmutable.
- [ ] **C)** Modificación total incluyendo `estado`: Se permite editar `nombre`, `tipologia`, `directorId`, `materiaId` y `estado` (`"Activa"` / `"De Baja"`).

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Verificación de unicidad de nombre al modificar (exclusión del propio ID)

**Context:** Según RF-21, no pueden existir dos cátedras con el mismo nombre (ignorando mayúsculas/minúsculas) dentro de la misma materia. Si el administrador modifica la tipología o el director manteniendo el mismo nombre de cátedra, una validación simple `existsByNombreIgnoreCaseAndMateria_Id` fallaría contra sí misma.

**Options:**

- [x] **A)** Excluir el propio ID en la consulta: Validar mediante `existsByNombreIgnoreCaseAndMateria_IdAndIdNot(nombre, materiaId, catedraId)`, rechazando con HTTP `400 Bad Request` (o `409 Conflict`) solo si existe *otra* cátedra con ese nombre en la materia.
- [ ] **B)** Validación condicional: Ejecutar la consulta de existencia únicamente cuando el `nombre` (o `materiaId`) enviado sea distinto al actual.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Autorización y alcance por Unidad Académica para `ADMIN_UNIDAD`

**Context:** El requerimiento define como actor a *"Administrador Global o Administrador de Unidad"*. Siguiendo el criterio de RF-21, debemos confirmar si un `ADMIN_UNIDAD` solo tiene permiso para modificar cátedras cuyas materias pertenezcan a su propia unidad académica asignada.

**Options:**

- [x] **A)** Validación estricta por unidad en Backend: Si el usuario autenticado es `ADMIN_UNIDAD`, la materia de la cátedra debe pertenecer a su unidad académica asignada; de lo contrario se rechaza con HTTP `403 Forbidden`. `ADMIN_GLOBAL` puede modificar cátedras de cualquier unidad.
- [ ] **B)** Sin restricción de unidad en Backend: Cualquier administrador (`ADMIN_GLOBAL` o `ADMIN_UNIDAD`) puede modificar cualquier cátedra existente, delegando el filtrado a la interfaz de usuario.

- [ ] **Other:** ______

**Notes:** Pensaba que fuera como en otras partes del proyecto, que un Admin de Unidad solo va a tener acceso a cátedras de su propia unidad

---

### R2 — spec-01: Condiciones de estado de la Cátedra y del Docente Director

**Context:** Al modificar una cátedra existente o asignarle un nuevo docente como director, debemos definir qué estados son válidos para permitir la operación y evitar asignaciones inconsistentes.

**Options:**

- [x] **A)** Cátedra activa y Docente activo: Solo se permite modificar cátedras que se encuentren en estado `"Activa"` (rechazando con `400 Bad Request` si está `"De Baja"`), y el docente director asignado debe existir (`404 Not Found` si no existe) y estar en estado `"Activo"` (`400 Bad Request` si está inactivo).
- [ ] **B)** Docente activo sin restricción de estado en Cátedra: Se permite modificar los datos de la cátedra independientemente de su estado actual, pero el docente director debe existir y estar en estado `"Activo"`.
- [ ] **C)** Solo validar existencia por ID (`404 Not Found` si la cátedra o el docente no existen), sin validar el campo `estado`.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Método HTTP y estructura del payload (`PUT` vs `PATCH`)

**Context:** En el backend existen endpoints de modificación implementados con `PUT` (ej. `PUT /api/cargos/{id}` en RF-18) y otros con `PATCH` (ej. `PATCH /api/materias/{id}` en RF-25). Debemos definir la convención para el endpoint de modificación de cátedra.

**Options:**

- [x] **A)** `PUT /api/catedras/{id}` con actualización completa de los campos editables (reutilizando o definiendo un DTO con `@NotBlank` / `@NotNull`), retornando HTTP `200 OK` con `CatedraResponseDTO`.
- [ ] **B)** `PATCH /api/catedras/{id}` con actualización parcial (`CatedraPatchRequestDTO`), permitiendo enviar únicamente los campos que se desean modificar y retornando HTTP `200 OK` con `CatedraResponseDTO`.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Campos editables vs inmutables en modificación de cátedra | Opción A: Solo datos propios de la cátedra modificables (`nombre`, `tipologia` de 1 a 4, `directorId`). La `materia` y el `estado` permanecen inmutables. |
| B2 | 01 | Blocker | Validación de unicidad de nombre excluyendo propio ID | Opción A: Excluir el propio ID mediante `existsByNombreIgnoreCaseAndMateria_IdAndIdNot(nombre, materiaId, catedraId)`, retornando `400 Bad Request` si existe otra cátedra con igual nombre en la materia. |
| R1 | 01 | Risk | Control de autorización por unidad para ADMIN_UNIDAD | Opción A: Validación estricta por unidad en Backend (`403 Forbidden` si la materia de la cátedra no pertenece a la unidad del `ADMIN_UNIDAD`). `ADMIN_GLOBAL` sin restricción de unidad. |
| R2 | 01 | Risk | Validación de estado activo en cátedra y docente director | Opción A: Solo se permite modificar cátedras en estado `"Activa"` (`400 Bad Request` si está `"De Baja"`), y el docente director asignado debe existir (`404 Not Found`) y estar en estado `"Activo"` (`400 Bad Request`). |
| M1 | 01 | Minor | Método HTTP y estructura del payload (PUT vs PATCH) | Opción A: `PUT /api/catedras/{id}` recibiendo `CatedraUpdateRequestDTO` (`nombre`, `tipologia`, `directorId`) y retornando `200 OK` con `CatedraResponseDTO`. |

