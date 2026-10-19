# Solution Diagrams — RF-19: Subdividir un cargo en 2 o más cargos

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of the solution being specified, illustrating the interaction between actors, controllers, services, repositories, and the database.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin (Global / Unidad)
    participant Controller as CargoController
    participant Service as CargoService
    participant CargoRepo as CargoRepository
    participant DedicacionRepo as DedicacionRepository
    participant DB as Database

    Admin->>Controller: POST /api/cargos/subdividir (SubdividirCargoRequest)
    Controller->>Service: subdividirCargo(request, username)
    Service->>CargoRepo: findByCodigo(request.cargoOrigenCodigo)
    alt Cargo padre no existe
        CargoRepo-->>Service: Optional.empty()
        Service-->>Controller: throw ResponseStatusException(404, "Cargo padre no encontrado")
        Controller-->>Admin: 404 Not Found
    end
    CargoRepo-->>Service: Cargo padre
    Service->>Service: Validar estado del padre ("Libre" o "Licencia")
    alt Estado inválido (ej. "Asignado" sin licencia)
        Service-->>Controller: throw ResponseStatusException(400, "Solo cargos en estado Libre o Licencia pueden subdividirse")
        Controller-->>Admin: 400 Bad Request
    end
    Service->>Service: Validar permisos del usuario (AdminUnidad pertenezca a la misma unidad y subcargos en su unidad)
    alt Permisos insuficientes
        Service-->>Controller: throw AccessDeniedException
        Controller-->>Admin: 403 Forbidden
    end
    Service->>Service: Validar subcargos (2 <= N <= 10, códigos únicos en payload y BD)
    Service->>DedicacionRepo: Obtener horas de dedicaciones
    DedicacionRepo-->>Service: Lista de dedicaciones
    Service->>Service: Validar suma(horas subcargos) <= horas(cargo padre)
    alt Suma de horas excede cargo original
        Service-->>Controller: throw ResponseStatusException(400, "La suma de horas de los subcargos supera al cargo original")
        Controller-->>Admin: 400 Bad Request
    end

    critical Transacción atómica de subdivisión
        Service->>Service: padre.setHorasOcupadas(padre.dedicacion.horas)
        Service->>CargoRepo: save(padre)
        loop Por cada subcargo
            Service->>CargoRepo: save(nuevoSubcargo con cargoOrigenDivision = padre, horasOcupadas = 0, estado = 'Libre')
            CargoRepo->>DB: INSERT into cargos
        end
    end

    Service-->>Controller: List<CargoDetailDTO>
    Controller-->>Admin: 201 Created (Detalles de los cargos generados)
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, interfaces, attributes, and methods involved in the solution and their relationships.

```mermaid
classDiagram
    class CargoController {
        -CargoService cargoService
        +subdividirCargo(SubdividirCargoRequest request, Authentication auth): ResponseEntity
    }

    class CargoService {
        -CargoRepository cargoRepository
        -DedicacionRepository dedicacionRepository
        -UnidadRepository unidadRepository
        -CategoriaRepository categoriaRepository
        -CaracterRepository caracterRepository
        -OrigenRepository origenRepository
        -UsuarioRepository usuarioRepository
        -AdminUnidadRepository adminUnidadRepository
        +subdividirCargo(SubdividirCargoRequest request, String username): List~CargoDetailDTO~
    }

    class SubdividirCargoRequest {
        -Long cargoOrigenCodigo
        -List~SubcargoItemRequest~ subcargos
        +getCargoOrigenCodigo(): Long
        +getSubcargos(): List~SubcargoItemRequest~
    }

    class SubcargoItemRequest {
        -Long codigo
        -LocalDate fechaCreacion
        -Integer origenId
        -Integer caracterId
        -Integer categoriaId
        -Integer dedicacionId
        -Integer unidadId
        -Long predecesorCodigo
    }

    class Cargo {
        -Integer id
        -Long codigo
        -Integer horasOcupadas
        -LocalDate fechaCreacion
        -Cargo cargoPredecesor
        -Cargo cargoOrigenDivision
        -Origen origen
        -Caracter caracter
        -Categoria categoria
        -Dedicacion dedicacion
        -Designacion designacion
        -Unidad unidad
        -String estado
    }

    class Dedicacion {
        -Integer dedicacionId
        -String dedicacionNombre
        -Integer dedicacionHoras
    }

    CargoController --> CargoService
    CargoService --> SubdividirCargoRequest
    SubdividirCargoRequest *-- SubcargoItemRequest
    CargoService --> Cargo
    Cargo --> Dedicacion
    Cargo --> Cargo : cargoOrigenDivision
    Cargo --> Cargo : cargoPredecesor
```

---

## 3. Additional Useful Diagram: State and Relationship Lifecycle

Diagram illustrating the structural hierarchy and status lifecycle when a position is divided into sub-positions according to the resolved decisions.

```mermaid
stateDiagram-v2
    direction TB

    state "Cargo Padre (Original)" as Padre {
        [*] --> Libre
        Libre --> Licencia : Docente solicita licencia
        Licencia --> Licencia_100Horas : RF-19 Subdivisión (Mantiene estado 'Licencia', horasOcupadas = 100%)
        Libre --> Libre_100Horas : RF-19 Subdivisión (Mantiene estado 'Libre', horasOcupadas = 100%)
        Licencia_100Horas --> [*] : Bloqueado para asignación docente directa
        Libre_100Horas --> [*] : Bloqueado para asignación docente directa
    }

    state "Subcargos Generados (2 <= N <= 4)" as Hijos {
        [*] --> Subcargo_Libre : Creado con cargoOrigenDivision = Padre, horasOcupadas = 0, estado = 'Libre'
        Subcargo_Libre --> Asignado : Designación docente
    }

    Padre --> Hijos : Subdivisión atómica (Suma Horas <= Padre)
```
