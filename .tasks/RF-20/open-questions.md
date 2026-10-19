# Open Questions Document — RF-20: Baja de un cargo

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Definición precisa de "Designación Vigente"

**Context:** El requerimiento dice: *"El cargo no debe estar vinculado a una designación vigente; si lo estuviera, primero debe desvincularse o finalizarse dicha designación"*. En el modelo de datos, `Cargo` tiene una relación `designacion` (`Designacion`), y la entidad `Designacion` contiene `designacionEstadoActual`, `designacionFechaInicio` y `designacionFechaFin`. Además, `Cargo` tiene `cargo_estado` (ej. "Libre", "Asignado", "Licencia"). Necesitamos definir la regla exacta para determinar si un cargo tiene una designación vigente.

**Options:**

- [x] **A)** Una designación se considera vigente si `cargo.getDesignacion() != null` y (`designacion.getDesignacionFechaFin() == null` o `designacion.getDesignacionFechaFin().isAfter(LocalDate.now())`) y `designacion.getDesignacionEstadoActual()` no es "Finalizada" / "Baja".
- [ ] **B)** Se rechaza la baja si `cargo.getDesignacion() != null` (independientemente de las fechas, exigiendo que previamente se desvincule formalmente poniendo `designacion_id = null`).
- [ ] **C)** Se evalúa únicamente el estado del cargo: si `cargo.getEstado().equalsIgnoreCase("Asignado")` se rechaza la baja, pero si está en `"Libre"` o `"Licencia"` se permite.

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Verbo y Ruta del Endpoint REST

**Context:** Dar de baja un cargo no es un borrado físico (`DELETE` en BD), sino una actualización de estado de ciclo de vida (`cargo_estado = "Dado de baja"`), preservando todos los datos históricos e integridad referencial. Debemos definir el contrato de la API.

**Options:**

- [x] **A)** `PATCH /api/cargos/{id}/baja` (o `PUT /api/cargos/{id}/baja`), semánticamente claro para un cambio de estado en el recurso.
- [ ] **B)** `DELETE /api/cargos/{id}`, manejándolo internamente como soft delete / cambio de estado a `"Dado de baja"`.
- [ ] **C)** `POST /api/cargos/{id}/baja`, utilizando una acción RPC-style sobre el recurso.

- [ ] **Other:** ______

**Notes:**

---

### B3 — spec-01: Alcance de Roles y Autorización (Admin Global vs Admin Unidad)

**Context:** La tabla de requisitos crudos indica explícitamente: `Actor: Administrador Global`. Sin embargo, en otros requerimientos de gestión de cargos (como RF-18 modificación y RF-19 subdivisión), se permite también la acción a `Admin de Unidad` restringida a los cargos de su propia unidad académica.

**Options:**

- [x] **A)** Solo `ADMIN_GLOBAL`: Exclusivo para el Administrador Global según la especificación estricta del requerimiento crudo. Si un `ADMIN_UNIDAD` intenta dar de baja un cargo, se responde con HTTP `403 Forbidden`.
- [ ] **B)** `ADMIN_GLOBAL` y `ADMIN_UNIDAD`: `ADMIN_GLOBAL` puede dar de baja cualquier cargo, y `ADMIN_UNIDAD` puede dar de baja cargos únicamente pertenecientes a su unidad asignada.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: Comportamiento ante un cargo ya "Dado de baja"

**Context:** Si se invoca el endpoint sobre un cargo que ya tiene estado `"Dado de baja"`.

**Options:**

- [x] **A)** Rechazar la operación con HTTP `400 Bad Request` indicando un mensaje claro de error ("El cargo ya se encuentra dado de baja").
- [ ] **B)** Idempotente: retornar HTTP `200 OK` con el cargo sin realizar modificaciones adicionales.

- [ ] **Other:** ______

**Notes:**

---

### R2 — spec-01: Impacto en cargos subdivididos o predecesores

**Context:** Un cargo puede haber sido subdividido (RF-19) y tener subcargos asociados (`cargoOrigenDivision`), o ser predecesor de otro cargo (`cargoPredecesor`).

**Options:**

- [ ] **A)** Se permite la baja siempre que se cumpla la condición principal (no tener designación vigente). Las relaciones históricas (`cargoOrigenDivision`, `cargoPredecesor`) se preservan intactas en la base de datos.
- [x] **B)** Si el cargo tiene subcargos activos originados por división que no estén dados de baja, se debe bloquear la baja del padre.

- [ ] **Other:** ______

**Notes:**

---

### R3 — spec-01: Visibilidad en la búsqueda de cargos activos (`findActiveCargos`)

**Context:** Al dar de baja un cargo, `cargo_estado` pasa a `"Dado de baja"`. En `CargoRepository.findActiveCargos`, cuando el parámetro `estado` es `null`, la consulta actual lista todos los cargos sin filtrar por estado a menos que se envíe explícitamente `estado`.

**Options:**

- [ ] **A)** Mantener el filtrado por parámetro `estado`: la UI de gestión de cargos puede buscar cargos por estado `"Dado de baja"`, `"Libre"`, `"Asignado"`, etc., según lo estipulado en la entrada del requerimiento ("Busca el cargo por número de cargo, fecha de creación o estado").
- [ ] **B)** Excluir por defecto los cargos `"Dado de baja"` en `findActiveCargos` cuando `estado == null`.

- [x] **Other:** Los cargos dados de baja van a aparecer en el futuro en la página Historial. No deben aparecer en

**Notes:**

---

## Minor Issues — can be decided during implementation

---

### M1 — spec-01: Payload de la solicitud de baja

**Context:** ¿Se requiere enviar un cuerpo en la solicitud (ej. motivo de baja, número de resolución) o es una acción directa sin payload adicional?

**Options:**

- [x] **A)** Sin body requerido (solicitud vacía `PATCH /api/cargos/{id}/baja`), ya que el requerimiento solo estipula cambiar el estado a "Dado de baja".
- [ ] **B)** Opcional/Requerido DTO con campo `motivo` o `observaciones`.

- [ ] **Other:** ______

**Notes:**

---

## Summary of Decisions

| Code | Status | Decision Summary | Impacted Specs |
| --- | --- | --- | --- |
| B1 | Decided (A) | Designación vigente si existe, no está finalizada y fechaFin >= hoy | `spec-01-baja-cargo.md` |
| B2 | Decided (A) | Endpoint `PATCH /api/cargos/{id}/baja` | `spec-01-baja-cargo.md` |
| B3 | Decided (A) | Exclusivo para rol `ADMIN_GLOBAL` (403 para `ADMIN_UNIDAD`) | `spec-01-baja-cargo.md` |
| R1 | Decided (A) | Retornar 400 Bad Request si el cargo ya está dado de baja | `spec-01-baja-cargo.md` |
| R2 | Decided (B) | Bloquear baja si el cargo tiene subcargos activos originados por división | `spec-01-baja-cargo.md` |
| R3 | Decided (Other) | Los cargos dados de baja no deben aparecer en la búsqueda de cargos activos (se reservan para la página Historial) | `spec-01-baja-cargo.md`, `CargoRepository.java` |
| M1 | Decided (A) | Sin body requerido en la petición | `spec-01-baja-cargo.md` |
