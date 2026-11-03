# Open Questions Document — RF-24: Alta de una materia

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Modelado y persistencia de "Plan Educativo"

**Context:** El requerimiento crudo solicita registrar `N° de Plan de Estudio` (anteriormente "Plan Educativo") al dar de alta una materia. Sin embargo, en el backend actual la entidad `Materia` (`src/main/java/com/atlas/atlas_backend/materia/Materia.java`) solo tiene: `id`, `nombre`, `horas`, `anio`, `despliegue` y `unidad`. No existe actualmente la entidad ni columna para `planEstudio`. Debemos definir cómo modelar y persistir este dato.

**Options:**

- [ ] **A)** Campo de texto en `Materia`: Agregar `planEducativo` (`String`, ej. length=50 o 100) en la tabla `materias` (`materia_plan_educativo`).
- [ ] **B)** Entidad relacional `PlanEducativo`: Crear la tabla/entidad `PlanEducativo` (`id`, `nombre`, `unidad_id`) y relacionar `Materia` mediante `@ManyToOne PlanEducativo planEducativo`.
- [ ] **C)** Desestimar en la entidad por ahora: Omitir `planEducativo` en `Materia` si pertenece a una futura funcionalidad de planes de estudio o carreras.

- [x] **Other:** Cambiar "plan educativo" a "plan de estudio", esto será un campo que guardará un año cualquiera (ej. Integer o String representando el año del plan, como 2023).

**Notes:**

---

### B2 — spec-01: Criterio de unicidad para "La materia no debe existir previamente"

**Context:** La condición del requerimiento indica: *"La materia no debe existir previamente"*. Se debe especificar a nivel de base de datos y validación de negocio cómo se determina que una materia ya existe (duplicada).

**Options:**

- [ ] **A)** Unicidad por `(nombre, unidad_id)`: No pueden existir dos materias con el mismo nombre dentro de la misma Unidad Académica (case-insensitive).
- [x] **B)** Unicidad por `(nombre, unidad_id, planEstudio)`: El nombre puede repetirse entre distintos planes de estudio de la misma unidad, pero no dentro del mismo plan y unidad.
- [ ] **C)** Unicidad global por `nombre`: No pueden existir dos materias con el mismo nombre en todo el sistema.

- [ ] **Other:** ______

**Notes:**

---

### B3 — spec-01: Formato de "Año/s Educativo"

**Context:** El requerimiento menciona: *"Año/s Educativo"*. En la entidad `Materia`, el atributo actual es `private Integer anio;`. Debemos confirmar cómo se modela y persiste el año educativo de la carrera.

**Options:**

- [ ] **A)** Entero simple `Integer anio`: Representa el año de la carrera al que pertenece la materia (ej. 1, 2, 3, 4, 5).
- [ ] **B)** Texto `String anio`: Permite valores como "1°", "1 y 2", "Anual", etc.
- [ ] **C)** Lista de años / relación múltiple `List<Integer>`: Permite seleccionar múltiples años educativos.

- [x] **Other:** Representaría el año de la carrera a la cual pertenece la materia, pero se guardaría como 1°, 2°, 3°, 4°, 5° (String de formato ordinal).

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Alcance de autorización para `ADMIN_UNIDAD` y selectores Dependencia/Unidad

**Context:** El actor definido es *"Administrador Global o Administrador de Unidad"*. El usuario `ADMIN_UNIDAD` tiene asignada una unidad académica (`Unidad`) que a su vez se asocia a su tipo/dependencia (`TipoUnidad`). En la pantalla, se debe seleccionar Dependencia y Unidad.

**Options:**

- [ ] **A)** Restricción estricta backend: Si un `ADMIN_UNIDAD` intenta dar de alta una materia enviando un `unidadId` diferente a su unidad asignada, se rechaza con HTTP `403 Forbidden`.
- [ ] **B)** Sobrescritura automática backend: Para `ADMIN_UNIDAD`, se ignora el `unidadId` recibido y se asocia automáticamente a su unidad asignada.
- [ ] **C)** Sin restricción de unidad: Tanto `ADMIN_GLOBAL` como `ADMIN_UNIDAD` pueden crear materias para cualquier unidad.

- [x] **Other:** Como en la pantalla donde el usuario va a poder ingresar una nueva materia, tiene un selector de Dependencia, para un admin de unidad el selector Dependencia estará fijo en su dependencia correspondiente (ej. si es del Departamento de Matemática, en Dependencia tiene fijo Departamento sin modificarlo). Además, el selector de Unidad tendrá fija su unidad a la que pertenece (Matemática, sin modificarlo). En el backend, se valida estrictamente que si es `ADMIN_UNIDAD`, la unidad coincida con su unidad asignada (HTTP 403 en caso contrario).

**Notes:**

---

### R2 — spec-01: Valores permitidos y validación de "Despliegue"

**Context:** El requerimiento pide el campo `Despliegue` (en la entidad mapeado a `materia_despliegue`). Necesitamos definir los valores permitidos y la lógica condicional de semestres.

**Options:**

- [ ] **A)** Valores fijos/restringidos: Se valida mediante Enum o lista permitida: `"Anual"`, `"Primer Cuatrimestre"`, `"Segundo Cuatrimestre"`.
- [ ] **B)** Texto libre no vacío: Se acepta cualquier cadena de hasta 50 caracteres para flexibilidad de diferentes calendarios académicos.

- [x] **Other:** Serían valores restringidos, siendo "Anual" o "Semestral". Y si elige Semestral, debe seleccionarse "Primer Semestre" o "Segundo Semestre" (guardando el valor específico correspondiente: "Anual", "Primer Semestre", o "Segundo Semestre").

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Reglas de validación para "Cantidad de horas semanales"

**Context:** Se requiere `horas` semanales (`materia_horas` Integer). Debemos definir las restricciones de valor mínimo y máximo.

**Options:**

- [x] **A)** Entero positivo estándar: `horas >= 1` y `horas <= 40`.
- [ ] **B)** Entero positivo sin límite superior estricto: `horas > 0`.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Modelado y persistencia de Plan de Estudio | Cambiar a "Plan de Estudio" guardando un año (ej. Integer `planEstudio = 2023`) en la entidad/tabla `materias` |
| B2 | 01 | Blocker | Criterio de unicidad de materia | Opción B: Unicidad por `(nombre, unidad_id, planEstudio)` (case-insensitive para nombre) |
| B3 | 01 | Blocker | Formato del campo Año/s Educativo | Se guarda como String con formato ordinal `"1°"`, `"2°"`, `"3°"`, `"4°"`, `"5°"` (representando el año de carrera) |
| R1 | 01 | Risk | Alcance de autorización y selectores Dependencia / Unidad | UI: selectores de Dependencia y Unidad fijos/bloqueados en su unidad asignada para `ADMIN_UNIDAD`. Backend: validación estricta con HTTP 403 Forbidden |
| R2 | 01 | Risk | Valores permitidos para Despliegue | Valores restringidos: `"Anual"`, `"Primer Semestre"` o `"Segundo Semestre"` (en UI: Anual o Semestral -> selector dependiente de Primer/Segundo Semestre) |
| M1 | 01 | Minor | Restricción de horas semanales | Opción A: Entero positivo entre 1 y 40 (`horas >= 1 && horas <= 40`) |
