# User Story Description

As an administrative or audit user, I need to view the detailed information of a specific license so that I can see the associated teacher, resolution details, designation information, linked position/cargo, and any supporting documentation.

---

# Story (INVEST)

**As** an Administrador Global, Administrador de Unidad, or Auditor  
**I want** to view the full details of a specific license  
**So that** I can review all administrative, designation, and cargo context of the leave

---

# Analysis

#### Approach

- The endpoint will fetch the complete `Licencia` entity along with its related objects:
  - Teacher (Docente/Usuario): Full Name ("Apellido, Nombre"), DNI, CUIL, institutional email.
  - Motivo: Reason name and total entitled days.
  - Designación: Resolution number, period (start/end date), status.
  - Cargo(s): Associated cargo code, category, dedication, character, unit name.
  - Attached documentation: Any uploaded resolutions or medical/academic certificates.
- Authorization:
  - Global Admin and Auditor can inspect any license.
  - Unit Admin can only view licenses belonging to their assigned unit; attempting to view another unit's license returns HTTP 403 or 404.

---

# Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` / `com.atlas.atlas_backend.licencia` |
| **Classes** | `LicenciaController.java`, `LicenciaService.java`, `LicenciaDetailDTO.java` |
| **Method / mapping** | `GET /api/licencias/{id}` |

---

# Acceptance Criteria

**Scenario 1: Viewing License Details (Happy path)**
* **Given** an authorized user is on the licenses list
* **When** they select a specific license row
* **Then** they are presented with the full license detail view
* **And** they see:
  - **Datos de la Licencia**: N° Resolución, Motivo, Fecha Inicio, Fecha Fin / Estimada, Cantidad de Días.
  - **Docente**: Apellido y Nombre, DNI, Email.
  - **Designación y Cargo**: N° Resolución de designación, Cargo(s) asociado(s), Unidad académica.

**Scenario 2: Unit Admin attempts to access a license from another unit (Unhappy path)**
* **Given** the user is logged in as an Administrador de Unidad for Unit A
* **When** they attempt to fetch `GET /api/licencias/{id}` where `{id}` belongs to Unit B
* **Then** the request is rejected with HTTP 404 Not Found or HTTP 403 Forbidden.

**Scenario 3: Auditor view constraints**
* **Given** the user is authenticated as an Auditor
* **When** they view the license detail
* **Then** all detail information is displayed in read-only mode
* **And** action buttons such as "Editar Licencia" or "Finalizar Licencia" are omitted.

---

# Test Plan

#### Scenarios

| # | Type | Description | Expected Result |
|---|------|-------------|-----------------|
| 1 | Success | Query valid license ID within user scope | HTTP 200 with complete `LicenciaDetailDTO` |
| 2 | Error | Query non-existent license ID | HTTP 404 |
| 3 | Error | Unit admin queries license ID from foreign unit | HTTP 404 / 403 |

---

# Post-implementation Validation

1. Run unit/integration tests: `./mvnw test`
2. Test `GET /api/licencias/{id}` with test user credentials
