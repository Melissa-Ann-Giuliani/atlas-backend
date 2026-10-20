# Solution Diagrams — RF-21: Alta de una cátedra

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram (Alta de Cátedra)

Shows the sequence flow of creating a new catedra, illustrating the interactions between Actor, Controller, Service, Repositories, and the Database.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin (Global / Unidad)
    participant Controller as CatedraController
    participant Service as CatedraService
    participant CatedraRepo as CatedraRepository
    participant DocenteRepo as DocenteRepository
    participant MateriaRepo as MateriaRepository
    participant DB as PostgreSQL Database

    Admin->>Controller: POST /api/catedras (CatedraRequestDTO)
    activate Controller
    Controller->>Service: crearCatedra(request, authenticatedUser)
    activate Service

    Service->>MateriaRepo: findById(request.materiaId)
    activate MateriaRepo
    MateriaRepo-->>Service: Materia (or 404 Not Found)
    deactivate MateriaRepo

    alt Caller is ADMIN_UNIDAD
        Service->>Service: Validar materia.unidad.id == admin.unidad.id
        opt Unidad no coincide
            Service-->>Controller: 403 Forbidden ("No tiene permisos sobre esta unidad")
        end
    end

    Service->>DocenteRepo: findById(request.directorId)
    activate DocenteRepo
    DocenteRepo-->>Service: Docente (or 404 Not Found)
    deactivate DocenteRepo

    Service->>CatedraRepo: existsByNombreAndMateriaId(nombre, materiaId)
    activate CatedraRepo
    CatedraRepo-->>Service: boolean (true/false)
    deactivate CatedraRepo
    opt Cátedra ya existe
        Service-->>Controller: 400 Bad Request ("Ya existe una cátedra con ese nombre para la materia")
    end

    Service->>CatedraRepo: existsByDirectorId(directorId)
    activate CatedraRepo
    CatedraRepo-->>Service: boolean (true/false)
    deactivate CatedraRepo
    opt Docente ya es director de otra cátedra
        Service-->>Controller: 400 Bad Request ("El docente ya es director de otra cátedra")
    end

    Service->>Service: Construir entidad Catedra (estado="Activo"/"Activa")
    Service->>CatedraRepo: save(catedra)
    activate CatedraRepo
    CatedraRepo->>DB: INSERT INTO catedras (...)
    DB-->>CatedraRepo: Catedra guardada (id autogenerado)
    CatedraRepo-->>Service: Catedra entidad
    deactivate CatedraRepo

    Service-->>Controller: CatedraResponseDTO
    deactivate Service
    Controller-->>Admin: 201 Created (CatedraResponseDTO)
    deactivate Controller
```

---

## 2. Class Diagram

Shows the classes, interfaces, DTOs, and relationships involved in the solution.

```mermaid
classDiagram
    class CatedraController {
        -CatedraService catedraService
        +crearCatedra(CatedraRequestDTO request, Authentication auth) ResponseEntity~CatedraResponseDTO~
    }

    class CatedraService {
        -CatedraRepository catedraRepository
        -DocenteRepository docenteRepository
        -MateriaRepository materiaRepository
        -UsuarioRepository usuarioRepository
        +crearCatedra(CatedraRequestDTO request, String username) CatedraResponseDTO
    }

    class CatedraRepository {
        <<interface>>
        +existsByNombreIgnoreCaseAndMateriaId(String nombre, Integer materiaId) boolean
        +existsByDirectorId(Integer directorId) boolean
    }

    class Catedra {
        -Integer id
        -String nombre
        -Integer tipologia
        -String estado
        -Docente director
        -Materia materia
    }

    class CatedraRequestDTO {
        -String nombre
        -Integer tipologia
        -Integer directorId
        -Integer materiaId
    }

    class CatedraResponseDTO {
        -Integer id
        -String nombre
        -Integer tipologia
        -String estado
        -Integer directorId
        -String directorNombreCompleto
        -Integer materiaId
        -String materiaNombre
    }

    CatedraController --> CatedraService
    CatedraService --> CatedraRepository
    CatedraService ..> CatedraRequestDTO
    CatedraService ..> CatedraResponseDTO
    CatedraRepository ..> Catedra
```

---

## 3. State & Lifecycle Diagram

Shows the lifecycle states of a `Catedra`.

```mermaid
stateDiagram-v2
    [*] --> Activa: Alta de Cátedra (RF-21, estado por defecto)
    Activa --> DeBaja: Baja de Cátedra (RF posterior)
    DeBaja --> [*]
```
