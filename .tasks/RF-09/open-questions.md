# Open Questions Document — RF-09: Registrar las designaciones de docentes

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Vinculación con Cargo en el registro de la designación

**Context:** En el modelo relacional del sistema (`cargos` y `designaciones`), la relación se define como `cargo.designacion_id` (un cargo apunta a su designación activa). En el requerimiento crudo (RF-09), los campos de entrada solicitados son: *N° de Legajo/Docente, N° de Resolución, Fecha de comienzo y Fecha de fin*, sin mencionar explícitamente el cargo. Si una designación se crea sin vincularse a un cargo, el cargo no pasa a estado `"Asignado"`, no se conocen las funciones/actividades académicas, y se genera inconsistencia con `CargoService` y `DocenteDetailsDTO`.

**Options:**

- [x] **A)** Vinculación obligatoria con un Cargo: El payload de creación de designación recibe `cargoId` (obligatorio). Se valida que el cargo exista, esté en estado `"Libre"` y no tenga designación vigente. Al crear la designación, se actualiza `cargo.setDesignacion(nuevaDesignacion)` y `cargo.setEstado("Asignado")`.
- [ ] **B)** Vinculación opcional con un Cargo: El campo `cargoId` es opcional en la petición. Si se envía, se vincula y actualiza el cargo a `"Asignado"`; si no se envía, la designación se crea asociada únicamente al docente sin asociar un cargo de inmediato.
- [ ] **C)** Endpoint desacoplado sin Cargo: La designación se crea únicamente con los datos del docente (`docenteId`, `nroResolucion`, `fechaInicio`, `fechaFin`). La asignación de cargos se delega a otro trámite/endpoint existente de gestión de cargos.

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Permisos del rol Auditor en la creación de designaciones

**Context:** El requerimiento crudo enumera como actor a: *"Administrador Global, Administrador de Unidad o Auditor"*. Sin embargo, en la arquitectura de seguridad del sistema Atlas (`SecurityConfig`, `CargoController`, `DocenteController`, `CatedraController`), el rol `AUDITOR` tiene estrictamente permisos de solo lectura (consultas e inspección mediante GET) para preservar la segregación de funciones. Permitir que `AUDITOR` ejecute operaciones de creación muta el estado del sistema y viola el principio de auditoría independiente.

**Options:**

- [x] **A)** Solo Administradores (Recomendado): Restringir la creación (`POST /api/designaciones`) exclusivamente a `ADMIN_GLOBAL` y `ADMIN_UNIDAD` (`hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')`). El rol `AUDITOR` solo podrá consultar designaciones existentes (GET).
- [ ] **B)** Incluir Auditor: Otorgar permisos de creación (`POST /api/designaciones`) al rol `AUDITOR` de forma explícita tal como menciona textualmente el texto del requerimiento crudo.

- [ ] **Other:** ______

**Notes:**

---

### B3 — spec-01: Generación de documentación e impresión de la designación

**Context:** El requerimiento crudo establece: *"generando la documentación adecuada, vinculándola al docente, y dando la posibilidad de imprimir dicha documentación"*. En el backend de Atlas no existen dependencias de generación de PDF o reporteadores (como iText, JasperReports o PDFBox en `pom.xml`), ni una tabla de archivos adjuntos de designaciones. Debemos clarificar el límite de responsabilidad entre Backend y Frontend.

**Options:**

- [x] **A)** Responsabilidad del Frontend vía DTO enriquecido (Recomendado): El backend registra la designación y retorna `200 OK` (o `201 Created`) con `DesignacionResponseDTO` conteniendo toda la información legal e institucional necesaria (datos del docente, resolución, cargo, fechas y emisor). La UI se encarga de renderizar el comprobante oficial y habilitar la impresión (`window.print()` / exportación PDF del navegador).
- [ ] **B)** Backend PDF Generation: Se debe incorporar una biblioteca de generación de PDFs en `pom.xml` (e.g. OpenPDF/iText) y exponer un endpoint `GET /api/designaciones/{id}/documento` que devuelva un `byte[]` en `application/pdf`.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Control de alcance por Unidad Académica para `ADMIN_UNIDAD`

**Context:** Si el usuario autenticado tiene el rol `ADMIN_UNIDAD`, sus acciones deben estar delimitadas a su unidad académica. Dependiendo de cómo se vincule la designación (al cargo o al docente), la regla de verificación de unidad cambia: los cargos tienen `unidad_id` explícito, mientras que los docentes pueden pertenecer a una unidad según su designación/cargo previo.

**Options:**

