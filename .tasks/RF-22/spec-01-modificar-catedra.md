# User Story — spec-01: Modificar los datos de una cátedra

---

## User Story Description

Esta especificación cubre la funcionalidad del backend para modificar los datos de una cátedra existente en el sistema Atlas (RF-22). Permite a los Administradores Globales y Administradores de Unidad actualizar los atributos propios de una cátedra registrada (`nombre`, `tipologia` de 1 a 4 y `directorId`) mediante `PUT /api/catedras/{id}`, manteniendo inmutables su `materia` asociada y su `estado`. El sistema verifica que la cátedra exista previamente y se encuentre en estado `"Activa"`, que el Administrador de Unidad solo opere sobre cátedras pertenecientes a materias de su propia unidad académica, que el docente asignado como director exista y esté en estado `"Activo"`, y que el nuevo nombre no colisione con el de otra cátedra dentro de la misma materia excluyendo el propio registro (`id != catedraId`).

---

## Story (INVEST)

```
**As an** Administrador Global o Administrador de Unidad,  
**I want to** modificar los datos de una cátedra existente (nombre, tipología y director de cátedra),  
**So that** la información organizativa y académica de la cátedra se mantenga actualizada y refleje los cambios docentes o curriculares, preservando la integridad de su materia y estado.
```

---

## Analysis

### Approach
1. **Endpoint REST**: Se expone `PUT /api/catedras/{id}` que recibe el identificador de la cátedra en la ruta y un payload JSON `CatedraUpdateRequestDTO` con los campos editables (`nombre`, `tipologia`, `directorId`).
2. **Inmutabilidad de Materia y Estado**:
   - Tanto la `materia` asociada como el `estado` de la cátedra permanecen inmutables a través de este endpoint.
3. **Validación de existencia y estado de la Cátedra**:
   - Se busca la cátedra por `id`. Si no existe, se retorna HTTP `404 Not Found` (`"Cátedra no encontrada"`).
   - Se verifica que la cátedra se encuentre en estado `"Activa"` (`"Activa".equalsIgnoreCase(catedra.getEstado())`). Si está `"De Baja"`, se rechaza la modificación con HTTP `400 Bad Request` (`"No se puede modificar una cátedra que no se encuentra en estado activa"`).
4. **Validación de Autorización por Unidad**:
   - Si el usuario autenticado tiene rol `ADMIN_UNIDAD`, se verifica que la materia asociada a la cátedra pertenezca a la misma unidad académica del administrador. Si no coincide, se retorna HTTP `403 Forbidden` (`"No tiene permisos para modificar cátedras en materias de otra unidad"`).
   - Si el usuario tiene rol `ADMIN_GLOBAL`, puede modificar cátedras de cualquier unidad académica.
5. **Validaciones de negocio**:
   - `nombre`: obligatorio (`@NotBlank`), longitud máxima 50 caracteres (`@Size(max = 50)`).
   - `tipologia`: obligatorio (`@NotNull`), entero entre 1 y 4 (`@Min(1)`, `@Max(4)`), correspondiente a las tipologías I a IV en números romanos.
   - `directorId`: obligatorio (`@NotNull`), debe existir en la base de datos (`404 Not Found` `"Docente no encontrado"`) y encontrarse en estado `"Activo"` (`400 Bad Request` `"El docente debe estar en estado activo"`). Un docente puede dirigir más de una cátedra simultáneamente (según RF-21).
   - **Unicidad de nombre en la materia (excluyendo el propio ID)**: No debe existir *otra* cátedra (`id != catedraId`) con el mismo nombre (case-insensitive) para la misma materia (`existsByNombreIgnoreCaseAndMateria_IdAndIdNot(nombre, materiaId, id)`). Si existe, se retorna HTTP `400 Bad Request` (`"Ya existe una cátedra con ese nombre para la materia seleccionada"`).
6. **Persistencia y Respuesta**:
   - Se actualizan los campos `nombre`, `tipologia` y `director` en la entidad `Catedra` y se persiste dentro de una transacción (`@Transactional`).
   - Retorna HTTP `200 OK` con `CatedraResponseDTO` conteniendo los datos actualizados.

---

## Starting Point

