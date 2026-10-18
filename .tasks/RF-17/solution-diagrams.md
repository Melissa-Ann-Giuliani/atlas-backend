# Solution Diagrams — RF-17

This document details the diagrams of the proposed solution for registering a new position (Alta de un cargo).

---

## 1. Sequence Diagram (Mandatory)

Shows the interaction flow between the actors, Spring Security, controller, service, and repositories when registering a cargo.

```mermaid
sequenceDiagram
    actor Admin as Admin (Global / Unidad)
    participant Auth as Spring Security
    participant Controller as CargoController
    participant Service as CargoService
    participant RepoCargo as CargoRepository
    participant RepoCatalog as Catalog Repositories (Origen, Caracter, etc.)
    participant RepoAdminUnidad as AdminUnidadRepository

    Admin->>Auth: POST /api/cargos (CargoCreateRequest, JWT)
    Auth-->>Controller: Forward Request (if authorized)

    Controller->>Controller: Validate request body (@Valid)
    alt Validation failed (missing fields)
        Controller-->>Admin: 400 Bad Request
    else Valid payload
        Controller->>Service: crearCargo(request, authentication.getName())

        alt User is ADMIN_UNIDAD
            Service->>RepoAdminUnidad: findById(usuarioId)
            RepoAdminUnidad-->>Service: AdminUnidad instance
            alt unidadId does not match user's unit
                Service-->>Controller: throw AccessDeniedException
                Controller-->>Admin: 403 Forbidden
            end
        end

        Service->>RepoCargo: existsByCodigo(codigo)
        RepoCargo-->>Service: exists (Boolean)
        alt Cargo code already exists
            Service-->>Controller: throw ResponseStatusException(409)
            Controller-->>Admin: 409 Conflict ("Cargo number already exists")
        else Unique cargo code
            Service->>RepoCatalog: Fetch Origen, Caracter, Categoria, Dedicacion, Unidad
            alt Any catalogue entity missing
                Service-->>Controller: throw EntityNotFoundException
                Controller-->>Admin: 404 Not Found
            else All catalogues found
                opt Predecessor provided
                    Service->>RepoCargo: findByCodigo(predecesorCodigo)
                    RepoCargo-->>Service: Optional<Cargo>
                    alt Predecessor not found
                        Service-->>Controller: throw EntityNotFoundException
                        Controller-->>Admin: 404 Not Found ("Predecessor not found")
                    end
                end

                Service->>Service: Build Cargo (estado="Libre", horasOcupadas=0, designacion=null)
                Service->>RepoCargo: save(cargo)
                RepoCargo-->>Service: savedCargo

                Service-->>Controller: saved CargoDetailDTO
                Controller-->>Admin: 201 Created (CargoDetailDTO)
            end
        end
    end
```

---

## 2. Class Diagram (Mandatory)

Outlines the static class structure, controller, service, DTOs, and relationships.

```mermaid
classDiagram
    class CargoController {
        - CargoService cargoService
        + getActiveCargos(...) ResponseEntity~Page~CargoListDTO~~
        + getCargoDetails(id, auth) ResponseEntity~CargoDetailDTO~
        + crearCargo(CargoCreateRequest, Authentication) ResponseEntity~CargoDetailDTO~
    }

    class CargoService {
        - CargoRepository cargoRepository
        - UsuarioRepository usuarioRepository
        - AdminUnidadRepository adminUnidadRepository
        - OrigenRepository origenRepository
        - CaracterRepository caracterRepository
        - CategoriaRepository categoriaRepository
        - DedicacionRepository dedicacionRepository
        - UnidadRepository unidadRepository
        + crearCargo(CargoCreateRequest, String username) CargoDetailDTO
    }

    class CargoCreateRequest {
        + Long codigo
        + LocalDate fechaCreacion
        + Integer origenId
        + Integer caracterId
        + Integer categoriaId
        + Integer dedicacionId
        + Integer unidadId
        + Long predecesorCodigo
    }

    class Cargo {
        + Integer id
        + Long codigo
        + Integer horasOcupadas
        + LocalDate fechaCreacion
        + String estado
        + Cargo cargoPredecesor
        + Origen origen
        + Caracter caracter
        + Categoria categoria
        + Dedicacion dedicacion
        + Unidad unidad
        + Designacion designacion
    }

    class Origen {
        + Integer origenId
        + String origenNombre
    }

    class Caracter {
        + Integer caracterId
        + String caracterNombre
    }

    class Categoria {
        + Integer categoriaId
        + String categoriaNombre
    }

    class Dedicacion {
        + Integer dedicacionId
        + String dedicacionNombre
        + Integer dedicacionHoras
    }

    class Unidad {
        + Integer unidadId
        + String nombre
    }

    CargoController ..> CargoCreateRequest : receives
    CargoController --> CargoService : delegates to
    CargoService --> Cargo : builds & persists
    Cargo --> Origen : references
    Cargo --> Caracter : references
    Cargo --> Categoria : references
    Cargo --> Dedicacion : references
    Cargo --> Unidad : references
    Cargo --> Cargo : optional predecessor
```

---

## 3. Additional Useful Diagram: State & Lifecycle Diagram

Illustrates the initial creation state of the Cargo and where it fits in the broader system lifecycle.

```mermaid
stateDiagram-v2
    [*] --> Libre: POST /api/cargos (RF-17 Alta de Cargo)<br/>horasOcupadas = 0, designacion = null

    Libre --> Asignado: Asignación en Designación docente
    Asignado --> Licencia: Docente toma licencia
    Licencia --> Asignado: Reincorporación de licencia
    Asignado --> Libre: Baja / Finalización de designación
    Libre --> DadoDeBaja: Baja definitiva de cargo
```
