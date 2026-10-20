# User Story — spec-01: Alta de una cátedra

---

## User Story Description

Esta especificación cubre la funcionalidad completa del backend para registrar el alta de una cátedra en el sistema Atlas. Permite a los Administradores Globales y Administradores de Unidad dar de alta una cátedra asociada a una materia académica, asignándole una tipología y un docente como director de cátedra, validando la unicidad, las restricciones de exclusividad del director y los permisos correspondientes.

---

## Story (INVEST)

```
**As an** Administrador Global o Administrador de Unidad,  
**I want to** registrar una nueva cátedra ingresando su nombre, tipología, director y materia,  
**So that** la cátedra quede activa y disponible para la planificación docente y la gestión de cargos de la materia.
```

---

## Analysis

### Approach
1. **Endpoint REST**: Se expone `POST /api/catedras` que recibe un payload JSON (`CatedraRequestDTO`).
2. **Validaciones de negocio**:
   - `nombre`: no vacío, longitud máxima 50 caracteres.
   - `tipologia`: no nulo, entero positivo válido.
   - `materiaId`: debe existir en la base de datos.
   - `directorId`: debe existir en la base de datos como Docente activo.
   - **Unicidad de cátedra**: No debe existir previamente una cátedra con el mismo nombre para la misma materia (`existsByNombreIgnoreCaseAndMateriaId`).
   - **Exclusividad del director**: El docente seleccionado no debe ser director de otra cátedra existente, respetando el constraint `UNIQUE (catedra_director_id)` de la base de datos.
   - **Autorización por Unidad**: Si el usuario es `ADMIN_UNIDAD`, la materia debe pertenecer a su unidad académica asignada. Si es `ADMIN_GLOBAL`, puede crear cátedras en cualquier materia.
3. **Persistencia y Estado**:
   - Se guarda la nueva entidad `Catedra` con estado `"Activo"` (o `"Activa"` según definición en `open-questions.md`).
   - Retorna HTTP `201 Created` con `CatedraResponseDTO`.

---

## Starting Point

| Componente | Detalle / Ruta |
| --- | --- |
| **Entidad** | [Catedra.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/Catedra.java) |
| **Repositorio** | [CatedraRepository.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/CatedraRepository.java) |
| **Servicio** | [CatedraService.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/catedra/CatedraService.java) |
| **Controlador** | `com.atlas.atlas_backend.catedra.CatedraController` (a crear) |
| **DTOs** | `CatedraRequestDTO`, `CatedraResponseDTO` en `com.atlas.atlas_backend.catedra.dto` (a crear) |

---

## Context

