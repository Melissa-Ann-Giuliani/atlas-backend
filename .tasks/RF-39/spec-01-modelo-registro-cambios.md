# User Story Description

Evolve the existing `Cambio` domain model, database schema, and `CambioService` so that every audited operation in the system captures exact timestamp (`LocalDateTime`), operation type (`ALTA`, `MODIFICACION`, `BAJA`), human-readable category (derived from `tablaAfectada`), and optional references to the affected `Usuario` (covering `Docente`, `AdminUnidad`, `Auditor`, `AdminGlobal` via `JOINED` inheritance) and `Unidad`. This enables accurate chronological auditing, user-based filtering, and unit-scoped RBAC in `spec-02`.

---

# Story (INVEST)

**As** a System Auditor / Global Administrator  
**I want** every relevant creation, modification, and deletion in the system to be recorded with timestamp, operation type, category, responsible user, affected user, and academic unit  
**So that** the audit log is complete, traceable, and ready for unit-scoped consultation.

---

# Analysis

#### Approach
- **Vertical Slice 1 (Data & Recording):**
  1. Update `public.cambios` in `init-atlas-db.sql` (and provide an idempotent `ALTER TABLE` migration script):
     - Change `cambio_fecha_hora` from `date` to `timestamp NOT NULL`.
     - Add `cambio_tipo_operacion VARCHAR(20) NOT NULL DEFAULT 'MODIFICACION'`.
     - Add `usuario_afectado_id INTEGER NULL` (FK to `usuarios`).
     - Add `unidad_id INTEGER NULL` (FK to `unidades`).
     - Enlarge `cambio_valor_anterior` / `cambio_valor_nuevo` to `VARCHAR(255)` (or `TEXT`).
  2. Create `TipoOperacion` enum (`ALTA`, `MODIFICACION`, `BAJA` — per `open-questions.md` M1).
  3. Update `Cambio.java` entity with `LocalDateTime fechaHora`, `TipoOperacion tipoOperacion`, `@ManyToOne Usuario usuarioAfectado`, `@ManyToOne Unidad unidad`. Category is derived cleanly from `tablaAfectada` (per `open-questions.md` R1).
  4. Extend `CambioService` with overloaded methods supporting `TipoOperacion`, `Usuario usuarioAfectado`, and `Unidad`, while preserving backwards compatibility for existing callers during transition.
  5. Instrument `ALTA`, `MODIFICACION`, and `BAJA` flows across all main entities (`usuarios`, `docentes`, `designaciones`, `cargos`, `funciones`, `licencias`, `renuncias`, `subrogancias`, `unidades`, `materias`, `catedras` — per `open-questions.md` B1).
- **Out of Scope:** Querying/filtering REST endpoints (covered in `spec-02-consulta-historial-cambios.md`).

---

# Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` |
| **Classes** | `src/main/java/com/atlas/atlas_backend/cambio/Cambio.java`, `src/main/java/com/atlas/atlas_backend/cambio/CambioService.java`, `src/main/java/com/atlas/atlas_backend/cambio/CambioRepository.java`, `init-atlas-db.sql` |
| **Existing Callers** | `DocenteService.java`, `UsuarioService.java`, `AdminGlobalService.java`, `AdminUnidadService.java`, `AuditorService.java` |

---

# Context

- [raw-requirement.md](./raw-requirement.md)
- [open-questions.md](./open-questions.md) (B1, B2, B3, R1, M1)
- [spec-02-consulta-historial-cambios.md](./spec-02-consulta-historial-cambios.md)

---

# Acceptance Criteria

### Scenario 1: Record an UPDATE change on a Usuario (e.g. Docente) with full metadata (Happy Path)
- **Given** an authenticated `ADMIN_UNIDAD` updates a `Usuario` (e.g. `Docente`) belonging to `Unidad` 1 (changing `domicilio` from `"Calle A"` to `"Calle B"`)
- **When** the update transaction completes
- **Then** a new `Cambio` record is persisted with:
  - `tipoOperacion` = `ACTUALIZACION`
  - `tablaAfectada` = `"docentes"`
  - `campoAfectado` = `"domicilio"`
  - `valorAnterior` = `"Calle A"`
  - `valorNuevo` = `"Calle B"`
  - `fechaHora` = current date and time (`LocalDateTime`)
  - `usuario` = authenticated responsible user
  - `usuarioAfectado` = updated `Usuario` (`Docente`)
  - `unidad` = `Unidad` 1

### Scenario 2: Record a CREATE operation
- **Given** an authenticated administrator creates a new audited entity (e.g., a `Designacion` for a `Docente` in `Unidad` 2, or a new `AdminUnidad`)
- **When** the creation succeeds
- **Then** a `Cambio` record is persisted with `tipoOperacion = CREACION`, `valorAnterior = null` (or `"-"`), `valorNuevo` containing the summary/identifier, and the linked `usuarioAfectado` and `unidad`.

### Scenario 3: Record a DELETE / logical deactivation operation
- **Given** an authenticated administrator deletes or deactivates an audited entity
- **When** the operation succeeds
- **Then** a `Cambio` record is persisted with `tipoOperacion = ELIMINACION`, `valorAnterior` populated, `valorNuevo = null` (or `"Inactivo"`), and the linked `usuarioAfectado` and `unidad` (if applicable).

### Scenario 4: Ignore no-op updates (Unhappy / Edge Case)
- **Given** an update request sends the exact same value for a field as already stored in the database
- **When** `CambioService.registrarCambio(...)` is invoked
- **Then** no `Cambio` record is persisted for that unchanged field.

---

# Test Plan

#### Context
- H2 in-memory database / Spring Boot test context with `CambioService` and `CambioRepository`.

#### Scenarios

| # | Type | Scenario | Expected Result |
| - | ---- | -------- | --------------- |
| 1 | Success | `registrarCambio` with `ACTUALIZACION`, `Usuario` (affected), `Unidad` | Persists `Cambio` with `LocalDateTime`, `usuario_afectado_id`, `unidad_id`, and `tipoOperacion` |
| 2 | Success | `registrarCambio` with `CREACION` and `valorAnterior = null` | Persists `Cambio` without NPE |
| 3 | Success | `registrarCambio` with `ELIMINACION` and `valorNuevo = null` | Persists `Cambio` without NPE |
| 4 | Edge Case | `valorAnterior` equals `valorNuevo` (after trimming) | No record saved |
| 5 | Regression | Existing callers (`DocenteService`, `UsuarioService`, etc.) | Continue compiling and recording changes with `LocalDateTime` |

---

# Post-implementation Validation

1. `./mvnw test` passes all unit and integration tests.
2. Verify `init-atlas-db.sql` matches JPA entity mappings.

---

# Non-functional notes

- **Performance:** Add database indexes on `cambios(cambio_fecha_hora DESC)`, `cambios(unidad_id)`, and `cambios(usuario_afectado_id)` to support fast filtered queries in `spec-02`.
- **Integrity:** Audit recording must participate in the same database transaction (`@Transactional`) as the mutating business operation so failed mutations do not leave orphan audit logs.
