# User Story – Decommission Any System User (RF-35)

## User Story Description

As a Global Administrator, I need the ability to decommission any registered system user (setting their state to inactive) so that access is immediately revoked while preserving historical audit and relational records, provided the user holds no active appointments/cargos.

---

## Story (INVEST)

```
**As a** Global Administrator (Administrador Global),
**I want to** decommission any user in the system,
**So that** their access to the system is revoked while preserving historical integrity and preventing security or operational risks.
```

---

## Analysis

### Approach

1. **Role and Permissions:**
   - Restricted exclusively to Global Administrators (`ADMIN_GLOBAL` / `ADMINISTRADOR_GLOBAL`).
2. **Pre-conditions & Validations:**
   - Target user must exist (`404 Not Found`).
   - Logged-in administrator cannot decommission themselves (`400 Bad Request`, `targetUser.getUsername().equalsIgnoreCase(authenticatedUsername)`).
   - Target user must be currently active (`usuario.getActivo() == true`); if already inactive, reject with `400 Bad Request` (`"El usuario ya se encuentra dado de baja"`).
   - The user cannot have active appointments/cargos (*"El usuario a dar de baja no puede tener cargos vigentes"*):
     - For `Docente`, verify using `DesignacionRepository.findByUsuarioId(userId)` and `Cargo` that no designation is active (`designacionEstadoActual` not in `'Finalizada'`, `'Cancelada'`, `'Baja'`) with a non-expired finish date (`designacionFechaFin == null` or `>= today`).
     - If active/vigente cargos exist, reject with `409 Conflict` (`"No se puede dar de baja el usuario porque posee cargos vigentes"`).
     - Non-docente users (`ADMIN_GLOBAL`, `ADMIN_UNIDAD`, `AUDITOR`) do not hold teaching `Designacion` records, satisfying this condition automatically.
3. **Action & Persistence:**
   - Update `usuario.setActivo(false)`.
   - If target user is a `Docente`, synchronize the sub-entity state: set `docente.setEstado("Dado de baja")` (or `"Inactivo"` if aligning with existing enum values, marking audit as `"Dado de baja"`).
   - Retain all historical records, credentials, associations, and assignments intact (soft-delete).
   - Log the state transition in the `cambios` audit table:
     - Register delta in table `"usuarios"`, field `"usuario_activo"`, previous value `"true"`, new value `"false"`.
     - If `Docente`, also register in table `"docentes"`, field `"docente_estado"`, previous value `docente.getEstado()`, new value `"Dado de baja"` (or `"Inactivo"`).
4. **Output:**
   - Return `200 OK` with confirmation message and updated user summary payload.

---

## Starting Point

| Element | Location |
|---|---|
| **Controller** | [UsuarioController.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/usuario/UsuarioController.java) |
| **Service** | [UsuarioService.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/usuario/UsuarioService.java) (to create) |
| **Repositories** | [UsuarioRepository.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/usuario/UsuarioRepository.java), [DocenteRepository.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/usuario/DocenteRepository.java), [DesignacionRepository.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/designacion/DesignacionRepository.java) |
| **Audit Service** | [CambioService.java](file:///c:/_dev/atlas/atlas-backend/src/main/java/com/atlas/atlas_backend/cambio/CambioService.java) |

---

## Acceptance Criteria

### Scenario 1: Successful decommissioning of an active non-docente user (Happy Path)
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **And** an existing user with ID `10` whose status is active (`activo = true`) and role is `ADMIN_UNIDAD` or `AUDITOR`
* **When** a request `PATCH /api/usuarios/10/baja` is sent
* **Then** the response status code is `200 OK`
* **And** the user's `activo` property is updated to `false`
* **And** an audit record is saved in `cambios` registering the transition of `usuario_activo` from `"true"` to `"false"`
* **And** the user can no longer log in.

---

### Scenario 2: Successful decommissioning of a docente without active cargos (Happy Path)
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **And** an existing `Docente` with ID `20` whose status is `activo = true`, `docente_estado = "Activo"`
* **And** the docente has no active designations (all previous designations are finalized or canceled)
* **When** a request `PATCH /api/usuarios/20/baja` is sent
* **Then** the response status code is `200 OK`
* **And** `usuario.activo` becomes `false` and `docente.estado` becomes `"Inactivo"`
* **And** audit records are registered in `cambios` for both tables.

---

### Scenario 3: Attempting to decommission a docente with active cargos (Unhappy Path - Conflict)
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **And** an existing `Docente` with ID `25` who holds an active designation linked to a cargo
* **When** a request `PATCH /api/usuarios/25/baja` is sent
* **Then** the response status code is `409 Conflict`
* **And** an error message is returned: `"No se puede dar de baja el usuario porque posee cargos vigentes"`
* **And** the user and docente states remain unchanged.

---

### Scenario 4: Attempting to decommission an already inactive user (Unhappy Path - Bad Request)
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **And** an existing user with ID `30` whose status is already inactive (`activo = false`)
* **When** a request `PATCH /api/usuarios/30/baja` is sent
* **Then** the response status code is `400 Bad Request`
* **And** an error message is returned: `"El usuario ya se encuentra inactivo"`.

---

### Scenario 5: Global Administrator attempting to decommission their own account (Unhappy Path - Bad Request)
* **Given** an authenticated user with username `"superadmin"` and role `ADMIN_GLOBAL`
* **When** a request `PATCH /api/usuarios/{superadminId}/baja` is sent
* **Then** the response status code is `400 Bad Request`
* **And** an error message is returned: `"No puede darse de baja a sí mismo"`.

---

### Scenario 6: User not found (Unhappy Path - Not Found)
* **Given** an authenticated user with role `ADMIN_GLOBAL`
* **When** a request `PATCH /api/usuarios/99999/baja` is sent
* **Then** the response status code is `404 Not Found`
* **And** an error message indicates user was not found.

---

### Scenario 7: Unauthorized role attempting decommission (Unhappy Path - Forbidden)
* **Given** an authenticated user with role `ADMIN_UNIDAD` or `DOCENTE`
* **When** a request `PATCH /api/usuarios/10/baja` is sent
* **Then** the response status code is `403 Forbidden`.

---

## Test Plan

| ID | Test Scenario | Expected Result |
|---|---|---|
| TC-01 | Decommission active AdminUnidad / Auditor | 200 OK, `usuario.activo = false`, audit logged |
| TC-02 | Decommission active Docente with no active cargos | 200 OK, `activo = false`, `docente.estado = "Inactivo"`, audit logged |
| TC-03 | Decommission Docente with active cargo | 409 Conflict with message "posee cargos vigentes" |
| TC-04 | Decommission user with non-existent ID | 404 Not Found |
| TC-05 | Decommission already inactive user | 400 Bad Request |
| TC-06 | Admin decommission self | 400 Bad Request |
| TC-07 | Non-admin global caller | 403 Forbidden |

---

## Post-implementation Validation

1. `mvn test -Dtest=UsuarioServiceTest`
2. `mvn test -Dtest=UsuarioControllerTest`
3. `./mvnw clean test-compile`
