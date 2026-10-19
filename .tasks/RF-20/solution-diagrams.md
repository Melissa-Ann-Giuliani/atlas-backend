# Solution Diagrams — RF-20: Baja de un cargo

This document details the diagrams of the proposed solution for decommissioning a position.

---

## 1. Sequence Diagram

Illustrates the end-to-end execution flow of the decommissioning endpoint, role validation, designation checks, and persistence.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin Global
    participant Controller as CargoController
    participant Service as CargoService
    participant UsuarioRepo as UsuarioRepository
    participant CargoRepo as CargoRepository
    participant DB as Database

    Admin->>Controller: PATCH /api/cargos/{id}/baja
    Controller->>Service: darDeBajaCargo(id, username)
    
    Service->>UsuarioRepo: findByUsername(username)
    UsuarioRepo-->>Service: Usuario (Rol)
    Service->>Service: Validar rol == "ADMIN_GLOBAL"
    alt Rol no autorizado
        Service-->>Controller: throw AccessDeniedException
        Controller-->>Admin: 403 Forbidden
    end

    Service->>CargoRepo: findById(id)
    alt Cargo no existe
        CargoRepo-->>Service: Optional.empty()
        Service-->>Controller: throw ResponseStatusException(404, "Cargo no encontrado")
        Controller-->>Admin: 404 Not Found
    end
    CargoRepo-->>Service: Cargo

    Service->>Service: Validar estado actual != "Dado de baja"
    alt Cargo ya dado de baja
        Service-->>Controller: throw ResponseStatusException(400, "El cargo ya se encuentra dado de baja")
        Controller-->>Admin: 400 Bad Request
    end

    Service->>Service: Validar designación vigente
    alt Tiene designación vigente activa
        Service-->>Controller: throw ResponseStatusException(400, "El cargo no puede darse de baja porque posee una designación vigente")
        Controller-->>Admin: 400 Bad Request
    end

    Service->>CargoRepo: findByCargoOrigenDivisionId(id)
    CargoRepo-->>Service: List<Cargo> subcargos
    Service->>Service: Validar que no existan subcargos activos (estado != 'Dado de baja')
    alt Posee subcargos derivados activos
        Service-->>Controller: throw ResponseStatusException(400, "No se puede dar de baja el cargo porque posee subcargos activos derivados de una división")
        Controller-->>Admin: 400 Bad Request
    end

    critical Actualización atómica de estado
        Service->>Service: cargo.setEstado("Dado de baja")
        Service->>Service: cargo.setHorasOcupadas(0)
        Service->>CargoRepo: save(cargo)
        CargoRepo->>DB: UPDATE cargos SET cargo_estado = 'Dado de baja', cargo_horas_ocupadas = 0 WHERE cargo_id = {id}
    end

    Service-->>Controller: CargoDetailDTO
    Controller-->>Admin: 200 OK (Detalle del cargo actualizado)
```

---

## 2. Class Diagram

Shows the components, services, repositories, and domain models involved in the decommissioning operation.

```mermaid
classDiagram
    class CargoController {
        -CargoService cargoService
        +darDeBajaCargo(Integer id, Authentication auth): ResponseEntity~CargoDetailDTO~
    }

    class CargoService {
        -CargoRepository cargoRepository
        -UsuarioRepository usuarioRepository
        +darDeBajaCargo(Integer cargoId, String username): CargoDetailDTO
        -esDesignacionVigente(Designacion designacion): boolean
    }

    class Cargo {
        -Integer id
        -Long codigo
        -Integer horasOcupadas
        -LocalDate fechaCreacion
        -String estado
        -Cargo cargoPredecesor
        -Cargo cargoOrigenDivision
        -Origen origen
        -Caracter caracter
        -Categoria categoria
        -Dedicacion dedicacion
        -Designacion designacion
        -Unidad unidad
        +setEstado(String estado): void
        +setHorasOcupadas(Integer horas): void
    }

    class Designacion {
        -Integer designacionId
        -Integer designacionNumeroResolucion
        -LocalDate designacionFechaInicio
        -LocalDate designacionFechaFin
        -String designacionEstadoActual
        -Integer usuarioId
    }

    class CargoDetailDTO {
        -Integer id
        -Long codigo
        -Integer horasOcupadas
        -LocalDate fechaCreacion
        -String estado
        -String unidad
        -String categoria
        -String dedicacion
    }

    CargoController --> CargoService
    CargoService --> Cargo
    Cargo --> Designacion
    CargoService --> CargoDetailDTO
```

---

## 3. State Lifecycle Diagram

Shows the state transitions of a `Cargo` entity highlighting the decommission transition.

```mermaid
stateDiagram-v2
    [*] --> Libre: Creación del cargo
    Libre --> Asignado: Designación formal de docente
    Asignado --> Libre: Finalización de designación
    Asignado --> Licencia: Solicitud de licencia docente
    Licencia --> Asignado: Reincorporación
    Licencia --> Libre: Finalización de licencia y designación
    Libre --> Dado_de_baja: Decommission (RF-20, sin designación vigente)
    Licencia --> Dado_de_baja: Decommission (RF-20, si designación finalizada/desvinculada)
    Dado_de_baja --> [*]: Estado terminal histórico
```
