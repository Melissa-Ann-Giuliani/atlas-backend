# User Story — spec-01: Baja de una cátedra

---

## User Story Description

Esta especificación cubre la funcionalidad del backend para generar la baja lógica de una cátedra existente en el sistema Atlas (RF-23). Permite a los Administradores Globales (`ADMIN_GLOBAL`) y Administradores de Unidad (`ADMIN_UNIDAD`) dar de baja una cátedra mediante el endpoint de ciclo de vida `PATCH /api/catedras/{id}/baja`. La baja cambia su estado de `"Activa"` a `"De baja"`, preservando el histórico y la integridad de los datos asociados (como su materia y su director docente asignado).

El sistema verifica que:
1. La cátedra exista previamente en la base de datos (HTTP `404 Not Found`).
2. El actor tenga permisos sobre la cátedra: si es `ADMIN_UNIDAD`, la materia asociada a la cátedra debe pertenecer a su unidad académica asignada (HTTP `403 Forbidden` si pertenece a otra unidad).
3. La cátedra no se encuentre ya en estado `"De baja"` (HTTP `400 Bad Request`).
4. La cátedra no posea comisiones u ofertas académicas activas asociadas (HTTP `409 Conflict`). Si no tiene dependencias activas, se procede con la baja lógica exitosa retornando HTTP `200 OK` con `CatedraResponseDTO` actualizado.

---

## Story (INVEST)

```
**As an** Administrador Global o Administrador de Unidad,  
**I want to** dar de baja una cátedra existente que no posea comisiones activas asociadas,  
**So that** la cátedra deje de estar operativa para nuevos ciclos académicos preservando su registro histórico, su director asignado y la coherencia organizativa de la unidad académica.
```

---

## Analysis

### Approach
1. **Endpoint REST de ciclo de vida**: Se expone `PATCH /api/catedras/{id}/baja` que recibe el identificador `id` de la cátedra como `@PathVariable` y no requiere cuerpo de petición (Request Body vacío).
2. **Validación de existencia**:
   - Se busca la cátedra por su identificador único. Si no existe, se retorna HTTP `404 Not Found` (`"Cátedra no encontrada"`).
3. **Validación de Autorización y Alcance por Unidad Académica**:
   - Si el usuario autenticado tiene rol `ADMIN_UNIDAD`, se valida que la materia asociada a la cátedra pertenezca a la misma unidad académica asignada a dicho administrador.
   - Si la unidad no coincide, se retorna HTTP `403 Forbidden` (`"No tiene permisos para dar de baja cátedras en materias de otra unidad"`).
   - Si el usuario tiene rol `ADMIN_GLOBAL`, puede operar sobre cátedras de cualquier unidad académica.
4. **Validación de Estado Actual**:
   - Se verifica el estado actual de la cátedra:
     - Si ya se encuentra en estado `"De baja"` (`"De baja".equalsIgnoreCase(catedra.getEstado())`), se rechaza la operación con HTTP `400 Bad Request` (`"La cátedra ya se encuentra dada de baja"`).
5. **Validación de Integridad y Dependencias Activas**:
   - Se valida que la cátedra no tenga comisiones activas vinculadas (por ejemplo mediante `comisionRepository.existsByCatedra_IdAndEstadoIgnoreCase(id, "ACTIVA")` o mecanismo equivalente).
   - Si existen comisiones activas, se rechaza la baja con HTTP `409 Conflict` (`"No se puede dar de baja la cátedra porque tiene comisiones activas asociadas"`).
6. **Persistencia y Preservación Histórica**:
   - Se actualiza el atributo `estado` de la cátedra a `"De baja"`.
   - Se preservan intactos los vínculos con `materia`, `nombre`, `tipologia` y `director` (el director asignado se mantiene para auditoría y consulta histórica del último titular).
   - La operación se ejecuta de manera atómica dentro de una transacción (`@Transactional`).
7. **Respuesta**:
   - Retorna HTTP `200 OK` con el payload de `CatedraResponseDTO` reflejando `estado: "De baja"`.

---

## Starting Point

| Componente | Detalle / Ruta |
| --- | --- |
| **Entidad** | [Catedra.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/Catedra.java) |
| **Repositorio** | [CatedraRepository.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/CatedraRepository.java) |
| **Servicio** | [CatedraService.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/CatedraService.java) |
| **Controlador** | [CatedraController.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/CatedraController.java) |
| **DTO Respuesta** | [CatedraResponseDTO.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/dto/CatedraResponseDTO.java) |
| **Endpoint** | `PATCH /api/catedras/{id}/baja` |
| **Payload / Request** | Vacío (sin body) |
| **Respuesta** | HTTP `200 OK` con `CatedraResponseDTO` (`estado: "De baja"`) |

---

## Context

