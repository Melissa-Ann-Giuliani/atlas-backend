# Open Questions Document — RF-25: Modificar los datos de una materia

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Campos editables vs. inmutables al modificar una materia

**Context:** El requerimiento crudo establece: *"El sistema debe ser capaz de modificar los datos de una materia"*. En RF-24 los atributos persistidos de una materia son: `nombre`, `horas`, `anio`, `despliegue`, `planEstudio` y `unidad` (junto con su dependencia/tipoUnidad). Debemos determinar si todos los campos son modificables o si la unidad académica (`unidad_id`) o el plan de estudio (`plan_estudio`) son inmutables una vez creada la materia, especialmente si ya existen cátedras o cargos vinculados.

**Options:**

- [ ] **A)** Modificación completa excepto unidad académica: Se permite modificar `nombre`, `horas`, `anio`, `despliegue` y `planEstudio`. La unidad académica (`unidad_id`) permanece fija/inmutable (o sólo modificable si no tiene relaciones).
- [ ] **B)** Todos los campos modificables: Se puede transferir la materia a otra unidad académica o modificar cualquier campo (`nombre`, `horas`, `anio`, `despliegue`, `planEstudio`, `unidadId`).
- [x] **C)** Solo datos pedagógicos modificables: Solo se pueden editar `nombre`, `horas`, `anio` y `despliegue`. Ni la `unidad` ni el `planEstudio` pueden modificarse una vez creada la materia.

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Verificación de unicidad al modificar y exclusión del propio ID

**Context:** Al modificar el nombre de una materia (y/o su plan de estudio o unidad), debe verificarse la regla de unicidad definida en RF-24: `(nombre, unidad_id, planEstudio)` case-insensitive. Si no se excluye el ID de la propia materia que se está editando (`id != materiaId`), guardar sin cambiar el nombre generaría un falso conflicto `409 Conflict`.

**Options:**

- [x] **A)** Validación excluyendo el propio ID: Validar que no exista otra materia con el mismo `(nombre, unidad_id, planEstudio)` donde `id != materiaId`. Si se mantiene el mismo nombre o no hay colisión con otra materia, se permite la actualización.
- [ ] **B)** Validación condicional: Solo ejecutar la verificación de unicidad si el `nombre`, `unidadId` o `planEstudio` sufrieron cambios respecto al estado original.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Autorización y alcance para `ADMIN_UNIDAD`

**Context:** El actor definido es *"Administrador Global o Administrador de Unidad"*. Si un `ADMIN_UNIDAD` intenta modificar una materia que pertenece a otra unidad académica distinta a la suya, o intenta reasignar la materia a otra unidad, se debe rechazar para evitar escalamiento de privilegios o edición no autorizada entre departamentos.

**Options:**

- [ ] **A)** Validación estricta en Backend: Se verifica que la materia a editar pertenezca a la unidad asignada al `ADMIN_UNIDAD`. Si pertenece a otra unidad, se devuelve HTTP `403 Forbidden`. Los administradores globales pueden modificar materias de cualquier unidad.
- [ ] **B)** Filtro transparente sin error: Si no pertenece a su unidad, devolver `404 Not Found` en lugar de `403 Forbidden` para no divulgar la existencia de materias en otras unidades.

- [x] **Other:** El administrador de unidad directamente no tendría forma de acceder a otras unidades. Tal como en el alta de materia, su dependencia 8tipo de unidad) estarían bloqueadas en el valor que les pertenece.

**Notes:**

---

### R2 — spec-01: Verificación de integridad con entidades vinculadas (Cátedras, Cargos)

**Context:** Si una materia ya tiene cátedras creadas o docentes asignados a cargos, modificar campos estructurales como `despliegue` (de Anual a Semestral) o `horas` podría impactar en la distribución horaria o cálculos académicos existentes.

**Options:**

- [ ] **A)** Actualización directa: Se permite actualizar sin bloquear por cátedras existentes, confiando en el criterio del administrador académico.
- [x] **B)** Validación de impacto: Si la materia posee cátedras activas asociadas, advertir o restringir cambios críticos en `despliegue` o `horas`.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Método HTTP y diseño del endpoint de modificación

**Context:** Debemos definir el método HTTP y la ruta RESTful estándar para la modificación de materia en el backend Spring Boot.

**Options:**

- [ ] **A)** `PUT /api/materias/{id}` con payload completo `MateriaUpdateRequest`: Reemplazo/actualización completa de los atributos de la materia.
- [x] **B)** `PATCH /api/materias/{id}` con payload parcial: Actualización parcial permitiendo enviar solo los campos que cambiaron.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Campos editables vs inmutables en modificación de materia | Opción C: Solo datos pedagógicos modificables (`nombre`, `horas`, `anio`, `despliegue`). `unidad` y `planEstudio` son inmutables una vez creada la materia. |
| B2 | 01 | Blocker | Validación de unicidad excluyendo propio ID | Opción A: Validar unicidad de `(nombre, unidad_id, planEstudio)` excluyendo el ID actual (`id != materiaId`). |
| R1 | 01 | Risk | Control de acceso y scoping para ADMIN_UNIDAD | UI: Dependencia y unidad fijas/bloqueadas en su unidad asignada. Backend: Validación estricta con HTTP 403 Forbidden si intenta modificar una materia perteneciente a otra unidad. |
| R2 | 01 | Risk | Integridad ante entidades vinculadas (Cátedras) | Opción B: Validación de impacto: Si la materia posee cátedras activas asociadas, restringir cambios críticos en `despliegue` o `horas` (rechazando con HTTP 409 Conflict o 400 Bad Request si intenta alterar despliegue u horas teniendo cátedras activas). |
| M1 | 01 | Minor | Método HTTP y diseño del endpoint (PUT vs PATCH) | Opción B: Endpoint `PATCH /api/materias/{id}` con payload parcial `MateriaPatchRequest` permitiendo enviar únicamente los campos modificados. |
