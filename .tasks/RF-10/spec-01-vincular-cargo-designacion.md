# User Story — spec-01: Vincular un cargo a una designación de un docente

---

## User Story Description

Esta especificación cubre la funcionalidad del backend para vincular un cargo académico disponible (en estado `"Libre"`) a una designación de un docente en el sistema Atlas (RF-10). Permite a los Administradores Globales (`ADMIN_GLOBAL`) y Administradores de Unidad (`ADMIN_UNIDAD`) seleccionar un cargo previamente creado en estado `"Libre"` y asociarlo formalmente a la designación del docente —tanto en un paso posterior sobre una designación creada sin cargo (por ejemplo, luego de acudir a Gestión > Crear Cargo en RF-17) mediante `PATCH /api/designaciones/{id}/cargo`, como durante el alta en `POST /api/designaciones` (Decisión B1)—.

Al completarse la vinculación:
1. Se verifica que la designación exista y que no posea ya un cargo vinculado (`400 Bad Request` si ya tiene uno, Decisión B2).
2. Se verifica que el cargo exista (`404 Not Found` si no existe) y se encuentre estrictamente en estado `"Libre"` (`400 Bad Request` en caso contrario).
3. Si el actor es `ADMIN_UNIDAD`, se valida que el cargo pertenezca a su misma unidad académica (`403 Forbidden` si pertenece a otra unidad, Decisión R3).
4. Se vincula la designación al cargo (`cargo.setDesignacion(designacion)`) y el cargo cambia su estado a `"Asignado"`.
5. No se dispara notificación por correo electrónico en este paso, ya que el envío del correo queda diferido para la confirmación final del trámite en RF-11 (Asignar funciones a un cargo perteneciente a una designación, Decisión R1).
6. Se retorna `DesignacionResponseDTO` con la información consolidada de la designación, el docente y el cargo vinculado.

---

## Story (INVEST)

```
**As an** Administrador Global o Administrador de Unidad,  
**I want to** vincular un cargo existente en estado "Libre" a la designación de un docente,  
**So that** el cargo quede formalmente en estado "Asignado" y vinculado a la designación dentro del trámite académico.
```

---

## Analysis

### Approach
1. **Consulta de Cargos Disponibles (Decisión R2)**:
   - El frontend consulta los cargos elegibles en estado `"Libre"` reutilizando `GET /api/cargos?estado=Libre` (RF-06), el cual filtra automáticamente por la unidad académica del `ADMIN_UNIDAD` autenticado y excluye cargos dados de baja.
   - Si el administrador necesita un cargo nuevo, el botón `"Crear Cargo"` en la UI realiza la redirección hacia Gestión > Alta de Cargo (`POST /api/cargos`, RF-17, Decisión M1), cuyo cargo creado en estado `"Libre"` queda disponible inmediatamente para su vinculación.
2. **Endpoint de Vinculación (Decisión B1)**:
   - Exponer `PATCH /api/designaciones/{id}/cargo` (recibiendo `DesignacionVincularCargoRequest` con `@NotNull Integer cargoId`), además de mantener el soporte opcional de `cargoId` en `POST /api/designaciones`.
3. **Control de Acceso y Roles**:
   - Autorizar a roles `ADMIN_GLOBAL`, `ADMIN_UNIDAD`, `ADMIN_DE_UNIDAD` y `ADMINISTRADOR_DE_UNIDAD`.
   - Denegar acceso al rol `AUDITOR` u otros roles no administrativos con HTTP `403 Forbidden`.
4. **Validaciones de Negocio**:
   - **Existencia de la Designación**: Si el `id` de la designación no existe en la base de datos, retornar HTTP `404 Not Found` (`"Designación no encontrada"`).
   - **Designación sin Cargo Previo (Decisión B2)**: Si la designación ya tiene un cargo vinculado en la tabla `cargos`, rechazar con HTTP `400 Bad Request` (`"La designación ya posee un cargo vinculado"`).
   - **Existencia del Cargo**: Si el `cargoId` no existe en la base de datos, retornar HTTP `404 Not Found` (`"Cargo no encontrado"`).
   - **Estado del Cargo**: El cargo debe encontrarse estrictamente en estado `"Libre"`. Si su estado es `"Asignado"`, `"Licencia"` o `"Dado de baja"`, rechazar con HTTP `400 Bad Request` (`"El cargo ya se encuentra asignado o en licencia"`).
   - **Alcance por Unidad Académica (Decisión R3)**: Si el usuario autenticado es `ADMIN_UNIDAD`, validar que `cargo.getUnidad().getUnidadId()` coincida con su unidad asignada. Si difiere, rechazar con HTTP `403 Forbidden` (`"No tiene permisos para vincular cargos de otra unidad"`).