| Componente | Detalle / Ruta |
| --- | --- |
| **Entidad** | [Catedra.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/Catedra.java) |
| **Repositorio** | [CatedraRepository.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/CatedraRepository.java) |
| **Servicio** | [CatedraService.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/CatedraService.java) |
| **Controlador** | [CatedraController.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/CatedraController.java) |
| **DTOs** | `CatedraUpdateRequestDTO` (a crear), [CatedraResponseDTO.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/dto/CatedraResponseDTO.java) |

---

## Context

- Requerimiento en [raw-requirement.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-22/raw-requirement.md)
- Decisiones técnicas y de negocio cerradas en [open-questions.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-22/open-questions.md) (B1: Opción A, B2: Opción A, R1: Opción A, R2: Opción A, M1: Opción A)
- Diagramas de solución en [solution-diagrams.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-22/solution-diagrams.md)

---

## Acceptance Criteria

### Scenario 1: Modificación exitosa de una cátedra por Administrador Global (Happy Path)
- **Given** un usuario autenticado con rol `ADMIN_GLOBAL`
- **And** existe una cátedra con ID `10` en estado `"Activa"`, nombre `"Cátedra A"`, tipología `1`, director con ID `5` y materia con ID `1`
- **And** existe un docente activo con ID `12`
- **And** no existe otra cátedra con nombre `"Cátedra A Renovada"` en la materia con ID `1`
- **When** se envía una petición `PUT /api/catedras/10` con el cuerpo:
  ```json
  {
    "nombre": "Cátedra A Renovada",
    "tipologia": 2,
    "directorId": 12
  }
  ```
- **Then** el sistema actualiza los datos de la cátedra en la base de datos preservando su `materia` (`id: 1`) y su `estado` (`"Activa"`)
- **And** responde con HTTP status `200 OK`
- **And** el cuerpo de la respuesta (`CatedraResponseDTO`) contiene `id: 10`, `nombre: "Cátedra A Renovada"`, `tipologia: 2`, `directorId: 12`, `materiaId: 1` y `estado: "Activa"`.

---

### Scenario 2: Modificación exitosa manteniendo el mismo nombre de la cátedra (Exclusión de propio ID)
- **Given** un usuario autenticado con rol `ADMIN_GLOBAL`
- **And** existe una cátedra activa con ID `10`, nombre `"Cátedra Única"`, tipología `1` y materia con ID `1`
- **And** existe un docente activo con ID `12`
- **When** se envía una petición `PUT /api/catedras/10` manteniendo `nombre: "Cátedra Única"` y cambiando `tipologia: 3` y `directorId: 12`
- **Then** el sistema no reporta conflicto de unicidad consigo misma
- **And** responde con HTTP status `200 OK` con `tipologia: 3`.

---

### Scenario 3: Modificación exitosa por Administrador de Unidad dentro de su propia unidad académica
- **Given** un usuario autenticado con rol `ADMIN_UNIDAD` asignado a la Unidad Académica con ID `5`
- **And** existe una cátedra activa con ID `20` cuya materia pertenece a la Unidad Académica con ID `5`
- **And** existe un docente activo con ID `15`
- **When** el administrador envía una petición `PUT /api/catedras/20` con datos válidos
- **Then** el sistema actualiza la cátedra y responde con HTTP status `200 OK`.

---

### Scenario 4: Rechazo cuando la cátedra a modificar no existe (Unhappy Path)
- **Given** un usuario autenticado con rol `ADMIN_GLOBAL` o `ADMIN_UNIDAD`
- **And** no existe ninguna cátedra con ID `9999`
- **When** se envía una petición `PUT /api/catedras/9999` con un payload válido
- **Then** el sistema responde con HTTP status `404 Not Found`
- **And** el mensaje de error indica `"Cátedra no encontrada"`.

---

### Scenario 5: Rechazo cuando la cátedra se encuentra dada de baja (Unhappy Path - R2)
- **Given** existe una cátedra con ID `10` cuyo estado es `"De Baja"`
- **When** se envía una petición `PUT /api/catedras/10` con datos válidos
- **Then** el sistema rechaza la modificación con HTTP status `400 Bad Request`
- **And** el mensaje de error indica que solo se pueden modificar cátedras en estado activa.

---

