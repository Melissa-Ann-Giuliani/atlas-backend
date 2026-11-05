# Open Questions Document — RF-21: Alta de una cátedra

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Definición de Tipología de Cátedra y Valores Permitidos

**Context:** El requerimiento indica que se ingresa `Tipología` y en la base de datos `Catedra.tipologia` es un campo `Integer NOT NULL` (`catedra_tipologia integer NOT NULL`). Si no definimos qué representa y qué valores o rango son válidos (o si existe una tabla de referencia / enum), la validación de entrada fallará o admitirá valores inconsistentes.

**Options:**

- [x] **A)** Tipologías estándar numéricas (ej. `1` = Simple/Teórica, `2` = Teórico-Práctica, `3` = Taller/Laboratorio) validadas como entero positivo (> 0 o dentro de 1..5).
- [ ] **B)** Solo valores numéricos entre `1` y `4` según ordenanza académica estándar.
- [ ] **C)** Un entero libre `>= 1` enviado desde el frontend sin restricción adicional en backend.

- [ ] **Other:** ______

**Notes:** It goes from 1 to 4, but in roman numbers.

---

### B2 — spec-01: Restricción de Unicidad de Cátedra y Condición de Existencia Previa

**Context:** El requerimiento estipula como condición: *"La cátedra no debe existir previamente"*. Además, en BD `Catedra` tiene `catedra_nombre` y `materia_id`. Debemos definir qué criterio determina que una cátedra "ya existe".

**Options:**

- [x] **A)** Unicidad por `materiaId` + `nombre` (no pueden existir dos cátedras con el mismo nombre dentro de la misma materia, ignorando mayúsculas/minúsculas).
- [ ] **B)** Unicidad global por `nombre` (no puede existir ninguna otra cátedra en el sistema con el mismo nombre).
- [ ] **C)** Unicidad de cátedra por `materiaId`: sólo puede haber una cátedra por materia (o cátedra única por materia + nombre).

- [ ] **Other:** ______

**Notes:**

---

### B3 — spec-01: Exclusividad del Director de Cátedra (Restricción UNIQUE en BD)

**Context:** En la tabla `catedras`, la columna `catedra_director_id` tiene restricción de unicidad estricta a nivel base de datos (`UNIQUE (catedra_director_id)` en PostgreSQL y `@OneToOne ... unique = true` en JPA). Esto significa que un docente sólo puede ser director de una única cátedra a la vez. Si el docente seleccionado ya dirige otra cátedra activa o existente, la BD lanzará error.

**Options:**

- [ ] **A)** Validar previamente en el servicio que el docente no figure como director de ninguna otra cátedra activa (`existsByDirectorIdAndEstado(...)` o `existsByDirectorId(...)`), retornando HTTP `400 Bad Request` ("El docente seleccionado ya es director de otra cátedra").
- [ ] **B)** Mantener la regla estricta: un docente sólo puede dirigir una única cátedra en todo el sistema (`existsByDirectorId(...)`). Si ya tiene una cátedra asignada (incluso de baja), se rechaza o debe desvincularse.

- [x] **Other:** We might have to modify, because sometimes there might be a docente that is jefe de cátedra in more than one cátedra

**Notes:**

---

### B4 — spec-01: Verbo y Ruta del Endpoint REST

**Context:** Debe definirse la URL y convención REST para el alta de cátedra.

**Options:**

- [x] **A)** `POST /api/catedras` enviando payload JSON con `nombre`, `tipologia`, `directorId`, `materiaId`, retornando HTTP `201 Created` con el recurso creado.
- [ ] **B)** `POST /api/materias/{materiaId}/catedras` como subrecurso de materia.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Alcance de Roles y Autorización (Admin Global vs Admin Unidad)

**Context:** El requerimiento indica: *"Actor: Administrador Global o Administrador de Unidad"*. Si un `Admin de Unidad` crea una cátedra, ¿debe validarse que la `Materia` pertenezca a su propia Unidad Académica asignada? Si no se valida, un admin de unidad podría crear cátedras en materias de otra facultad/unidad.

**Options:**

- [x] **A)** Validar pertenencia: Si el usuario es `ADMIN_UNIDAD`, la materia asociada (`materia.unidad.id`) debe coincidir con la unidad del administrador. Si es `ADMIN_GLOBAL`, puede crear cátedras para cualquier materia de cualquier unidad. Si no coincide, retornar HTTP `403 Forbidden`.
- [ ] **B)** Ambos roles pueden operar sin validación de unidad a nivel backend (se delega el filtrado de opciones a la UI).

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Requisitos de Estado del Docente y de la Materia

**Context:** Al asignar un `director` (Docente) y una `materia`, ¿qué condiciones deben cumplir estos registros relacionados?

**Options:**

- [x] **A)** El docente debe existir y estar en estado `"Activo"`. La materia debe existir en la base de datos.
- [ ] **B)** El docente debe existir (cualquier estado excepto dado de baja) y la materia debe existir.
- [ ] **C)** Solo verificar existencia por ID (`404 Not Found` si no existe el docente o la materia).

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Formato del Estado ("Activo" vs "Activa")

**Context:** El requerimiento menciona: *"El campo de Estado se carga automáticamente como 'Activo' (sus dos posibles estados son 'Activo' o 'de Baja')"* y en la salida dice *"quedando en estado 'Activa'"*. En el modelo Java y BD (`catedra_estado`), el campo es `String`.

**Options:**

- [x] **A)** Usar `"Activa"` / `"De Baja"` (en concordancia gramatical femenina con "cátedra").
- [ ] **B)** Usar `"Activo"` / `"De Baja"` (siguiendo el estándar de otros modelos como Cargo/Docente).

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Definición y validación de tipología de cátedra | Valores enteros de 1 a 4 (`@Min(1)`, `@Max(4)`), representando tipologías I a IV en números romanos. |
| B2 | 01 | Blocker | Regla de unicidad y existencia previa de cátedra | Unicidad por combinación de `materiaId` + `nombre` (case-insensitive). |
| B3 | 01 | Blocker | Validación previa de exclusividad de director de cátedra | Se permite que un docente dirija más de una cátedra. Se elimina la restricción de exclusividad (relación `@ManyToOne` en BD/JPA, no se valida `existsByDirectorId`). |
| B4 | 01 | Blocker | Verbo y ruta del endpoint REST para alta de cátedra | `POST /api/catedras` con payload JSON (`CatedraRequestDTO`) retornando `201 Created` (`CatedraResponseDTO`). |
| R1 | 01 | Risk | Control de autorización por unidad para ADMIN_UNIDAD | Control por unidad: `ADMIN_UNIDAD` solo puede crear cátedras en materias de su propia unidad académica (`403 Forbidden` si no coincide); `ADMIN_GLOBAL` sin restricción. |
| R2 | 01 | Risk | Validaciones de estado activo de director y materia | Docente debe existir y estar en estado `"Activo"`; Materia debe existir (`404 Not Found` si no existen, `400 Bad Request` si docente inactivo). |
| M1 | 01 | Minor | Consistencia de valor en estado ("Activo" vs "Activa") | Usar `"Activa"` como estado inicial al persistir la cátedra (y `"De Baja"` para baja). |
