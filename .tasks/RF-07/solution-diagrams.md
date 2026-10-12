# Solution Diagrams — RF-07

## 1. Sequence Diagram (Mandatory)

This diagram shows the flow of fetching the active licenses list and handling unit-scoped authorization.

```mermaid
sequenceDiagram
    actor Admin as Administrador / Auditor
    participant UI as Atlas Frontend
    participant API as LicenciaController
    participant Service as LicenciaService
    participant DB as PostgreSQL DB

    Admin->>UI: Navigate to "Licencias" list
    UI->>API: GET /api/licencias?search=&motivoId=&page=0&size=10
    Note over API: Extracts JWT to obtain User Role & Unit
    API->>Service: getActiveLicencias(username, search, motivoId, pageable)
    
    alt Role == ADMIN_GLOBAL OR AUDITOR
        Service->>DB: Query active licenses across all units
    else Role == ADMIN_UNIDAD
        Service->>DB: Query active licenses restricted to user's unit_id
    end
    
    DB-->>Service: Page<Licencia>
    Service-->>API: Page<LicenciaListDTO>
    API-->>UI: 200 OK (Page of LicenciaListDTO)
    UI-->>Admin: Render active licenses table
```

---

## 2. Class Diagram (Mandatory)

This diagram models the key backend entities, DTOs, and Spring components for RF-07.

```mermaid
classDiagram
    class LicenciaController {
        +getActiveLicencias(search, motivoId, page, size, authentication) ResponseEntity~Page~LicenciaListDTO~~
        +getLicenciaDetails(id, authentication) ResponseEntity~LicenciaDetailDTO~
    }

    class LicenciaService {
        +getActiveLicencias(username, search, motivoId, pageable) Page~LicenciaListDTO~
        +getLicenciaDetails(id, username) LicenciaDetailDTO
    }

    class LicenciaRepository {
        +findActiveLicencias(unitId, search, motivoId, pageable) Page~LicenciaListDTO~
    }

    class Licencia {
        +Integer id
        +String nroResolucion
        +LocalDate fechaInicio
        +LocalDate fechaFin
        +LocalDate fechaFinEstim
        +Integer cantidadDias
        +MotivoLicencia motivo
        +Designacion designacion
    }

    class MotivoLicencia {
        +Integer id
        +String nombre
        +Integer diasCorresp
    }

    class LicenciaListDTO {
        +Integer id
        +String nroResolucion
        +String motivoNombre
        +String docenteNombreCompleto /* Formato: "Apellido, Nombre" */
        +LocalDate fechaInicio
        +LocalDate fechaFin
        +Integer diasUsados
        +String estado
    }

    class LicenciaDetailDTO {
        +Integer id
        +String nroResolucion
        +LocalDate fechaInicio
        +LocalDate fechaFin
        +LocalDate fechaFinEstim
        +Integer cantidadDias
        +String motivoNombre
        +String docenteNombreCompleto /* Formato: "Apellido, Nombre" */
        +Long docenteDni
        +String docenteEmail
        +Integer designacionNroResolucion
        +List~String~ cargosVinculados
    }

    LicenciaController --> LicenciaService
    LicenciaService --> LicenciaRepository
    LicenciaRepository --> Licencia
    Licencia --> MotivoLicencia
    LicenciaService ..> LicenciaListDTO
    LicenciaService ..> LicenciaDetailDTO
```

---

## 3. State & Lifecycle Diagram (Supporting Diagram)

Illustrates the lifecycle conditions determining when a license is considered "Activa".

```mermaid
stateDiagram-v2
    [*] --> Registrada: Creada con Fecha Inicio y Estimada
    Registrada --> Activa: CURRENT_DATE >= fecha_inicio Y <= fecha_fin/fecha_fin_estim
    Activa --> Finalizada: CURRENT_DATE > fecha_fin OR Reincorporación efectiva
    Finalizada --> [*]
```
