# RF-34 — Solution Diagrams

## Sequence Diagram — Modify Auditor Flow

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador Global
    participant Client as Frontend / Client
    participant Controller as UsuarioController
    participant Service as AuditorService
    participant Repo as AuditorRepository
    participant UserRepo as UsuarioRepository
    participant CambioSvc as CambioService
    participant DB as Database

    Admin->>Client: Edit Auditor Details & Click Save
    Client->>Controller: PUT /api/usuarios/auditor/{id} (AuditorUpdateRequest)
    activate Controller
    Note over Controller: Verify Global Admin Role
    Controller->>Service: modificarAuditor(id, request, authUsername)
    activate Service

    Service->>UserRepo: findByUsername(authUsername)
    UserRepo-->>Service: Optional<Usuario> (usuarioResponsable)

    Service->>Repo: findById(id)
    Repo-->>Service: Optional<Auditor>

    alt Auditor Not Found
        Service-->>Controller: throw ResponseStatusException(NOT_FOUND)
        Controller-->>Client: 404 Not Found ("Auditor no encontrado")
    end

    alt Username collision
        Service->>UserRepo: findByUsername(nuevoUsername)
        UserRepo-->>Service: existingUser (id != targetId)
        Service-->>Controller: throw ResponseStatusException(CONFLICT)
        Controller-->>Client: 409 Conflict ("El nombre de usuario ya se encuentra en uso")
    end

    alt Correo collision
        Service->>UserRepo: existsByCorreoAndIdNot(nuevoCorreo, id)
        UserRepo-->>Service: true
        Service-->>Controller: throw ResponseStatusException(CONFLICT)
        Controller-->>Client: 409 Conflict ("El correo electrónico ya se encuentra en uso")
    end

    Note over Service,CambioSvc: Register individual deltas for modified fields
    Service->>CambioSvc: registrarCambio("auditores", "username", old, new, resp)
    Service->>CambioSvc: registrarCambio("auditores", "nombre", old, new, resp)
    Service->>CambioSvc: registrarCambio("auditores", "apellido", old, new, resp)
    Service->>CambioSvc: registrarCambio("auditores", "correo", old, new, resp)
    Service->>CambioSvc: registrarCambio("auditores", "cargo_au_nombre", oldCargo, newCargo, resp)

    Service->>Repo: save(auditor)
    Repo->>DB: UPDATE auditores & usuarios
    DB-->>Repo: Auditor updated
    Repo-->>Service: savedAuditor

    Service-->>Controller: savedAuditor
    deactivate Service

    Controller-->>Client: 200 OK (AuditorResponse / Auditor object)
    deactivate Controller
    Client-->>Admin: Show Success Notification
```

## Architectural Component Diagram

```mermaid
graph TD
    subgraph REST API Layer
        UC[UsuarioController<br>PUT /api/usuarios/auditor/{id}]
        AUR[AuditorUpdateRequest]
    end

    subgraph Service Layer
        AS[AuditorService<br>modificarAuditor]
        CS[CambioService<br>registrarCambio]
    end

    subgraph Data Access Layer
        AR[AuditorRepository]
        UR[UsuarioRepository]
        CR[CambioRepository]
    end

    subgraph Database Tables
        T_USERS[(usuarios)]
        T_AUD[(auditores)]
        T_CAMBIOS[(cambios)]
    end

    UC -->|Validates DTO & Passes Auth| AS
    AUR -.-> UC
    AS -->|Find & Save| AR
    AS -->|Check Uniqueness & Auth User| UR
    AS -->|Delta Logging| CS
    CS -->|Persist Deltas| CR
    AR --> T_USERS
    AR --> T_AUD
    UR --> T_USERS
    CR --> T_CAMBIOS
```
