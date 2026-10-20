# Solution Diagrams — RF-24

This document details the visual diagrams of the proposed solution for registering a new subject (Alta de una materia).

---

## 1. Sequence Diagram (Mandatory)

Shows the interaction flow between the actors, Spring Security, controller, service, and repositories when registering a subject.

```mermaid
sequenceDiagram
    actor Admin as Admin (Global / Unidad)
    participant Auth as Spring Security
    participant Controller as MateriaController
    participant Service as MateriaService
    participant RepoAdminUnidad as AdminUnidadRepository
    participant RepoUnidad as UnidadRepository
    participant RepoMateria as MateriaRepository

    Admin->>Auth: POST /api/materias (MateriaCreateRequest, JWT)
    Auth-->>Controller: Forward Request (if authenticated & authorized)

    Controller->>Controller: Validate request body (@Valid: horas 1..40, valid anio, valid despliegue)
    alt Validation failed (missing/invalid fields)
        Controller-->>Admin: 400 Bad Request
    else Valid payload
        Controller->>Service: crearMateria(request, authentication.getName())

        alt User has role ADMIN_UNIDAD
            Service->>RepoAdminUnidad: findByEmail(username)
            RepoAdminUnidad-->>Service: AdminUnidad instance
            alt request.unidadId != adminUnidad.unidad.id
                Service-->>Controller: throw AccessDeniedException
                Controller-->>Admin: 403 Forbidden ("Not authorized for this academic unit")
            end
        end

        Service->>RepoUnidad: findById(request.unidadId)
        alt Academic unit does not exist
            RepoUnidad-->>Service: Optional.empty()
            Service-->>Controller: throw EntityNotFoundException
            Controller-->>Admin: 404 Not Found ("Unidad not found")
        else Academic unit exists
            RepoUnidad-->>Service: Unidad entity
            Service->>RepoMateria: existsByNombreIgnoreCaseAndUnidadUnidadIdAndPlanEstudio(request.nombre, request.unidadId, request.planEstudio)
            alt Subject already exists in the same unit and study plan
                RepoMateria-->>Service: true
                Service-->>Controller: throw ResponseStatusException(409, "Subject already exists in unit and study plan")
                Controller-->>Admin: 409 Conflict ("Materia already exists for this unit and plan")
            else Subject does not exist
                RepoMateria-->>Service: false
                Service->>Service: Build new Materia(nombre, horas, anio, despliegue, planEstudio, unidad)
                Service->>RepoMateria: save(materia)
                RepoMateria-->>Service: savedMateria
                Service-->>Controller: MateriaDetailDTO
                Controller-->>Admin: 201 Created (MateriaDetailDTO)
            end
        end
    end
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, interfaces, attributes, and methods involved in the solution and their relationships.

```mermaid
classDiagram
    class MateriaController {
        -MateriaService materiaService
        +crearMateria(MateriaCreateRequest request, Authentication authentication) ResponseEntity~MateriaDetailDTO~
    }

    class MateriaService {
        -MateriaRepository materiaRepository
        -UnidadRepository unidadRepository
        -AdminUnidadRepository adminUnidadRepository
        +crearMateria(MateriaCreateRequest request, String username) MateriaDetailDTO
        -validarUnidadAutorizada(Integer unidadId, String username) void
    }

    class MateriaRepository {
        <<interface>>
        +existsByNombreIgnoreCaseAndUnidadUnidadIdAndPlanEstudio(String nombre, Integer unidadId, Integer planEstudio) boolean
    }

    class MateriaCreateRequest {
        +String nombre
        +Integer horas
        +String anio
        +String despliegue
        +Integer unidadId
        +Integer dependenciaId
        +Integer planEstudio
    }

    class MateriaDetailDTO {
        +Integer id
        +String nombre
        +Integer horas
        +String anio
        +String despliegue
        +Integer unidadId
        +String unidadNombre
        +Integer planEstudio
    }

    class Materia {
        -Integer id
        -String nombre
        -Integer horas
        -String anio
        -String despliegue
        -Integer planEstudio
        -Unidad unidad
    }

    class Unidad {
        -Integer unidadId
        -String unidadNombre
        -TipoUnidad tipoUnidad
    }

    class TipoUnidad {
        -Integer id
        -String nombre
    }

    MateriaController --> MateriaService
    MateriaController ..> MateriaCreateRequest
    MateriaController ..> MateriaDetailDTO
    MateriaService --> MateriaRepository
    MateriaService ..> Materia
    Materia --> Unidad
    Unidad --> TipoUnidad
```

---

## 3. Entity-Relationship & UI Selector Overview

Shows the relationship between Dependencia (`TipoUnidad`), `Unidad`, `Materia`, and `Catedra`.

```mermaid
erDiagram
    TIPO_UNIDAD ||--o{ UNIDAD : "categorizes (Dependencia)"
    UNIDAD ||--o{ MATERIA : "offers"
    MATERIA ||--o{ CATEDRA : "has"
    DOCENTE ||--o| CATEDRA : "directs"

    TIPO_UNIDAD {
        int tipo_unidad_id PK
        string tipo_unidad_nombre "ej. Facultad, Departamento, Escuela"
    }

    UNIDAD {
        int unidad_id PK
        string unidad_nombre "ej. Matemática, Computación"
        int tipo_unidad_id FK
    }

    MATERIA {
        int materia_id PK
        string materia_nombre
        int materia_horas "1 a 40"
        string materia_anio "1°, 2°, 3°, 4°, 5°"
        string materia_despliegue "Anual, Primer Semestre, Segundo Semestre"
        int materia_plan_estudio "ej. 2023"
        int unidad_id FK
    }

    CATEDRA {
        int catedra_id PK
        string catedra_nombre
        int materia_id FK
        int catedra_director_id FK
    }
```
