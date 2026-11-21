# Open Questions Document — RF-37

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Mecanismo de re-designación: ¿Nueva designación con clonación o extensión de la existente?

**Context:** El requerimiento dice: *"Se extiende o genera la nueva designación del docente, y el alerta de vencimiento correspondiente se desactiva"*. Si se sobreescribe/extiende la designación existente, se pierde la trazabilidad histórica de resoluciones y períodos académicos anteriores. Si se genera una nueva, se debe definir el ciclo de vida de la designación anterior (por ejemplo, si pasa de inmediato a estado `"Renovada"`/`"Inactiva"` o si permanece `"Activa"` hasta que transcurra su `fechaFin`).

**Options:**

- [x] **A)** **Generar una nueva designación y dar por finalizada la anterior:** Se crea una nueva entidad `Designacion` clonando docente y cargos vinculados. La designación anterior se marca con estado `"Renovada"` o `"Inactiva"` inmediatamente para desactivar su alerta.
- [ ] **B)** **Generar una nueva designación con inicio diferido:** Se crea una nueva `Designacion` con `fechaInicio = designacionAnterior.fechaFin + 1 día`. La anterior permanece `"Activa"` hasta su fecha de fin real, pero se le asocia un flag o estado `"Renovada"` para que el cálculo de alertas del RF-36 la ignore y no vuelva a alertar.
- [ ] **C)** **Extensión in-place:** No se crea un nuevo registro; se actualiza la `fechaFin` y opcionalmente el `numeroResolucion` en la designación existente. La alerta desaparece porque `fechaFin` supera el umbral de 30 días.

- [ ] **Other:** ______

**Notes:** Se debe dejar también colocar las nuevas fechas de inicio y fin de la nueva designación

---

### B2 — spec-01: Entrada de datos requerida en el flujo de Re-designación Simple

**Context:** El objetivo del RF-37 es que la acción sea "de forma simple" desde la pantalla de Vencimientos. Es necesario definir qué datos solicita el formulario modal/acción para no sobrecargar de inputs al usuario, pero asegurando la validez legal del nombramiento.

**Options:**

- [ ] **A)** **Mínimo estricto:** Solo solicita el nuevo `numeroResolucion` y la nueva `fechaFin`. Los cargos, docente y dedicación se replican idénticos; la `fechaInicio` se calcula automáticamente como el día posterior a la `fechaFin` previa.
- [ ] **B)** **Mínimo con confirmación de fechas:** Solicita `numeroResolucion`, `fechaInicio` (pre-poblada con día posterior a la fecha fin actual pero editable) y `fechaFin`. No permite alterar cargos ni docente en este modal rápido.
- [ ] **C)** **Formulario completo pre-cargado:** Abre el formulario general de designación pre-llenado con todos los datos y cargos de la designación previa, permitiendo ajustar cargos, fechas y resolución antes de confirmar.

- [x] **Other:** Se supone que ya está implementado el DTO que envía la información necesaria al frontend para ver cuáles están por vencer (cada uno con un botón de renovar). Si se presiona ese botón, debe ir a la pantalla de designación en trámites, enviando la información ya cargada, lista para modificar lo que fuera necesario (fechas, funciones, etc).

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Validación de precondición (¿Debe estar obligatoriamente por vencer?)

**Context:** El requerimiento estipula como condición: *"Debe existir una alerta de vencimiento activa sobre la designación"*. Si un usuario intenta invocar la API de re-designación con una designación que no está dentro del período de alerta (vence en más de 30 días) o que ya está cerrada, debe definirse si se bloquea a nivel de servicio o se permite flexibilidad.

**Options:**

- [ ] **A)** **Validación estricta (400 Bad Request):** El backend valida que la designación esté `"Activa"` y dentro del umbral de vencimiento próximo (`fechaFin <= today + 30 días` o ya vencida recientemente). Si no cumple, rechaza la operación.
- [x] **B)** **Flexible:** Solo valida que la designación exista, pertenezca a la unidad correspondiente y esté `"Activa"`, permitiendo re-designar aunque falten más de 30 días.

- [ ] **Other:** 

**Notes:** Ya que va a ser flexible, deberías agrevarse unos detalles:
-- Agregar un parámetro opcional al endpoint GET /api/designaciones/vencimientos: Por ejemplo: @RequestParam(required = false) Integer dias (o diasUmbral), o un flag @RequestParam(required = false) Boolean sinLimite.
-- En DesignacionRepository y DesignacionService, si viene el umbral personalizado o flag, ajustar/omitir fechaHasta = hoy.plusDays(...).

---

### R2 — spec-01: Restricciones de autorización y control de unidades (RBAC multi-tenant)

**Context:** Siguiendo las reglas de negocio de RF-36 y del sistema, un `ADMIN_UNIDAD` solo debe poder re-designar designaciones pertenecientes a los cargos de su propia unidad académica, mientras que `ADMIN_GLOBAL` puede operar sobre cualquier unidad. Si no se valida a nivel de entidad/cargo, un Admin de Unidad podría re-designar docentes de otras facultades.

**Options:**

- [x] **A)** **Validación estricta de pertenencia a Unidad:** `ADMIN_UNIDAD` solo puede re-designar si todos los cargos asociados a la designación corresponden a su unidad asignada; de lo contrario retorna `403 Forbidden`. `ADMIN_GLOBAL` tiene acceso irrestricto.
- [ ] **B)** **Basado únicamente en el rol:** Cualquier usuario con rol `ADMIN_UNIDAD` o `ADMIN_GLOBAL` puede ejecutar la acción sin contrastar la unidad del cargo vs. la unidad del token.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Endpoint REST propuesto para la acción

**Context:** Conviene definir la convención de la API para que guarde consistencia con el controlador `DesignacionController`.

**Options:**

- [x] **A)** `POST /api/designaciones/{id}/redesignar` recibiendo un DTO de request (`RedesignacionRequestDTO`) con los campos definidos en B2.
- [ ] **B)** `POST /api/designaciones` estándar (reutilizando el endpoint existente de alta enviando el payload completo).
- [ ] **C)** `PUT /api/designaciones/{id}/extender` para modificación in-place.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|---|---|---|---|---|
| B1 | 01 | Blocker | Mecanismo: nueva designación vs. extensión | **Opción A:** Nueva entidad `Designacion`, anterior pasa a `"Renovada"`/`"Inactiva"`. Nuevas fechas de inicio y fin configurables. |
| B2 | 01 | Blocker | Datos de entrada para re-designación simple | **Other:** Flujo integrado frontend/backend: desde vencimientos botón "Renovar" navega al formulario de designación pre-cargado con docente, cargo y datos previos para editar fechas/funciones y emitir `POST /api/designaciones/{id}/redesignar`. |
| R1 | 01 | Risk | Validación de precondición de alerta activa | **Opción B + Other:** Validación flexible (activa + permisos). Se añade soporte de parámetro opcional `dias` / `sinLimite` en `GET /api/designaciones/vencimientos` para flexibilidad en umbrales de búsqueda. |
| R2 | 01 | Risk | Validación RBAC multi-tenant por unidad | **Opción A:** Validación estricta por unidad. `ADMIN_UNIDAD` solo opera sobre designaciones con cargos de su unidad. `ADMIN_GLOBAL` sin restricción. |
| M1 | 01 | Minor | Convención del endpoint REST | **Opción A:** `POST /api/designaciones/{id}/redesignar` recibiendo `RedesignacionRequestDTO` o `DesignacionRedesignarRequest`. |

---