- Requerimiento en bruto: [raw-requirement.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-23/raw-requirement.md)
- Registro de Decisiones y Preguntas Abiertas: [open-questions.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-23/open-questions.md) (B1: Opción A, B2: Opción A, R1: Opción A, R2: Opción A, R3: Opción A, M1: Opción A)
- Requerimientos relacionados: [RF-21 Alta de Cátedra](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-21), [RF-22 Modificación de Cátedra](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-22), [RF-26 Baja de Materia](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-26).

---

## Acceptance Criteria

### Scenario 1: Baja exitosa de una cátedra por Administrador Global (Happy Path)
* **Given** un usuario autenticado con rol `ADMIN_GLOBAL`
* **And** una cátedra existente con ID `1`, en estado `"Activa"`, con nombre `"Cátedra A"`, tipología `1`, asociada a la materia `"Álgebra"` y dirigida por el docente con ID `10`
* **And** la cátedra no tiene comisiones activas asociadas
* **When** el administrador envía una solicitud `PATCH /api/catedras/1/baja`
* **Then** el sistema responde con HTTP `200 OK`
* **And** la cátedra en la base de datos queda con `estado = "De baja"`
* **And** el cuerpo de la respuesta contiene `CatedraResponseDTO` con `id: 1`, `estado: "De baja"`, `directorId: 10`, preservando su vinculación histórica
* **And** los demás datos (`nombre`, `tipologia`, `materiaId`) se mantienen inalterados.

### Scenario 2: Baja exitosa de una cátedra por Administrador de Unidad de su propia unidad académica
* **Given** un usuario autenticado con rol `ADMIN_UNIDAD` asignado a la unidad académica `1`
* **And** una cátedra existente con ID `2` en estado `"Activa"` perteneciente a una materia de la unidad académica `1`
* **And** la cátedra no tiene comisiones activas asociadas
* **When** el administrador envía una solicitud `PATCH /api/catedras/2/baja`
* **Then** el sistema responde con HTTP `200 OK`
* **And** la cátedra queda registrada en la base de datos con `estado = "De baja"`.

### Scenario 3: Rechazo de baja de cátedra por Administrador de Unidad sobre materia de otra unidad académica
* **Given** un usuario autenticado con rol `ADMIN_UNIDAD` asignado a la unidad académica `1`
* **And** una cátedra existente con ID `3` perteneciente a una materia de la unidad académica `2`
* **When** el administrador envía una solicitud `PATCH /api/catedras/3/baja`
* **Then** el sistema responde con HTTP `403 Forbidden`
* **And** el cuerpo de respuesta contiene el mensaje `"No tiene permisos para dar de baja cátedras en materias de otra unidad"`
* **And** el estado de la cátedra no sufre modificaciones.

### Scenario 4: Rechazo al intentar dar de baja una cátedra inexistente
* **Given** un usuario autenticado con rol `ADMIN_GLOBAL` o `ADMIN_UNIDAD`
* **And** no existe ninguna cátedra con ID `999` en el sistema
* **When** el administrador envía una solicitud `PATCH /api/catedras/999/baja`
* **Then** el sistema responde con HTTP `404 Not Found`
* **And** el mensaje de error indica `"Cátedra no encontrada"`.

### Scenario 5: Rechazo al intentar dar de baja una cátedra que ya se encuentra "De baja"
* **Given** un usuario autenticado con rol `ADMIN_GLOBAL` o `ADMIN_UNIDAD` autorizado
* **And** una cátedra con ID `4` cuyo estado actual ya es `"De baja"`
* **When** el usuario envía una solicitud `PATCH /api/catedras/4/baja`
* **Then** el sistema responde con HTTP `400 Bad Request`
* **And** el mensaje de error indica `"La cátedra ya se encuentra dada de baja"`
* **And** la cátedra permanece en estado `"De baja"` sin cambios adicionales.

### Scenario 6: Rechazo al intentar dar de baja una cátedra con comisiones activas asociadas
* **Given** un usuario autenticado con rol `ADMIN_GLOBAL` o `ADMIN_UNIDAD` autorizado
* **And** una cátedra con ID `5` en estado `"Activa"` que posee al menos una comisión en estado activo asociada
* **When** el usuario envía una solicitud `PATCH /api/catedras/5/baja`
* **Then** el sistema responde con HTTP `409 Conflict`
* **And** el mensaje de error indica `"No se puede dar de baja la cátedra porque tiene comisiones activas asociadas"`
* **And** el estado de la cátedra en la base de datos permanece `"Activa"`.

### Scenario 7: Rechazo de acceso por rol no autorizado
* **Given** un usuario autenticado con un rol no administrativo (por ejemplo `DOCENTE` o `ALUMNO`)
* **When** el usuario envía una solicitud `PATCH /api/catedras/1/baja`
* **Then** el sistema rechaza la petición con HTTP `403 Forbidden`.

