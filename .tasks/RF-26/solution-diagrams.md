# Solution Diagrams — RF-26: Baja de una materia

This document details the diagrams of the proposed solution for decommissioning a subject via lifecycle state transition (`PATCH /api/materias/{id}/baja`).

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of the decommission operation, illustrating actor interaction, security scoping, active cátedra checks, and state transition.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin (Global / Unidad)
    participant Ctrl as MateriaController
    participant Svc as MateriaService
    participant CatRepo as CatedraRepository
    participant MatRepo as MateriaRepository

    Admin->>Ctrl: PATCH /api/materias/{id}/baja
    activate Ctrl

    Ctrl->>Svc: darDeBajaMateria(id, username)
    activate Svc

    Svc->>MatRepo: findById(id)
    MatRepo-->>Svc: Optional<Materia>

    alt Subject not found
        Svc-->>Ctrl: throw ResponseStatusException(NOT_FOUND, "Materia no encontrada")
        Ctrl-->>Admin: 404 Not Found
    end

    alt Subject already decommissioned
        Svc-->>Ctrl: throw ResponseStatusException(BAD_REQUEST, "La materia ya se encuentra dada de baja")
        Ctrl-->>Admin: 400 Bad Request
    end

    alt Role is ADMIN_UNIDAD and subject.unidad != user.unidad
        Svc-->>Ctrl: throw AccessDeniedException("No tiene permisos para dar de baja materias en esta unidad académica")
        Ctrl-->>Admin: 403 Forbidden
    end

    Svc->>CatRepo: existsByMateria_IdAndEstadoIgnoreCase(id, "ACTIVA")
    CatRepo-->>Svc: boolean hasActiveCatedras

    alt hasActiveCatedras is true
        Svc-->>Ctrl: throw ResponseStatusException(CONFLICT, "No se puede dar de baja la materia porque tiene cátedras activas asociadas")
        Ctrl-->>Admin: 409 Conflict
    end

    Note over Svc,MatRepo: Transición de ciclo de vida: materia.setEstado("Dada de baja")
    Svc->>MatRepo: save(materia)
    MatRepo-->>Svc: Materia (saved)

    Svc-->>Ctrl: MateriaDetailDTO (con estado = "Dada de baja")
    deactivate Svc

    Ctrl-->>Admin: 200 OK (MateriaDetailDTO)
    deactivate Ctrl
```

---

## 2. Class Diagram (Mandatory)

Outlines the participating domain entities, controllers, services, and repositories, including the `estado` lifecycle attribute.

```mermaid
classDiagram
    class MateriaController {
        -MateriaService materiaService
        +darDeBajaMateria(id: Integer, auth: Authentication): ResponseEntity~MateriaDetailDTO~
    }

    class MateriaService {
        -MateriaRepository materiaRepository
        -CatedraRepository catedraRepository
        -UsuarioRepository usuarioRepository
        +darDeBajaMateria(id: Integer, username: String): MateriaDetailDTO
    }

    class MateriaRepository {
        +findById(id: Integer): Optional~Materia~
        +save(materia: Materia): Materia
        +existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudio(nombre: String, unidadId: Integer, plan: Integer): boolean
    }

    class CatedraRepository {
        +existsByMateria_IdAndEstadoIgnoreCase(materiaId: Integer, estado: String): boolean
    }

    class Materia {
        -Integer id
        -String nombre
        -Integer horas
        -String anio
        -String despliegue
        -Integer planEstudio
        -String estado
        -Unidad unidad
        +getEstado(): String
        +setEstado(estado: String): void
    }

    class MateriaDetailDTO {
        +Integer id
        +String nombre
        +Integer horas
        +String anio
        +String despliegue
        +Integer planEstudio
        +String estado
        +Integer unidadId
        +String unidadNombre
    }

    class Catedra {
        -Integer id
        -String estado
        -Materia materia
    }

    class Unidad {
        -Integer unidadId
        -String unidadNombre
    }

    MateriaController --> MateriaService
    MateriaService --> MateriaRepository
    MateriaService --> CatedraRepository
    Materia --> Unidad : belongs to
    Catedra --> Materia : references
    MateriaService ..> MateriaDetailDTO : maps to
```

---

## 3. State Transition Diagram

Illustrates the lifecycle states and transitions for the `Materia` entity.

```mermaid
stateDiagram-v2
    [*] --> Activa : Alta de Materia (RF-24)
    Activa --> Activa : Modificación (RF-25)
    Activa --> Dada_de_baja : PATCH /api/materias/{id}/baja (RF-26) [sin cátedras activas]
    Dada_de_baja --> [*] : Registro histórico preservado
```
