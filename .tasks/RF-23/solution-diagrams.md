# Solution Diagrams — RF-23: Baja de una cátedra

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram (Baja de Cátedra)

Shows the sequence flow of decommissioning an existing catedra (`PATCH /api/catedras/{id}/baja`), illustrating the interactions between Actor, Controller, Service, Repositories, and the Database.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin (Global / Unidad)
    participant Controller as CatedraController
    participant Service as CatedraService
    participant CatedraRepo as CatedraRepository
    participant UsuarioRepo as UsuarioRepository
    participant AdminUnidadRepo as AdminUnidadRepository
    participant DB as PostgreSQL Database

    Admin->>Controller: PATCH /api/catedras/{id}/baja
    activate Controller
    Controller->>Service: darDeBajaCatedra(id, username)
    activate Service

    Service->>CatedraRepo: findById(id)
    activate CatedraRepo
    CatedraRepo-->>Service: Optional<Catedra>
    deactivate CatedraRepo

    opt Cátedra no existe
        Service-->>Controller: 404 Not Found ("Cátedra no encontrada")
        Controller-->>Admin: 404 Not Found
    end

    alt Caller is ADMIN_UNIDAD
        Service->>UsuarioRepo: findByUsername(username)
        UsuarioRepo-->>Service: Usuario
        Service->>AdminUnidadRepo: findById(usuario.id)
        AdminUnidadRepo-->>Service: AdminUnidad
        Service->>Service: Validar catedra.materia.unidad.id == adminUnidad.unidad.id
        opt Unidad no coincide
            Service-->>Controller: 403 Forbidden ("No tiene permisos para dar de baja cátedras en materias de otra unidad")
            Controller-->>Admin: 403 Forbidden
        end
    end

    opt Cátedra ya está en estado "De baja"
        Service-->>Controller: 400 Bad Request ("La cátedra ya se encuentra dada de baja")
        Controller-->>Admin: 400 Bad Request
    end

    Service->>Service: catedra.setEstado("De baja")
    Note over Service: Preserva director histórico, materia, tipologia y nombre
    Service->>CatedraRepo: save(catedra)
    activate CatedraRepo
    CatedraRepo->>DB: UPDATE catedras SET estado = 'De baja' WHERE id = :id
    DB-->>CatedraRepo: OK
    CatedraRepo-->>Service: Catedra actualizada
    deactivate CatedraRepo

    Service->>Service: Mapear a CatedraResponseDTO
    Service-->>Controller: CatedraResponseDTO (estado: "De baja")
    deactivate Service
    Controller-->>Admin: 200 OK (CatedraResponseDTO)
    deactivate Controller
```

---

## 2. Activity / Validation Flow Diagram

Illustrates the decision tree during the decommission lifecycle operation.

```mermaid
flowchart TD
    Start(["Inicio: PATCH /api/catedras/{id}/baja"]) --> FindCatedra["Buscar Catedra por ID"]
    FindCatedra --> Exists{"¿Existe la Cátedra?"}
    Exists -- No --> Err404["404 Not Found<br>('Cátedra no encontrada')"]

    Exists -- Sí --> CheckRole{"¿Usuario es ADMIN_UNIDAD?"}
    CheckRole -- Sí --> CheckUnit{"¿materia.unidad.id == adminUnidad.unidad.id?"}
    CheckUnit -- No --> Err403["403 Forbidden<br>('No tiene permisos para dar de baja cátedras en materias de otra unidad')"]

    CheckRole -- No (ADMIN_GLOBAL) --> CheckState{"¿Estado actual es 'De baja'?"}
    CheckUnit -- Sí --> CheckState

    CheckState -- Sí --> Err400["400 Bad Request<br>('La cátedra ya se encuentra dada de baja')"]
    CheckState -- No --> ApplyBaja["Set estado = 'De baja'<br>(Mantener director y datos intactos)"]

    ApplyBaja --> SaveDB["Persistir en BD (save)"]
    SaveDB --> Response200(["200 OK: CatedraResponseDTO"])
```

---

## 3. Class Diagram & Architecture

Highlights the components participating in the decommission slice.

```mermaid
classDiagram
    class CatedraController {
        +darDeBajaCatedra(id: Integer, authentication: Authentication): ResponseEntity~CatedraResponseDTO~
    }

    class CatedraService {
        +darDeBajaCatedra(id: Integer, username: String): CatedraResponseDTO
    }

    class CatedraRepository {
        +findById(id: Integer): Optional~Catedra~
        +save(catedra: Catedra): Catedra
    }

    class Catedra {
        -id: Integer
        -nombre: String
        -tipologia: Integer
        -estado: String
        -director: Docente
        -materia: Materia
        +setEstado(estado: String): void
        +getEstado(): String
    }

    class CatedraResponseDTO {
        +id: Integer
        +nombre: String
        +tipologia: Integer
        +estado: String
        +directorId: Integer
        +directorNombre: String
        +materiaId: Integer
        +materiaNombre: String
    }

    CatedraController --> CatedraService
    CatedraService --> CatedraRepository
    CatedraService --> CatedraResponseDTO
    CatedraRepository --> Catedra
```

