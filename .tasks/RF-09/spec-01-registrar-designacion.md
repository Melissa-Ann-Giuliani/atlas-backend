# User Story — spec-01: Registrar las designaciones de docentes

---

## User Story Description

Esta especificación cubre la funcionalidad del backend para registrar las designaciones de docentes en el sistema Atlas (RF-09). Permite a los Administradores Globales (`ADMIN_GLOBAL`) y Administradores de Unidad (`ADMIN_UNIDAD`) registrar formalmente una designación para un docente, vinculando el número de resolución, el periodo de vigencia (fecha de inicio y fecha de fin opcional) y el cargo académico correspondiente (obligatorio según decisión B1).

Al completarse el registro:
1. Se crea la entidad `Designacion` con estado `"Activo"`.
2. El cargo indicado (que debe encontrarse en estado `"Libre"`) se vincula a la designación (`cargo.setDesignacion(...)`) y pasa a estado `"Asignado"`.
3. El estado del docente transiciona a `"Activo"` (`docente_estado = "Activo"`) y su usuario queda habilitado (`activo = true`).
4. Se valida la unicidad de la resolución para el docente (decisión R3), impidiendo duplicaciones.
5. Se dispara una notificación asíncrona fail-safe por correo electrónico al docente a través de `EmailService` (decisión R2).
6. Se retorna el DTO enriquecido `DesignacionResponseDTO` con la información consolidada para su visualización e impresión en frontend (decisión B3).

---

## Story (INVEST)

```
**As an** Administrador Global o Administrador de Unidad,  
**I want to** registrar la designación académica de un docente vinculándolo a un cargo disponible, con su número de resolución y periodo de vigencia,  
**So that** el docente y su cargo queden formalmente activos en el sistema, asegurando la consistencia institucional y permitiendo emitir la documentación correspondiente.
```

---

## Analysis

### Approach
1. **Endpoint de Creación**: Exponer `POST /api/designaciones` recibiendo `DesignacionCreateRequest` con validaciones Bean Validation (`@Valid`).
2. **Control de Acceso y Roles (Decisión B2)**:
   - Autorizar a roles `ADMIN_GLOBAL`, `ADMIN_UNIDAD` y `ADMIN_DE_UNIDAD`.
   - Denegar acceso al rol `AUDITOR` u otros roles con HTTP `403 Forbidden` (segregación de funciones).
3. **Validaciones de Negocio**:
   - **Existencia del Docente (Decisión M1)**: Si el `docenteId` no existe en la base de datos, retornar HTTP `404 Not Found` (`"Docente no encontrado"`).
   - **Validación del Cargo (Decisión B1)**: `cargoId` es obligatorio.
     - Debe existir en BD (`404 Not Found`).
     - Debe encontrarse en estado `"Libre"` (`400 Bad Request` `"El cargo ya se encuentra asignado o en licencia"`).
     - Si el actor es `ADMIN_UNIDAD`, la unidad del cargo debe coincidir con su unidad académica asignada (Decisión R1, `403 Forbidden` `"No tiene permisos para registrar designaciones en cargos de otra unidad"`).
   - **Consistencia de Fechas**: `fechaInicio` es obligatoria. Si se envía `fechaFin`, debe ser igual o posterior a `fechaInicio` (`400 Bad Request` `"La fecha de finalización no puede ser anterior a la fecha de inicio"`).
   - **Unicidad de Resolución por Docente (Decisión R3)**: Si el docente ya cuenta con una designación registrada bajo el mismo número de resolución, rechazar con HTTP `400 Bad Request` (`"El docente ya posee una designación registrada con el número de resolución indicado"`).
4. **Persistencia Transaccional (`@Transactional`)**:
   - Guardar `Designacion` con `designacion_estado_actual = "Activo"`.
   - Actualizar `cargo.setDesignacion(designacion)` y `cargo.setEstado("Asignado")`.
   - Actualizar `docente.setEstado("Activo")` y `usuario.setActivo(true)`.
5. **Notificación Asíncrona Fail-Safe (Decisión R2)**:
   - Invocar `@Async` en `EmailService` con destinatario y datos de la designación. Si el envío SMTP falla, capturar y registrar en logs sin abortar la transacción.
6. **Respuesta Enriquecida (Decisión B3)**:
   - Retornar HTTP `201 Created` con `DesignacionResponseDTO` conteniendo datos del docente, cargo, resolución, unidad y fechas para habilitar la impresión en el cliente.

---

## Starting Point

| Componente | Detalle |
|---|---|
| **Controlador** | `com.atlas.atlas_backend.designacion.DesignacionController` |
| **Servicio** | `com.atlas.atlas_backend.designacion.DesignacionService` |
| **Repositorio** | `com.atlas.atlas_backend.designacion.DesignacionRepository` |
| **Entidades** | `com.atlas.atlas_backend.designacion.Designacion`, `com.atlas.atlas_backend.usuario.Docente`, `com.atlas.atlas_backend.cargo.Cargo` |
| **Notificación** | `com.atlas.atlas_backend.notification.EmailService` |

---

## Acceptance Criteria

### Scenario 1: Registro exitoso de designación con cargo por Administrador Global (Happy Path)
* **Given** un docente existente en el sistema con estado `"Inactivo"`
* **And** un cargo existente en estado `"Libre"`
* **When** un usuario autenticado con rol `ADMIN_GLOBAL` envía una petición `POST /api/designaciones` con:
  * `docenteId: 10`
  * `cargoId: 25`
  * `nroResolucion: 1024`
  * `fechaInicio: "2026-03-01"`
  * `fechaFin: "2027-02-28"`
