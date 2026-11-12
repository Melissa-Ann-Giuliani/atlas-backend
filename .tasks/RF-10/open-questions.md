# Open Questions Document — RF-10: Vincular un cargo a una designación de un docente

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Modalidad de flujo y endpoint para vincular el cargo a la designación

**Context:** En RF-09 (`POST /api/designaciones`), se desacopló la obligatoriedad de `cargoId` para permitir crear una designación inicialmente sin cargo asociado (dejando al docente en su estado previo), o bien enviando `cargoId` en la misma petición inicial. Sin embargo, RF-10 establece: *"El administrador, luego de haber generado una nueva designación en Trámites, elige entre los distintos cargos en estado 'Libre' disponibles... La designación creada se le vincula el cargo elegido, el cual pasa a estado 'Asignado'"*. Actualmente no existe un endpoint dedicado para vincular un cargo a una designación ya existente.

**Options:**

- [x] **A)** Soportar ambos flujos mediante un nuevo endpoint de vinculación `PATCH /api/designaciones/{id}/cargo` (o `PUT /api/designaciones/{id}/vincular-cargo` con `{ "cargoId": 25 }`) **además** de mantener el soporte directo de `cargoId` en `POST /api/designaciones`. Así, el frontend puede vincular el cargo tanto en un paso posterior (ej. tras volver de crear un cargo en RF-17) como en el mismo paso de creación.
- [ ] **B)** Flujo estrictamente en dos pasos: `POST /api/designaciones` solo registra la designación (sin cargo), y la vinculación del cargo se realiza exclusivamente a través del nuevo endpoint `PATCH /api/designaciones/{id}/cargo`.
- [ ] **C)** Flujo en un único envío al confirmar en el frontend: En la pantalla de Trámites > Designaciones, la selección del cargo en el modal/lista ("Asignar cargo") ocurre en cliente antes del submit final, persistiendo todo en una única llamada a `POST /api/designaciones` con `cargoId` (sin agregar un nuevo endpoint de vinculación posterior).

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Reasignación o reemplazo cuando la designación ya tiene un cargo vinculado

**Context:** En el modelo de datos (`cargos.designacion_id`), la relación es 1:1 en la práctica operativa de una designación a un cargo, aunque la clave foránea reside en la tabla `cargos`. Si se invoca la vinculación de cargo sobre una designación que ya poseía un cargo previamente vinculado, no está definido si debe rechazarse la operación o si debe liberarse el cargo anterior.

**Options:**

- [x] **A)** Rechazar si ya posee un cargo vinculado (`400 Bad Request`): Una designación creada sin cargo solo puede vincularse una vez a un cargo `"Libre"`. Si la designación ya tiene un cargo asociado, se retorna `400 Bad Request` (`"La designación ya posee un cargo vinculado"`). Cualquier cambio posterior debe tramitarse por modificación/baja.
- [ ] **B)** Reemplazo automático del cargo anterior: Si la designación ya tenía un cargo vinculado, el cargo anterior vuelve a estado `"Libre"` (`designacion = null`) y el nuevo cargo elegido pasa a `"Asignado"`.
- [ ] **C)** Permitir múltiples cargos por designación (1:N): Una misma resolución/designación puede vincularse a más de un cargo sin desvincular el anterior.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Activación del Docente y disparo de notificación por correo electrónico

**Context:** En el commit reciente `8f834dd` de RF-09, cuando se registra una designación sin `cargoId`, el docente permanece en estado `"Inactivo"` hasta que se vincule el cargo en etapas posteriores, pero `EmailService.sendDesignacionNotification` se ejecuta igualmente en `registrarDesignacion` diciendo *"Tu estado en el sistema ahora se encuentra Activo"*. Debemos definir en qué momento exacto se activa al docente y se envía el correo de notificación.

**Options:**

- [ ] **A)** Activar al docente y notificar al vincular el cargo: Cuando se vincula el cargo en RF-10, el docente pasa a `"Activo"` (`usuario.activo = true`), el cargo pasa a `"Asignado"`, y se envía la notificación por correo (o solo se envía el correo cuando la designación queda efectivamente vinculada a un cargo y el docente activo).
- [ ] **B)** Activar al docente en RF-10 sin re-enviar correo: `POST /api/designaciones` envía el correo en el alta de la designación (RF-09), y el paso de vinculación de cargo (RF-10) únicamente cambia el cargo a `"Asignado"` y el docente a `"Activo"` sin disparar un segundo email.

- [x] **Other:** Debería enviarse el correo solamente cuando se haya confirmado todo el proceso, lo cual será en RF-11: Asignar funciones a un cargo perteneciente a una designación (que será abordado más adelante)