### Scenario 6: Rechazo por colisión de nombre con otra cátedra de la misma materia (Unhappy Path - B2)
- **Given** existen dos cátedras en la materia con ID `1`: cátedra ID `10` (`"Cátedra Mañana"`) y cátedra ID `11` (`"Cátedra Tarde"`, activa)
- **When** se envía una petición `PUT /api/catedras/11` cambiando su nombre a `"Cátedra Mañana"`
- **Then** el sistema rechaza la modificación con HTTP status `400 Bad Request`
- **And** el mensaje de error indica que ya existe una cátedra con ese nombre para la materia seleccionada.

---

### Scenario 7: Rechazo por nuevo director inexistente o en estado inactivo (Unhappy Path - R2)
- **Given** existe una cátedra activa con ID `10`
- **And** existe un docente con ID `18` en estado `"De Baja"` (inactivo)
- **When** se envía una petición `PUT /api/catedras/10` asignando `directorId: 18`
- **Then** el sistema responde con HTTP status `400 Bad Request` indicando que el docente debe estar en estado activo
- **And** si se envía un `directorId` inexistente, responde con HTTP status `404 Not Found` (`"Docente no encontrado"`).

---

### Scenario 8: Rechazo cuando un Administrador de Unidad intenta modificar una cátedra de otra unidad (Forbidden - R1)
- **Given** un usuario autenticado con rol `ADMIN_UNIDAD` asignado a la Unidad Académica con ID `3`
- **And** existe una cátedra activa con ID `30` asociada a una materia de la Unidad Académica con ID `9`
- **When** el usuario envía una petición `PUT /api/catedras/30`
- **Then** el sistema no modifica la cátedra y responde con HTTP status `403 Forbidden`
- **And** el mensaje de error indica que no tiene permisos para modificar cátedras en materias de otra unidad.

---

### Scenario 9: Validación de formato y rango de campos (Bean Validation)
- **Given** una petición `PUT /api/catedras/10` con nombre en blanco, nombre mayor a 50 caracteres, `directorId` nulo o tipología fuera del rango `1..4`
- **When** el controlador valida el payload de entrada (`@Valid`)
- **Then** el sistema responde con HTTP status `400 Bad Request` detallando los errores de validación.

---

## Test Plan

### Context
Tests unitarios en `CatedraServiceTest` (con Mockito) y tests de controlador en `CatedraControllerTest` (con `MockMvc` y `@WithMockUser`), verificando todos los flujos de éxito, validación de estado activo en cátedra y docente, unicidad excluyendo propio ID y seguridad por rol/unidad.

### Scenarios Table

| # | Tipo | Escenario | Resultado Esperado |
|---|---|---|---|
| 1 | Éxito | Admin Global modifica nombre, tipología y director de una cátedra activa | `200 OK` + `CatedraResponseDTO` actualizado |
| 2 | Éxito | Modificación conservando el mismo nombre de la cátedra | `200 OK` + `CatedraResponseDTO` actualizado |
| 3 | Éxito | Admin Unidad modifica cátedra perteneciente a su propia unidad | `200 OK` + `CatedraResponseDTO` actualizado |
| 4 | Error | Cátedra con `id` inexistente | `404 Not Found` ("Cátedra no encontrada") |
| 5 | Error | Cátedra en estado `"De Baja"` | `400 Bad Request` ("No se puede modificar una cátedra...") |
| 6 | Error | Nombre duplicado con otra cátedra de la misma materia (`id != actual`) | `400 Bad Request` ("Ya existe una cátedra con ese nombre...") |
| 7 | Error | Nuevo director en estado inactivo | `400 Bad Request` ("El docente debe estar en estado activo") |
| 8 | Error | Nuevo director no encontrado por ID | `404 Not Found` ("Docente no encontrado") |
| 9 | Seguridad | Admin Unidad intenta modificar cátedra de otra unidad académica | `403 Forbidden` |
| 10 | Validación | Campos obligatorios faltantes o tipología fuera del rango 1..4 | `400 Bad Request` |

---

## Post-implementation Validation

1. `.\mvnw test -Dtest=CatedraServiceTest`
2. `.\mvnw test -Dtest=CatedraControllerTest`
3. Verificación de compilación global: `.\mvnw test-compile`

---

## Non-functional notes

- **Seguridad**: Endpoint protegido con `@PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')")`.
- **Integridad Transaccional**: Método anotado con `@Transactional` en `CatedraService`.