* **Then** el sistema responde con HTTP `201 Created`
* **And** la designación queda registrada en base de datos con estado `"Activo"`
* **And** el cargo pasa a estado `"Asignado"` con la nueva designación vinculada
* **And** el estado del docente pasa a `"Activo"` y su usuario queda habilitado
* **And** se dispara una notificación por correo electrónico al docente.

### Scenario 2: Registro exitoso de designación con fecha fin indefinida (Campo vacío)
* **Given** un docente existente en el sistema
* **And** un cargo en estado `"Libre"`
* **When** un `ADMIN_GLOBAL` envía una petición `POST /api/designaciones` omitiendo el campo `fechaFin` (valor `null`)
* **Then** el sistema responde con HTTP `201 Created`
* **And** la designación se registra con `designacionFechaFin` igual a `null`
* **And** el docente y cargo quedan activados satisfactoriamente.

### Scenario 3: Registro exitoso por Administrador de Unidad sobre cargo de su unidad
* **Given** un Administrador de Unidad asignado a la unidad académica `ID = 1`
* **And** un cargo perteneciente a la unidad académica `ID = 1` en estado `"Libre"`
* **When** el Administrador de Unidad envía una petición `POST /api/designaciones` para dicho cargo y un docente válido
* **Then** el sistema valida la coincidencia de unidad y responde HTTP `201 Created`.

### Scenario 4: Rechazo por Administrador de Unidad operando sobre cargo de otra unidad
* **Given** un Administrador de Unidad asignado a la unidad académica `ID = 1`
* **And** un cargo perteneciente a la unidad académica `ID = 2`
* **When** el Administrador de Unidad intenta registrar una designación asociando dicho cargo
* **Then** el sistema rechaza la operación con HTTP `403 Forbidden` y el mensaje `"No tiene permisos para registrar designaciones en cargos de otra unidad"`.

### Scenario 5: Rechazo cuando el docente no existe (Error 404)
* **Given** un identificador de docente inexistente en el sistema
* **When** el administrador envía la petición de registro con dicho `docenteId`
* **Then** el sistema responde con HTTP `404 Not Found` y el mensaje `"Docente no encontrado"`.

### Scenario 6: Rechazo cuando el cargo no existe (Error 404)
* **Given** un identificador de cargo inexistente en el sistema
* **When** el administrador envía la petición de registro con dicho `cargoId`
* **Then** el sistema responde con HTTP `404 Not Found` y el mensaje `"Cargo no encontrado"`.

### Scenario 7: Rechazo cuando el cargo ya está asignado o no está "Libre" (Error 400)
* **Given** un cargo existente cuyo estado es `"Asignado"` o `"Licencia"`
* **When** el administrador intenta registrar una nueva designación para dicho cargo
* **Then** el sistema responde con HTTP `400 Bad Request` indicando que el cargo no está disponible para asignación.

### Scenario 8: Rechazo por duplicidad de número de resolución para el mismo docente (Error 400)
* **Given** un docente que ya cuenta con una designación registrada con el número de resolución `1024`
* **When** el administrador intenta registrar una nueva designación para dicho docente con el mismo número de resolución `1024`
* **Then** el sistema responde con HTTP `400 Bad Request` y el mensaje `"El docente ya posee una designación registrada con el número de resolución indicado"`.

### Scenario 9: Rechazo por inconsistencia de fechas (`fechaFin < fechaInicio`)
* **When** se envía una petición con `fechaInicio: "2026-10-01"` y `fechaFin: "2026-05-01"`
* **Then** el sistema responde con HTTP `400 Bad Request` y el mensaje `"La fecha de finalización no puede ser anterior a la fecha de inicio"`.

### Scenario 10: Rechazo por rol Auditor en operación de escritura (Segregación de funciones)
* **Given** un usuario autenticado con rol `AUDITOR`
* **When** intenta enviar una petición `POST /api/designaciones`
* **Then** el sistema rechaza la solicitud con HTTP `403 Forbidden`.

---

## Test Plan

| ID | Tipo | Escenario | Resultado Esperado |
|---|---|---|---|
| T1 | Unit | Registro exitoso completo por ADMIN_GLOBAL | 201 Created, designación activa, cargo asignado, docente activo |
| T2 | Unit | Registro con fechaFin nula (designación abierta) | 201 Created, fechaFin null |
| T3 | Unit | Registro por ADMIN_UNIDAD en su propia unidad | 201 Created |
| T4 | Unit | Intento de ADMIN_UNIDAD sobre cargo de otra unidad | 403 Forbidden |
| T5 | Unit | Docente inexistente | 404 Not Found |
| T6 | Unit | Cargo inexistente | 404 Not Found |
| T7 | Unit | Cargo previamente asignado | 400 Bad Request |
| T8 | Unit | Número de resolución duplicado para el mismo docente | 400 Bad Request |
| T9 | Unit | Fecha fin anterior a fecha inicio | 400 Bad Request |
| T10 | Integration | Intento de POST con rol AUDITOR | 403 Forbidden |
| T11 | Unit | Falla de envío SMTP de email | 201 Created (fail-safe logueado) |

---

## Post-implementation Validation

1. `mvn test -Dtest=DesignacionServiceTest`
2. `mvn test -Dtest=DesignacionControllerTest`
3. `mvn clean verify`
4. Validación manual mediante API client (Swagger / curl / Postman).

---

## Non-functional notes

- **Seguridad**: Regla fail-closed para `ADMIN_UNIDAD`. Auditor restringido a solo lectura.
- **Atomicidad Transaccional**: Si la actualización de la designación, cargo o docente falla, se debe aplicar rollback integral.
- **Asincronía de Notificaciones**: Las notificaciones por correo deben enviarse mediante `@Async` para no degradar el tiempo de respuesta del endpoint.
