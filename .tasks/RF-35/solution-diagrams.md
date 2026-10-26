# Solution Diagrams — RF-35 (Baja de Usuario)

This document details the diagrams of the proposed solution for decommissioning any user in the system.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of decommissioning a user, illustrating the interaction between the Global Administrator, Controller, Service, Repositories, and Audit service.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador Global
    participant Controller as UsuarioController
    participant Service as UsuarioService
    participant UserRepo as UsuarioRepository
    participant DocenteRepo as DocenteRepository
    participant DesigRepo as DesignacionRepository
    participant CambioService as CambioService

    Admin->>Controller: PATCH /api/usuarios/{id}/baja
    activate Controller

    Controller->>Service: darDeBajaUsuario(id, authUsername)
    activate Service

    Service->>UserRepo: findById(id)
    alt User Not Found
        Service-->>Controller: 404 NOT_FOUND ("Usuario no encontrado")
        Controller-->>Admin: 404 Not Found
    end

    opt Target User == Authenticated User
        Service-->>Controller: 400 BAD_REQUEST ("No puede darse de baja a sí mismo")
        Controller-->>Admin: 400 Bad Request
    end

    opt User Already Inactive (activo == false)
        Service-->>Controller: 400 BAD_REQUEST ("El usuario ya se encuentra inactivo")
        Controller-->>Admin: 400 Bad Request
    end

    opt Target User is Docente
        Service->>DesigRepo: findActiveByUsuarioId(id)
        alt Has Active / Non-expired Designaciones
            Service-->>Controller: 409 CONFLICT ("No se puede dar de baja: posee cargos vigentes")
            Controller-->>Admin: 409 Conflict
        end
    end

    Note over Service,UserRepo: Proceed with deactivation
    Service->>Service: user.setActivo(false)
    Service->>UserRepo: save(user)

    opt User is Docente
        Service->>DocenteRepo: findById(id)
        Service->>Service: docente.setEstado("Inactivo")
        Service->>DocenteRepo: save(docente)
    end

    Service->>CambioService: registrarCambio("usuarios", "usuario_activo", "true", "false", authUsername)
    opt User is Docente
        Service->>CambioService: registrarCambio("docentes", "docente_estado", "Activo", "Inactivo", authUsername)
    end

    Service-->>Controller: UsuarioBajaResponseDTO
    deactivate Service

    Controller-->>Admin: 200 OK (Confirmation & User Details)
    deactivate Controller
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, interfaces, attributes, and methods involved in the solution and their relationships.

```mermaid
classDiagram
    class UsuarioController {
        -UsuarioService usuarioService
        +darDeBajaUsuario(Integer id, Authentication auth): ResponseEntity~?~
    }

    class UsuarioService {
        -UsuarioRepository usuarioRepository
        -DocenteRepository docenteRepository
        -DesignacionRepository designacionRepository
        -CambioService cambioService
        +darDeBajaUsuario(Integer id, String authUsername): UsuarioResponseDTO
    }

    class UsuarioRepository {
        +findById(Integer id): Optional~Usuario~
        +save(Usuario usuario): Usuario
    }

    class DocenteRepository {
        +findById(Integer id): Optional~Docente~
        +save(Docente docente): Docente
    }

    class DesignacionRepository {
        +findByUsuarioId(Integer usuarioId): List~Designacion~
    }

    class CambioService {
        +registrarCambio(String tabla, String campo, String valAnt, String valNuevo, String usuario): void
    }

    class Usuario {
        -Integer id
        -String username
        -String correo
        -String nombre
        -String apellido
        -Boolean activo
        -Rol rol
    }

    class Docente {
        -Long dni
        -Long cuil
        -String estado
    }

    Usuario <|-- Docente
    UsuarioController --> UsuarioService
    UsuarioService --> UsuarioRepository
    UsuarioService --> DocenteRepository
    UsuarioService --> DesignacionRepository
    UsuarioService --> CambioService
```

---

## 3. State & Validation Flow Diagram

Illustrates the decision logic, business rule validations, and resulting state transition during user decommissioning.

```mermaid
flowchart TD
    Start([Start: Decommission Request]) --> FindUser[Find Usuario by ID]
    FindUser --> Found{User exists?}
    Found -- No --> Err404[Return 404: Usuario no encontrado]
    Found -- Yes --> CheckSelf{Target User == Logged Admin?}
    CheckSelf -- Yes --> Err400Self[Return 400: No puede darse de baja a sí mismo]
    CheckSelf -- No --> CheckActivo{usuario_activo == true?}
    CheckActivo -- No --> Err400Inactive[Return 400: Usuario ya inactivo]
    CheckActivo -- Yes --> CheckRole{Is user a Docente?}
    CheckRole -- No --> DeactivateUser[Set usuario.activo = false]
    CheckRole -- Yes --> CheckCargos{Has active Designaciones / Cargos?}
    CheckCargos -- Yes --> Err409Cargos[Return 409: Posee cargos vigentes]
    CheckCargos -- No --> DeactivateDocente[Set usuario.activo = false<br/>Set docente.estado = 'Inactivo']
    DeactivateUser --> Audit[Register audit in cambios table]
    DeactivateDocente --> Audit
    Audit --> Save[Persist changes in DB]
    Save --> Return200([Return 200 OK: Usuario dado de baja])
```