**Notes:**

---

### R2 — spec-01: Consulta de cargos disponibles en estado "Libre"

**Context:** El requerimiento indica: *"elige entre los distintos cargos en estado 'Libre' disponibles"*. Actualmente existe `GET /api/cargos?estado=Libre` (de RF-06) que filtra automáticamente por la unidad académica del `ADMIN_UNIDAD` (o todas las unidades para `ADMIN_GLOBAL`), excluye los `"Dado de baja"` y soporta paginación y búsqueda.

**Options:**

- [x] **A)** Reutilizar `GET /api/cargos?estado=Libre` existente: Utilizar el endpoint actual de listado de cargos pasando el filtro `estado=Libre` (respetando el alcance por unidad del administrador autenticado).
- [ ] **B)** Crear un endpoint específico `GET /api/cargos/libres` (sin paginación o con DTO simplificado `CargoResumenDTO`) pensado exclusivamente para selectores/dropdowns en el formulario de designaciones.

- [ ] **Other:** ______

**Notes:**

---

### R3 — spec-01: Control de alcance por Unidad Académica para `ADMIN_UNIDAD`

**Context:** Al igual que en RF-09 y RF-17, un `ADMIN_UNIDAD` solo tiene jurisdicción sobre su propia unidad académica. Si un `ADMIN_UNIDAD` intenta vincular a una designación un cargo perteneciente a otra unidad, el backend debe impedirlo.

**Options:**

- [x] **A)** Validación estricta de unidad del cargo (`403 Forbidden`): Verificar que `cargo.getUnidad().getUnidadId()` coincida con la unidad del `ADMIN_UNIDAD` autenticado (incluyendo el rol `ADMINISTRADOR_DE_UNIDAD`). En caso contrario, lanzar `AccessDeniedException` (`403 Forbidden`: `"No tiene permisos para vincular cargos de otra unidad"`).
- [ ] **B)** Sin restricción de unidad en la vinculación: Permitir vincular cualquier cargo en estado `"Libre"` sin importar la unidad.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Redirección a "Crear Cargo" (RF-17) y retorno al trámite

**Context:** El requerimiento menciona: *"En caso de necesitar un nuevo cargo, puede ser redirigido a Gestión para un alta de cargo (RF17) a través del botón 'Crear Cargo'"*. En una arquitectura REST, la navegación entre pantallas es responsabilidad del Frontend, mientras que el Backend provee `POST /api/cargos` (RF-17) y el endpoint de vinculación (RF-10).

**Options:**

- [x] **A)** Responsabilidad de navegación en Frontend: El botón "Crear Cargo" es una redirección de UI hacia RF-17 (`POST /api/cargos`). El backend garantiza que la designación pueda crearse previamente sin cargo (o que el nuevo cargo creado en estado `"Libre"` aparezca inmediatamente en `GET /api/cargos?estado=Libre`) para luego vincularlo mediante RF-10.
- [ ] **B)** Creación de cargo inline dentro del endpoint de designación: Permitir enviar un objeto `nuevoCargo` embebido dentro del request de designación/vinculación para crear el cargo y vincularlo en una sola transacción.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | spec-01 | Blocker | Endpoint y flujo para vincular un cargo a una designación existente | Opción A: Nuevo endpoint `PATCH /api/designaciones/{id}/cargo` además de mantener soporte en `POST /api/designaciones` |
| B2 | spec-01 | Blocker | Comportamiento si la designación ya posee un cargo vinculado | Opción A: Rechazar con `400 Bad Request` (`"La designación ya posee un cargo vinculado"`) |
| R1 | spec-01 | Risk | Activación del docente y momento de envío del email de notificación | Other: Diferir el envío del correo de notificación hasta la confirmación final del proceso en RF-11 (Asignar funciones al cargo); en RF-10 no se envía email |
| R2 | spec-01 | Risk | Endpoint para listar los cargos en estado "Libre" | Opción A: Reutilizar `GET /api/cargos?estado=Libre` existente |
| R3 | spec-01 | Risk | Validación de alcance de unidad para `ADMIN_UNIDAD` | Opción A: Validación estricta de unidad del cargo (`403 Forbidden`: `"No tiene permisos para vincular cargos de otra unidad"`) |
| M1 | spec-01 | Minor | Alcance backend/frontend del botón "Crear Cargo" (redirección a RF-17) | Opción A: Navegación en Frontend hacia RF-17 (`POST /api/cargos`) y posterior vinculación en RF-10 |

