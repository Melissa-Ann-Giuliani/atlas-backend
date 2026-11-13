# Open Questions Document — RF-11: Asignar funciones a un cargo perteneciente a una designación

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Contrato y granularidad para crear/asignar las funciones

**Context:** El requerimiento indica: *"Para la primera función, ingresa: Cantidad de horas, Nombre de Materia. Al crear una nueva función, ingresa nombre de función, tipo, y cantidad de horas, seleccionando la materia vinculada... Una vez creadas las funciones, presiona sobre el botón 'Generar Documento'"*.
En la base de datos, la tabla `funciones` posee columnas: `funcion_id`, `funcion_nombre` (varchar 100), `funcion_horas` (int), `cargo_id` (fk) y `materia_id` (fk). No existe actualmente una columna `tipo` en la entidad `Funcion`, y para la primera función el requerimiento no menciona un nombre explícito en la entrada.

**Options:**

- [ ] **A)** Envío en lote (Batch / Bulk) al confirmar: Endpoint `POST /api/cargos/{id}/funciones` (o `POST /api/designaciones/{id}/funciones`) que recibe una lista completa de funciones `[{ "nombre": "Docencia Frente a Alumnos", "tipo": "Docencia", "horas": 10, "materiaId": 1 }, ...]`. Si para la primera función el usuario solo especifica materia y horas, el frontend/backend asigna un nombre y tipo por defecto (ej. `"Docencia Regular"` o el nombre de la materia).
- [ ] **B)** Creación función por función: Endpoint unitario `POST /api/cargos/{id}/funciones` que va agregando y acumulando funciones al cargo. Luego, un endpoint separado (o acción de finalización) `POST /api/designaciones/{id}/confirmar` genera el documento y activa al docente.
- [x] **C)** Agregar columna `funcion_tipo` en la base de datos y exigir `nombre`, `tipo`, `horas` y `materiaId` para todas las funciones (homogeneizando el formulario en frontend).

- [ ] **Other:** ______

**Notes:** `funcion_tipo` debería ser de tipo `VARCHAR`, ya que puede contener si es actividad de planta, investigación, gestión o extensión.

---

### B2 — spec-01: Validación de horas asignadas vs. dedicación del cargo

**Context:** El requerimiento estipula como condición: *"La suma de horas de todas las funciones asignadas al cargo no puede superar la cantidad de horas correspondientes a la dedicación del cargo"*. Además indica: *"actualizando la cantidad de horas ocupadas del cargo como la sumatoria de horas de todas las funciones"*.

**Options:**

- [x] **A)** Límite máximo permisivo ($\le$): La sumatoria de horas de las funciones puede ser menor o igual a `dedicacion.dedicacionHoras` (permite que un cargo de 40hs tenga funciones asignadas por 20hs y quede con horas ocupadas = 20 y horas disponibles = 20).
- [ ] **B)** Límite exacto (=): Al confirmar y generar el documento de designación, la suma total de horas debe igualar exactamente la dedicación del cargo ($\sum \text{horas} = \text{dedicacionHoras}$), rechazando si quedan horas sin asignar.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Generación y vinculación del documento

**Context:** El requerimiento dice: *"Consecuentemente, queda registrado en el sistema la designación del docente, generando la documentación adecuada, vinculándola al docente, y dando la posibilidad de imprimir dicha documentación"*. Existe un módulo/entidad `Archivo` en el backend (`/api/archivos`).

**Options:**

- [x] **A)** Generación de PDF en backend: El backend compila un PDF formal con los datos del docente, resolución, cargo y desglose de funciones/horas, lo guarda en `archivos` vinculado a la entidad y retorna la URL/ID de descarga para que el frontend lo descargue o mande a imprimir.
- [ ] **B)** Simulación / registro de metadato de documento: Se registra una entrada en la tabla `archivos` con el nombre generado (ej. `designacion-{id}.pdf`) y metadatos, delegando el renderizado visual al frontend vía vista de impresión HTML/CSS (`window.print()`).

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Notificación al docente y activación

**Context:** El requerimiento dice: *"El estado del docente se actualiza automáticamente, quedando en el sistema con el nuevo estado 'Activo' y se manda una notificación al docente solicitante"*. En RF-10 se acordó explícitamente: *"Debería enviarse el correo solamente cuando se haya confirmado todo el proceso, lo cual será en RF-11"*.

**Options:**

- [x] **A)** Notificación asíncrona por Email vía `EmailService`: Se actualiza `docente.usuario.activo = true` (o estado en Docente) y se invoca `emailService.sendDesignacionCompletaNotification(...)` con el resumen del cargo, funciones asignadas y adjunto/enlace al documento.
- [ ] **B)** Notificación interna en sistema + Email: Se crea una notificación en base de datos para la bandeja del usuario y adicionalmente se envía el correo electrónico.

- [ ] **Other:** ______

**Notes:**

---

### R3 — spec-01: Alcance de las Materias a asignar

**Context:** Cada función requiere una `Materia`. Un cargo pertenece a una `Unidad` académica (Facultad/Departamento).

**Options:**

- [x] **A)** Restricción estricta por Unidad: Solo se pueden seleccionar materias que pertenezcan a la misma Unidad académica a la que pertenece el Cargo.
- [ ] **B)** Catálogo abierto: Se permite seleccionar cualquier materia activa del sistema sin filtrar por unidad (o con filtro opcional).

- [ ] **Other:** ______

**Notes:**

---

## Summary of Decisions

| Question | Status | Decision | Impact |
| -------- | ------ | -------- | ------ |
| B1 (Contrato/Granularidad funciones) | Resolved | **Opción C** | Agregar columna `funcion_tipo` (`VARCHAR(50)`) a `funciones`. Exigir `nombre`, `tipo` (ej. docencia/planta, investigación, gestión, extensión), `horas` y `materiaId` en el payload. |
| B2 (Límite horas ocupadas) | Resolved | **Opción A** | Regla permisiva $\sum \text{horas} \le \text{cargo.dedicacionHoras}$. Se actualiza `cargo.horasOcupadas` con la sumatoria. |
| R1 (Generación de documento) | Resolved | **Opción A** | Generar PDF de la designación en el backend (iText/OpenPDF o generador existente), almacenar en entidad `Archivo` y retornar ID/URL para descarga/impresión. |
| R2 (Notificación docente) | Resolved | **Opción A** | Activar docente (`usuario.activo = true`) y despachar notificación por correo vía `EmailService` con detalles del cargo y funciones. |
| R3 (Alcance de materias) | Resolved | **Opción A** | Validar que cada `materiaId` pertenezca a la misma `Unidad` del cargo. Rechazar con `400 Bad Request` si pertenece a otra unidad. |