5. **Persistencia Transaccional (`@Transactional`)**:
   - Asociar la designación al cargo: `cargo.setDesignacion(designacion)` y actualizar `cargo.setEstado("Asignado")`.
6. **Diferimiento de Notificación por Email (Decisión R1)**:
   - El correo electrónico de notificación al docente **no** se envía en RF-09 ni en RF-10; se enviará únicamente cuando se confirme todo el proceso en **RF-11** (Asignar funciones a un cargo perteneciente a una designación).
7. **Respuesta**:
   - Retornar HTTP `200 OK` en `PATCH /api/designaciones/{id}/cargo` (o `201 Created` si se vinculó directamente en `POST /api/designaciones`) con `DesignacionResponseDTO` incluyendo el resumen del docente y del cargo asignado.

---

## Starting Point

| Componente | Detalle |
|---|---|
| **Controlador** | `src/main/java/com/atlas/atlas_backend/designacion/DesignacionController.java`<br>`src/main/java/com/atlas/atlas_backend/cargo/CargoController.java` |
| **Servicio** | `src/main/java/com/atlas/atlas_backend/designacion/DesignacionService.java` |
| **Repositorio** | `src/main/java/com/atlas/atlas_backend/designacion/DesignacionRepository.java`<br>`src/main/java/com/atlas/atlas_backend/cargo/CargoRepository.java` |
| **DTOs** | `src/main/java/com/atlas/atlas_backend/designacion/DesignacionVincularCargoRequest.java`<br>`src/main/java/com/atlas/atlas_backend/designacion/DesignacionResponseDTO.java` |
| **Entidades** | `com.atlas.atlas_backend.designacion.Designacion`, `com.atlas.atlas_backend.cargo.Cargo`, `com.atlas.atlas_backend.usuario.Docente` |

---

## Context

Ver [`raw-requirement.md`](./raw-requirement.md) para el requerimiento original y [`open-questions.md`](./open-questions.md) para las decisiones acordadas. Relacionado directamente con [RF-09](../RF-09/index-spec.md) (Registrar designaciones) y [RF-17](../RF-17/index-spec.md) (Alta de cargo).

---

## Acceptance Criteria

### Scenario 1: Vinculación exitosa de un cargo en estado "Libre" a una designación existente por Administrador Global (Happy Path)
* **Given** una designación existente con `id = 100` (asociada al docente `id = 10`) que aún no posee cargo vinculado
* **And** un cargo existente con `id = 25` cuyo estado es `"Libre"`
* **When** un usuario autenticado con rol `ADMIN_GLOBAL` envía una petición `PATCH /api/designaciones/100/cargo` con:
  * `cargoId: 25`
* **Then** el sistema responde con HTTP `200 OK`
* **And** el cargo `25` queda vinculado a la designación `100` (`designacion_id = 100`)
* **And** el estado del cargo `25` pasa a `"Asignado"`
* **And** no se envía notificación por correo electrónico en esta etapa (diferida a RF-11)
* **And** la respuesta contiene `DesignacionResponseDTO` con los datos de la designación, del docente y del cargo vinculado.

### Scenario 2: Vinculación exitosa por Administrador de Unidad sobre un cargo de su propia unidad
* **Given** un Administrador de Unidad autenticado perteneciente a la unidad académica `ID = 1`
* **And** una designación existente con `id = 100` sin cargo vinculado
* **And** un cargo con `id = 25` en estado `"Libre"` perteneciente a la unidad académica `ID = 1`
* **When** el Administrador de Unidad envía `PATCH /api/designaciones/100/cargo` con `cargoId: 25`
* **Then** el sistema valida la pertenencia del cargo a la unidad `ID = 1`, vincula el cargo pasándolo a `"Asignado"`, y responde con HTTP `200 OK`.

### Scenario 3: Rechazo cuando el Administrador de Unidad intenta vincular un cargo de otra unidad (Error 403 — Decisión R3)
* **Given** un Administrador de Unidad autenticado perteneciente a la unidad académica `ID = 1`
* **And** una designación existente con `id = 100` sin cargo vinculado
* **And** un cargo con `id = 30` en estado `"Libre"` perteneciente a la unidad académica `ID = 2`
* **When** el Administrador de Unidad envía `PATCH /api/designaciones/100/cargo` con `cargoId: 30`
* **Then** el sistema no vincula el cargo ni modifica su estado
* **And** responde con HTTP `403 Forbidden` y el mensaje `"No tiene permisos para vincular cargos de otra unidad"`.

### Scenario 4: Rechazo cuando el cargo elegido no se encuentra en estado "Libre" (Error 400)
* **Given** una designación existente con `id = 100` sin cargo vinculado
* **And** un cargo existente con `id = 25` cuyo estado es `"Asignado"`, `"Licencia"` o `"Dado de baja"`
* **When** un administrador envía `PATCH /api/designaciones/100/cargo` con `cargoId: 25`
* **Then** el sistema rechaza la vinculación con HTTP `400 Bad Request` y el mensaje `"El cargo ya se encuentra asignado o en licencia"`.

