# Solution Diagrams - RF-06

## Sequence Diagram

This diagram shows the flow of fetching the active cargos list, highlighting how unit authorization is enforced.

```mermaid
sequenceDiagram
    actor Admin as Administrador
    participant UI as Atlas Frontend (UI)
    participant API as CargoController
    participant Service as CargoService
    participant DB as Database

    Admin->>UI: Navigate to "Cargos" list
    UI->>API: GET /api/cargos?status=ASIGNADO&page=0&size=20
    Note over API: Extracts JWT token to identify<br/>User Role and Unit ID
    API->>Service: getActiveCargos(role, unitId, filters, pageable)
    
    alt Role == Administrador Global OR Auditor (Global)
        Service->>DB: SELECT * FROM cargos WHERE status='ASIGNADO'
    else Role == Administrador de Unidad
        Service->>DB: SELECT * FROM cargos WHERE status='ASIGNADO' AND unit_id = :unitId
    end
    
    DB-->>Service: Page<Cargo>
    Service-->>API: Page<CargoDTO>
    API-->>UI: 200 OK (JSON)
    UI-->>Admin: Display paginated table of cargos
```

## Class Diagram

This diagram models the key backend entities and DTOs involved in serving this list.

```mermaid
classDiagram
    class CargoController {
        +getActiveCargos(Pageable, CargoFilterDTO) Page~CargoListDTO~
    }
    
    class CargoService {
        +getActiveCargos(UserContext, CargoFilterDTO, Pageable) Page~CargoListDTO~
    }

    class Cargo {
        +Long id
        +String status
        +Date startDate
        +Date endDate
        +Long teacherId
        +Long unitId
    }

    class CargoListDTO {
        +Long id
        +String cargoName
        +String teacherName
        +String unitName
        +String dedication
    }

    class UserContext {
        +String role
        +Long unitId
    }

    CargoController --> CargoService
    CargoService --> Cargo
    CargoService --> CargoListDTO
    CargoService --> UserContext
```