- [x] **A)** Validación por Unidad del Cargo (Recomendado si se asocia Cargo): Se valida que el `cargo.getUnidad().getId()` coincida con la unidad académica del `ADMIN_UNIDAD`. Si difiere, se rechaza con HTTP `403 Forbidden` (`"No tiene permisos para registrar designaciones en cargos de otra unidad"`). `ADMIN_GLOBAL` puede operar sobre cualquier unidad.
- [ ] **B)** Validación por Unidad del Docente: Se valida que el docente esté adscrito a la misma unidad del `ADMIN_UNIDAD` (o si no tiene unidad previa, se le asocia a la del administrador).
- [ ] **C)** Sin validación de unidad en backend: Se permite la creación a cualquier `ADMIN_UNIDAD` confiando en los filtros aplicados en el frontend.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Notificación al docente y tolerancia a fallos

**Context:** El requerimiento indica: *"El docente es notificado sobre su designación aprobada"*. En el backend existe `EmailService` para el envío de notificaciones por correo electrónico. Si el servidor de correos (SMTP) falla, no responde, o el docente no tiene un correo válido registrado, debemos definir si la transacción se cancela o si la creación persiste con un registro de advertencia.

**Options:**

- [x] **A)** Envío asíncrono fail-safe (Recomendado): Se invoca un método asíncrono (`@Async`) en `EmailService`. Si el envío falla o no hay correo configurado, se registra el error en el log pero la designación se guarda exitosamente (HTTP `201 Created`), garantizando la continuidad operativa.
- [ ] **B)** Envío transaccional síncrono: Si el envío del correo de notificación falla, la transacción hace rollback y se retorna un error HTTP `500 Internal Server Error`.

- [ ] **Other:** ______

**Notes:**

---

### R3 — spec-01: Unicidad del Número de Resolución y validación de duplicados

**Context:** Una designación cuenta con `designacion_numero_resolucion` (Integer). Si se ingresa una resolución repetida para el mismo docente o en el mismo periodo lectivo, podría generarse una duplicación involuntaria.

**Options:**

- [x] **A)** Validación de unicidad de resolución por docente y fechas: No se permite registrar una designación para el mismo docente con el mismo número de resolución, retornando `400 Bad Request` si ya existe.
- [ ] **B)** Permitir números de resolución sin restricción de unicidad estricta en BD: No se bloquea la creación por número de resolución (múltiples docentes pueden estar designados bajo la misma resolución general de facultad/consejo directivo).

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Identificador "N° de Legajo" en la entrada

**Context:** El requerimiento crudo menciona: *"Ingresa: N° de Legajo, N° de Resolución, o Apellido y Nombre del Docente"*. En el modelo de base de datos (`usuarios`, `docentes`), no existe la columna `legajo` (se utiliza `usuario_id`, `docente_dni` o `docente_cuil`).

**Options:**

- [x] **A)** Utilizar `docenteId` en el contrato REST y permitir búsqueda por DNI/Apellido/Nombre (Recomendado): En la API backend el contrato de creación recibe `docenteId` (Integer), y para la búsqueda previa se utiliza el endpoint de consulta de docentes filtrando por DNI, CUIL o Apellido/Nombre.
- [ ] **B)** Incorporar la columna `docente_legajo` en la base de datos: Modificar la entidad `Docente` y el script DDL agregando la columna `legajo`.

- [ ] **Other:** ______

**Notes:** En el futuro quizás tengamos que agregar número de legajo de docente

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | spec-01 | Blocker | Vinculación obligatoria u opcional con Cargo | Opción A: Vinculación obligatoria con un Cargo en estado Libre, pasando a Asignado |
| B2 | spec-01 | Blocker | Permisos del rol Auditor en creación (escritura vs solo lectura) | Opción A: Solo Administradores (ADMIN_GLOBAL y ADMIN_UNIDAD), AUDITOR solo lectura |
| B3 | spec-01 | Blocker | Generación de documentación/impresión (Frontend vs Backend PDF) | Opción A: Frontend vía DTO enriquecido (window.print / comprobante) |
| R1 | spec-01 | Risk | Control de alcance y permisos por Unidad Académica | Opción A: Validación por Unidad del Cargo asociado (403 Forbidden si difiere) |
| R2 | spec-01 | Risk | Mecanismo y resiliencia de la notificación al docente | Opción A: Envío asíncrono fail-safe vía EmailService (@Async) |
| R3 | spec-01 | Risk | Regla de unicidad de la resolución de designación | Opción A: Validación de unicidad de resolución por docente (400 Bad Request) |
| M1 | spec-01 | Minor | Equivalencia de "N° de Legajo" con `docenteId` / `dni` | Opción A: Uso de docenteId en contrato REST; búsqueda por DNI/nombre en UI |