### Scenario 5: Rechazo cuando el cargo no existe (Error 404 — Solo se pueden asignar cargos ya creados)
* **Given** una designación existente con `id = 100`
* **And** un identificador de cargo `999` que no existe en el sistema
* **When** un administrador envía `PATCH /api/designaciones/100/cargo` con `cargoId: 999`
* **Then** el sistema responde con HTTP `404 Not Found` y el mensaje `"Cargo no encontrado"`.

### Scenario 6: Rechazo cuando la designación no existe (Error 404)
* **Given** un identificador de designación `999` que no existe en el sistema
* **When** un administrador envía `PATCH /api/designaciones/999/cargo` con `cargoId: 25`
* **Then** el sistema responde con HTTP `404 Not Found` y el mensaje `"Designación no encontrada"`.

### Scenario 7: Rechazo cuando la designación ya posee un cargo vinculado (Error 400 — Decisión B2)
* **Given** una designación existente con `id = 100` que ya tiene un cargo vinculado en estado `"Asignado"`
* **And** otro cargo con `id = 26` en estado `"Libre"`
* **When** un administrador envía `PATCH /api/designaciones/100/cargo` con `cargoId: 26`
* **Then** el sistema rechaza la operación con HTTP `400 Bad Request` y el mensaje `"La designación ya posee un cargo vinculado"`.

### Scenario 8: Rechazo por rol Auditor en operación de vinculación (Error 403)
* **Given** un usuario autenticado con rol `AUDITOR`
* **When** intenta enviar una petición `PATCH /api/designaciones/100/cargo`
* **Then** el sistema rechaza la petición con HTTP `403 Forbidden`.

---

## Test Plan

#### Context
Base de datos o mocks configurados con usuarios (`ADMIN_GLOBAL`, `ADMIN_UNIDAD`, `AUDITOR`), unidades académicas, docentes, designaciones sin cargo y con cargo, y cargos en estados `"Libre"`, `"Asignado"` y `"Licencia"`.

#### Scenarios

| ID | Tipo | Escenario | Rol | Resultado Esperado |
|---|---|---|---|---|
| T1 | Unit / Integration | Vinculación exitosa de cargo `"Libre"` a designación sin cargo | `ADMIN_GLOBAL` | `200 OK`, cargo en `"Asignado"`, sin envío de email (diferido a RF-11) |
| T2 | Unit / Integration | Vinculación exitosa por `ADMIN_UNIDAD` en cargo de su unidad | `ADMIN_UNIDAD` | `200 OK`, cargo en `"Asignado"` |
| T3 | Unit / Integration | Rechazo a `ADMIN_UNIDAD` por cargo de otra unidad | `ADMIN_UNIDAD` | `403 Forbidden` |
| T4 | Unit / Integration | Rechazo por cargo en estado `"Asignado"` o `"Licencia"` | `ADMIN_GLOBAL` | `400 Bad Request` |
| T5 | Unit / Integration | Rechazo por cargo inexistente (`cargoId` no creado) | `ADMIN_GLOBAL` | `404 Not Found` |
| T6 | Unit / Integration | Rechazo por designación inexistente | `ADMIN_GLOBAL` | `404 Not Found` |
| T7 | Unit / Integration | Rechazo cuando la designación ya tiene un cargo vinculado | `ADMIN_GLOBAL` | `400 Bad Request` |
| T8 | Integration | Rechazo por `cargoId` nulo en el payload (`@Valid`) | `ADMIN_GLOBAL` | `400 Bad Request` |
| T9 | Integration | Rechazo a usuario con rol `AUDITOR` | `AUDITOR` | `403 Forbidden` |

---

## Post-implementation Validation

1. `mvn test -Dtest=DesignacionServiceTest`
2. `mvn test -Dtest=DesignacionControllerTest`
3. `mvn clean verify`

---

## Non-functional notes

- **Seguridad y Segregación de Funciones**: Solo `ADMIN_GLOBAL` y `ADMIN_UNIDAD` pueden vincular cargos a designaciones. `ADMIN_UNIDAD` está restringido a cargos pertenecientes a su unidad académica.
- **Atomicidad Transaccional**: La vinculación del cargo (`estado = "Asignado"`, `designacion_id`) debe ejecutarse bajo una transacción `@Transactional`.
- **Diferimiento de Notificación**: Conforme a la Decisión R1, el envío de correo electrónico al docente no se realiza al crear la designación ni al vincular el cargo, sino al finalizar el proceso completo en RF-11.
