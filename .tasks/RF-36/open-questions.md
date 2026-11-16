# Open Questions Document — RF-36: Mantener un control sobre los próximos vencimientos de designaciones

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Configuración y dinamismo del umbral de días para vencimientos

**Context:** El requerimiento estipula: *"La fecha de finalización debe encontrarse dentro de un umbral configurado (por ejemplo, 30 días antes del vencimiento)"*. Si no se define el mecanismo de parametrización del umbral, no queda claro si el backend filtra por un valor fijo o si la UI puede solicitar diferentes ventanas de tiempo (ej. 15, 30, 60 días).

**Options:**

- [ ] **A)** Umbral por defecto configurable con override opcional por query parameter: El sistema define un valor predeterminado (ej. 30 días vía configuración/property), pero los endpoints `GET /api/designaciones/vencimientos` y `GET /api/designaciones/vencimientos/resumen` aceptan el parámetro opcional `?dias=30` para permitir al usuario cambiar la ventana en la interfaz.
- [x] **B)** Umbral fijo estático del sistema: El backend calcula siempre con una constante fija de 30 días sin permitir parámetro dinámico en la consulta.
- [ ] **C)** Configuración persistida por Unidad / Global en Base de Datos: Se agrega una tabla o columna de configuración del sistema donde cada unidad o el administrador global define su umbral en días.

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01/02: Inclusión o exclusión de designaciones ya vencidas

**Context:** El requerimiento dice: *"El sistema debe mantener un control constante sobre los próximos vencimientos de ciertas designaciones, mostrando alertas de cuánto falta para el vencimiento"* y *"indicando los días restantes antes de vencer"*. Si una designación venció ayer y no fue renovada ni dada de baja, omitirla podría provocar que designaciones vencidas pasen desapercibidas.

**Options:**

- [ ] **A)** Solo futuras dentro del umbral: Filtrar estrictamente `fechaFin >= CURRENT_DATE` y `fechaFin <= CURRENT_DATE + umbral`. Las ya vencidas no aparecen en este listado ni en el contador de próximos vencimientos.
- [x] **B)** Próximas a vencer + ya vencidas recientes (o filtro por estado): Por defecto incluir próximas a vencer (`diasRestantes >= 0`), pero permitir un parámetro o pestaña en la vista para listar designaciones ya caducadas recientemente (o mostrar días restantes negativos / badge "Vencida").
- [ ] **C)** Incluir vencidas pendientes de regularización: El listado y el contador consideran toda designación en estado activo cuya `fechaFin <= CURRENT_DATE + umbral` (las vencidas muestran días restantes = 0 o valor negativo).

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-02: Mecanismo y ubicación de la "Alerta al ingresar al sistema"

**Context:** El requerimiento indica: *"Además, al ingresar un administrador al sistema, se le muestra un alerta, indicando la cantidad de designaciones por vencer"*. Si no se define la UX/API correspondiente, el frontend podría no saber si renderizar un diálogo modal intrusivo, una notificación tipo toast o una tarjeta/badge en el dashboard principal.

**Options:**

- [x] **A)** Endpoint dedicado de resumen en Dashboard (`GET /api/dashboard/vencimientos/resumen` o `/api/designaciones/vencimientos/resumen`): Retorna `{ "totalPorVencer": N, "umbralDias": 30 }`. El frontend lo consume al montar la vista de inicio/dashboard y muestra una tarjeta informativa destacada con acceso directo a *Gestión > Vencimientos*.
- [ ] **B)** Banner / Notificación Toast al iniciar sesión: Muestra un aviso flotante / banner descartable en la cabecera apenas inicia la sesión del administrador indicando: *"Tiene N designaciones próximas a vencer en los próximos 30 días"*.
- [ ] **C)** Ambas (Badge en navegación/dashboard + notificación al ingresar): El endpoint provee el conteo para el badge y para el banner informativo de bienvenida.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Estado de las designaciones a auditar

**Context:** En la entidad `Designacion`, existe el campo `designacion_estado_actual` (ej. "Activa", "Baja", "Revocada", "Finalizada"). Si no se filtra por el estado actual, designaciones que fueron dadas de baja o canceladas prematuramente podrían alertar falsos positivos.

**Options:**

- [x] **A)** Solo designaciones en estado `"Activa"` (o no finalizadas/canceladas): Filtrar `designacionEstadoActual = 'Activa'` con `designacionFechaFin IS NOT NULL`.
- [ ] **B)** Filtrar excluyendo estados terminales: Excluir explícitamente estados `"Baja"`, `"Cancelada"`, `"Anulada"`.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Información requerida en la grilla de salida

**Context:** El requerimiento menciona: *"indicando los docentes pertenecientes, y los días restantes antes de vencer la designación correspondiente"*. Una designación se vincula a cargos docentes que a su vez poseen materia, dedicación, categoría y unidad académica.

**Options:**

- [x] **A)** DTO enriquecido: Retornar ID de designación, número de resolución, fecha de inicio, fecha de fin, días restantes, ID y nombre completo del docente, documento/legajo, unidad académica, y cargos asociados (código de cargo, dedicación, materia).
- [ ] **B)** DTO simplificado: Retornar solo ID designación, resolución, fecha de fin, días restantes, nombre del docente y unidad.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Parametrización del umbral de días para vencimientos | **B**: Umbral fijo estático del sistema (30 días) |
| B2 | 01/02 | Blocker | Alcance con respecto a designaciones ya vencidas | **B**: Próximas a vencer por defecto (`diasRestantes >= 0`) con soporte para consultar vencidas recientes |
| R1 | 02 | Risk | Contrato y formato de la alerta al ingresar al sistema | **A**: Endpoint dedicado en Dashboard (`GET /api/designaciones/vencimientos/resumen`) para tarjeta informativa en inicio |
| R2 | 01 | Risk | Filtro por estado vigente de la designación | **A**: Solo designaciones en estado `"Activa"` con `fechaFin` definida |
| M1 | 01 | Minor | Nivel de detalle y estructura del DTO de vencimientos | **A**: DTO enriquecido (resolución, fechas, días restantes, docente con documento/legajo, unidad y cargos) |