- Requerimiento en [raw-requirement.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-21/raw-requirement.md)
- Decisiones técnicas y de negocio en [open-questions.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-21/open-questions.md)
- Diagramas de solución en [solution-diagrams.md](file:///c:/_dev/atlas/atlas-backend/.tasks/RF-21/solution-diagrams.md)

---

## Acceptance Criteria

### Scenario 1: Alta exitosa de cátedra por Administrador Global (Happy Path)
- **Given** un usuario autenticado con rol `ADMIN_GLOBAL`
- **And** existe una materia con ID `1`
- **And** existe un docente activo con ID `10` que no dirige ninguna cátedra
- **And** no existe una cátedra con nombre `"Cátedra A"` para la materia con ID `1`
- **When** se envía una petición `POST /api/catedras` con el cuerpo:
  ```json
  {
    "nombre": "Cátedra A",
    "tipologia": 1,
    "directorId": 10,
    "materiaId": 1
  }
  ```
- **Then** el sistema responde con HTTP status `201 Created`
- **And** el cuerpo de la respuesta contiene el ID generado, nombre `"Cátedra A"`, tipología `1`, estado `"Activo"`, director con ID `10` y materia con ID `1`
- **And** la cátedra queda registrada y persistida en la base de datos.

---

### Scenario 2: Alta exitosa de cátedra por Administrador de Unidad en materia de su propia unidad
- **Given** un usuario autenticado con rol `ADMIN_UNIDAD` asignado a la Unidad Académica con ID `5`
- **And** existe una materia con ID `2` perteneciente a la Unidad Académica con ID `5`
- **And** existe un docente activo con ID `11` sin cátedra asignada
- **When** el administrador envía una petición `POST /api/catedras` para dicha materia
- **Then** el sistema responde con HTTP status `201 Created`
- **And** la cátedra se crea con estado `"Activo"`.

---

### Scenario 3: Rechazo por cátedra ya existente en la misma materia (Unhappy Path)
- **Given** un usuario autenticado con rol `ADMIN_GLOBAL` o `ADMIN_UNIDAD`
- **And** ya existe una cátedra registrada con el nombre `"Cátedra B"` para la materia con ID `1`
- **When** se envía una petición `POST /api/catedras` con `nombre: "Cátedra B"` y `materiaId: 1`
- **Then** el sistema responde con HTTP status `400 Bad Request`
- **And** el mensaje de error indica que ya existe una cátedra con ese nombre para la materia seleccionada.

---

### Scenario 4: Rechazo por director ya asignado a otra cátedra (Unhappy Path - Unique Constraint)
- **Given** existe un docente con ID `12` que ya es director de otra cátedra registrada
- **When** se envía una petición `POST /api/catedras` con `directorId: 12`
- **Then** el sistema responde con HTTP status `400 Bad Request`
- **And** el mensaje de error indica que el docente ya es director de otra cátedra.

---

### Scenario 5: Rechazo por Administrador de Unidad intentando crear cátedra en materia de otra unidad (Forbidden)
- **Given** un usuario autenticado con rol `ADMIN_UNIDAD` asignado a la Unidad Académica con ID `3`
- **And** una materia con ID `4` perteneciente a la Unidad Académica con ID `9`
- **When** el usuario envía una petición `POST /api/catedras` con `materiaId: 4`
- **Then** el sistema responde con HTTP status `403 Forbidden`
- **And** el mensaje de error indica que no tiene permisos para crear cátedras en materias de otra unidad.

---

### Scenario 6: Rechazo por entidad no encontrada (Materia o Docente inexistente)
- **Given** una petición `POST /api/catedras` con un `materiaId` o `directorId` inexistente
- **When** el servicio intenta resolver las dependencias
- **Then** el sistema responde con HTTP status `404 Not Found`
- **And** el cuerpo de la respuesta indica el recurso que no fue encontrado.

---

### Scenario 7: Validación de campos obligatorios e inválidos
- **Given** una petición `POST /api/catedras` con nombre nulo/en blanco, tipología negativa o campos obligatorios faltantes
- **When** el controlador ejecuta la validación de bean validation (`@Valid`)
- **Then** el sistema responde con HTTP status `400 Bad Request`
- **And** se listan los errores específicos de validación de campos.

---

## Test Plan

### Context
Base de datos de pruebas configurada (H2 o PostgreSQL testcontainer) con roles cargados (`ADMIN_GLOBAL`, `ADMIN_UNIDAD`, `DOCENTE`), unidades y materias precargadas.

### Scenarios Table

| # | Tipo | Escenario | Resultado Esperado |
|---|---|---|---|
| 1 | Éxito | Admin Global crea cátedra válida con director disponible | 201 Created + CatedraResponseDTO |
| 2 | Éxito | Admin Unidad crea cátedra en materia de su propia facultad | 201 Created + CatedraResponseDTO |
| 3 | Error | Alta con nombre duplicado para la misma materia | 400 Bad Request ("Ya existe una cátedra con ese nombre...") |
| 4 | Error | Alta con director que ya tiene cátedra asignada | 400 Bad Request ("El docente ya es director de otra cátedra") |
| 5 | Seguridad | Admin Unidad intentando registrar cátedra en materia foránea | 403 Forbidden |
| 6 | Error | Materia o Docente no encontrados por ID | 404 Not Found |
| 7 | Validación | Body incompleto o campos vacíos/inválidos | 400 Bad Request |

---

## Post-implementation Validation

1. `.\mvnw test -Dtest=CatedraServiceTest`
2. `.\mvnw test -Dtest=CatedraControllerTest`
3. Verificación de compilación global: `.\mvnw test-compile`

---

## Non-functional notes

- **Seguridad**: Métodos y endpoints asegurados con `@PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_UNIDAD')")`.
- **Integridad Transaccional**: La operación debe ser `@Transactional` para garantizar la consistencia atómica de la persistencia.
- **Rendimiento**: Consultas de existencia indexadas (`existsByNombre...` y `existsByDirectorId...`).
