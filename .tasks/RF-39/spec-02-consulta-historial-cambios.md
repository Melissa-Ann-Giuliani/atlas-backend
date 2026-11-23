# User Story Description

Expose a REST API endpoint (`GET /api/cambios`) allowing `Administrador Global`, `Administrador de Unidad`, and `Auditor` users to consult the chronological change history of the system, applying dynamic filters (affected user name/surname, date range, operation type, responsible user) and enforcing strict academic unit isolation for unit-scoped roles.

---

# Story (INVEST)

**As** a Global Administrator, Unit Administrator, or Auditor  
**I want** to view and filter a chronological list of system changes (most recent first)  
**So that** I can audit who modified what information, when, and with what previous and new values within my authorized scope.

---

# Analysis

#### Approach
- **Vertical Slice 2 (Query & RBAC):**
  1. Create `CambioResponseDTO` (and nested `UsuarioResumenDTO`) containing: `id`, `fechaHora`, `usuarioResponsable`, `categoria`, `campoAfectado`, `valorAnterior`, `valorNuevo`, `tipoOperacion`, `usuarioAfectadoId`, `usuarioAfectadoNombreCompleto`, `unidadId`.
  2. Extend `CambioRepository` with `JpaSpecificationExecutor<Cambio>` (or a custom JPQL `@Query`) to support dynamic optional filters:
     - `usuarioAfectado` (LIKE case-insensitive on `usuarioAfectado.nombre` or `usuarioAfectado.apellido`)
     - `fechaDesde` / `fechaHasta` (inclusive `LocalDateTime` bounds)
     - `tipoOperacion` (`ALTA`, `MODIFICACION`, `BAJA`)
     - `usuarioId` (responsible administrator)
     - `unidadId` (enforced automatically for `ADMIN_UNIDAD`; optional filter for `ADMIN_GLOBAL` and `AUDITOR`)
  3. Default sorting: `fechaHora DESC, id DESC`.
  4. Create `CambioController` exposing `GET /api/cambios` secured with `@PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_UNIDAD', 'AUDITOR')")`.
  5. Enforce RBAC in `CambioService.consultarHistorial(...)` (per `open-questions.md` B2):
     - Extract authenticated user and role from `SecurityContextHolder`.
     - If `ADMIN_UNIDAD`, force `unidadId = usuario.unidadId` and `usuarioAfectado IS NOT NULL`.
     - If `ADMIN_GLOBAL` or `AUDITOR`, allow viewing all changes across all units (with optional `unidadId` filter).

---

# Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` |
| **Classes** | `src/main/java/com/atlas/atlas_backend/cambio/CambioController.java` (new), `src/main/java/com/atlas/atlas_backend/cambio/CambioResponseDTO.java` (new), `src/main/java/com/atlas/atlas_backend/cambio/CambioService.java`, `src/main/java/com/atlas/atlas_backend/cambio/CambioRepository.java` |
| **Security** | `src/main/java/com/atlas/atlas_backend/security/` |

---

# Context

- [raw-requirement.md](./raw-requirement.md)
- [open-questions.md](./open-questions.md) (B2, R1, R2, M1, M2)
- [spec-01-modelo-registro-cambios.md](./spec-01-modelo-registro-cambios.md)

---

# Acceptance Criteria

### Scenario 1: Global Administrator or Auditor lists all changes chronologically (Happy Path)
- **Given** an authenticated `ADMIN_GLOBAL` or `AUDITOR` user
- **And** multiple `Cambio` records exist across different academic units and dates
- **When** the user sends `GET /api/cambios` without filters
- **Then** the response status is `200 OK`
- **And** the returned list is ordered from most recent to oldest (`fechaHora DESC`)
- **And** each item includes `fechaHora`, `usuarioResponsable`, `categoria`, `campoAfectado`, `valorAnterior`, `valorNuevo`, and `tipoOperacion`.

### Scenario 2: Unit Administrator sees only their own unit's users
- **Given** an authenticated `ADMIN_UNIDAD` assigned to `Unidad` 1
- **And** changes exist for users in `Unidad` 1, users in `Unidad` 2, and global entities (`unidadId = null`)
- **When** the user sends `GET /api/cambios`
- **Then** the response status is `200 OK`
- **And** only changes with `usuarioAfectado != null` and `unidadId = 1` are returned.

### Scenario 3: Combined filtering by affected user name, date range, operation type, and responsible user
- **Given** an authenticated authorized user
- **When** the user sends `GET /api/cambios?usuarioAfectado=Perez&fechaDesde=2026-01-01&fechaHasta=2026-01-31&tipoOperacion=MODIFICACION&usuarioId=5`
- **Then** the response status is `200 OK`
- **And** only records matching all provided criteria within the user's authorized scope are returned.

### Scenario 4: Unauthorized role attempts to access change history (Unhappy Path)
- **Given** an authenticated user with role `DOCENTE` (or an unauthenticated request)
- **When** the user sends `GET /api/cambios`
- **Then** the system rejects the request with `403 Forbidden` (or `401 Unauthorized` if unauthenticated).

### Scenario 5: Invalid date range (Unhappy Path)
- **Given** an authenticated authorized user
- **When** the user sends `GET /api/cambios?fechaDesde=2026-05-10&fechaHasta=2026-05-01`
- **Then** the system responds with `400 Bad Request` indicating `fechaDesde` cannot be after `fechaHasta`.

### Scenario 6: No matching records
- **Given** no changes match the applied filters
- **When** the user sends `GET /api/cambios`
- **Then** the system responds according to `open-questions.md` (R2 — default `200 OK` with empty page).

---

# Test Plan

#### Context
- MockMvc / `@SpringBootTest` with `@WithMockUser` for `ADMIN_GLOBAL`, `ADMIN_UNIDAD`, `AUDITOR`, and `DOCENTE`.

#### Scenarios

| # | Type | Scenario | Expected Result |
| - | ---- | -------- | --------------- |
| 1 | Success | `ADMIN_GLOBAL` queries `GET /api/cambios` | `200 OK`, all units, ordered `fechaHora DESC` |
| 2 | Success | `ADMIN_UNIDAD` queries `GET /api/cambios` | `200 OK`, strictly filtered to own `unidadId` |
| 3 | Success | `AUDITOR` queries `GET /api/cambios` | `200 OK`, strictly filtered to own `unidadId` |
| 4 | Alternative | Filter by `usuarioAfectado`, `fechaDesde`, `fechaHasta`, `tipoOperacion`, `usuarioId` | `200 OK`, only matching records |
| 5 | Error | `DOCENTE` role calls `GET /api/cambios` | `403 Forbidden` |
| 6 | Error | `fechaDesde > fechaHasta` | `400 Bad Request` |

---

# Post-implementation Validation

1. `./mvnw test` passes all controller, service, and repository tests.
2. Manual smoke test with JWT tokens for `ADMIN_GLOBAL`, `ADMIN_UNIDAD`, and `AUDITOR`.

---

# Non-functional notes

- **Security:** Unit scoping for `ADMIN_UNIDAD` and `AUDITOR` must be enforced server-side from the authenticated user's profile/claims, never trusting a client-supplied `unidadId` query parameter.
- **Performance:** Use `Pageable` (default `size = 20`) and fetch-join `usuario` / `usuarioAfectado` (or `@EntityGraph`) to prevent N+1 queries when mapping to `CambioResponseDTO`.
