# User Story Description

As an administrative or audit user, I need to view a list of all active teacher licenses in the faculty so that I can monitor faculty leaves, maintain operational continuity, and review license data within my authorization scope.

---

# Story (INVEST)

**As** an Administrador Global, Administrador de Unidad, or Auditor  
**I want** to view a paginated list of active licenses  
**So that** I can track staff currently on leave within my administrative purview

---

# Analysis

#### Approach

- The endpoint will fetch `Licencia` records that represent currently active leaves, defined by:
  - `designacion.designacion_estado_actual = 'Licencia'` AND `CURRENT_DATE BETWEEN licencia_fecha_inicio AND COALESCE(licencia_fecha_fin, licencia_fecha_fin_estim)`
- Authorization must be enforced strictly:
  - **Administrador Global**: Can view active licenses across all units in the faculty.
  - **Administrador de Unidad**: Restricted to active licenses corresponding to their unit (department, institute, or center), joined via `Licencia -> Designacion -> Cargo -> Unidad`.
  - **Auditor**: Global read-only access (no actions or editing capabilities).
- Columns displayed in the list (`LicenciaListDTO`):
  - **N° Resolución** (`licencia_nro_resolucion`)
  - **Motivo** (`motivo.motivo_l_nombre`)
  - **Docente** (`docente.apellido + ", " + docente.nombre`)
  - **Fecha Inicio** (`licencia_fecha_inicio`)
  - **Fecha Fin** (`COALESCE(licencia_fecha_fin, licencia_fecha_fin_estim)`)
  - **Días Usados** (`licencia_cantidad_dias`)
  - **Estado** (computed or mapped as "Activa")
- Filtering support:
  - Search by teacher name / resolution number.
  - Motivo de Licencia (`motivoId`).
  - Date range filters (`fechaDesde`, `fechaHasta`).
  - Unit filter (available when logged in as Global Admin).
  - Extensible to accept future column-based filters.
- Pagination and sorting applied at the database level for performance.

---

# Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` / `com.atlas.atlas_backend.licencia` |
| **Classes** | `LicenciaController.java`, `LicenciaService.java`, `LicenciaRepository.java`, `LicenciaListDTO.java` |
| **Method / mapping** | `GET /api/licencias` with pagination and filter criteria |

---

# Acceptance Criteria

**Scenario 1: Global Admin views all active licenses (Happy path)**
* **Given** the user is authenticated as an Administrador Global
* **When** they request the active licenses list via `GET /api/licencias`
* **Then** they receive a paginated list of all active licenses across the faculty
* **And** each record contains: N° Resolución, Motivo, Docente, Fecha Inicio, Fecha Fin, Días Usados, and Estado.

**Scenario 2: Unit Admin views active licenses restricted to their unit**
* **Given** the user is authenticated as an Administrador de Unidad belonging to "Departamento de Computación"
* **When** they request the active licenses list
* **Then** the results contain only active licenses associated with designations/cargos within "Departamento de Computación"
* **And** no records from other departments or institutes are returned.

**Scenario 3: Auditor views active licenses in read-only mode**
* **Given** the user is authenticated as an Auditor
* **When** they access the licenses list
* **Then** they can view the active licenses
* **And** no modification, deletion, or creation endpoints or UI action buttons are accessible to them.

**Scenario 4: Filtering licenses by motive and search term**
* **Given** an authorized user is viewing the active licenses list
* **When** they apply a search term for a teacher's surname and select a specific "Motivo de Licencia"
* **Then** only active licenses matching both criteria are returned in the paginated response.

**Scenario 5: Unauthenticated or unauthorized role access (Unhappy flow)**
* **Given** a user is unauthenticated or has a role without viewing permissions (e.g. standard Docente)
* **When** they attempt to access `GET /api/licencias`
* **Then** the system returns HTTP 401 Unauthorized or HTTP 403 Forbidden.

---

# Test Plan

#### Context
Requires seed data with multiple `Licencia` records across different `Unidad` entities, some past, some active, and associated `Docente` and `Designacion` entries.

#### Scenarios

| # | Type | Description | Expected Result |
|---|------|-------------|-----------------|
| 1 | Success | Global Admin queries `/api/licencias` | Returns HTTP 200 with total active licenses across all units |
| 2 | Success | Unit Admin queries `/api/licencias` | Returns HTTP 200 with licenses filtered strictly to user's unit |
| 3 | Success | Query with `search="Perez"` and `motivoId=1` | Returns matching subset of records |
| 4 | Error | Unauthenticated request | HTTP 401 |
| 5 | Error | Request by unauthorized role | HTTP 403 |

---

# Post-implementation Validation

1. Run unit/integration tests: `./mvnw test`
2. Verify API response structure using local dev server (`GET /api/licencias`)
3. Verify queries executed contain unit restriction where applicable

---

# Non-functional notes

- **Security**: Authorization and unit-scoping must be verified on the backend service layer and in SQL/JPQL queries, never trusted solely to frontend filtering.
- **Performance**: Must support database-level pagination (`Pageable`).
