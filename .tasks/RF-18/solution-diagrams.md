# Solution Diagrams — RF-18: Modificar los datos de un cargo

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of the cargo update process, illustrating interaction between the client, security layer, controller, service, and database repositories.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador (Global / Unidad)
    participant Auth as Spring Security
    participant Ctrl as CargoController
    participant Svc as CargoService
    participant Repo as CargoRepository
    participant CatRepo as Catalog Repositories (Origen, Caracter, Categoria, Dedicacion)
    participant DB as Database

    Admin->>Auth: PUT /api/cargos/{id} (CargoUpdateRequest, JWT)
    Auth-->>Ctrl: Authorized (ADMIN_GLOBAL / ADMIN_UNIDAD)
    Ctrl->>Svc: modificarCargo(id, request, username)
    
    Svc->>Repo: findById(id)
    alt Cargo not found
        Repo-->>Svc: Optional.empty()
        Svc-->>Ctrl: throw ResponseStatusException(NOT_FOUND)
        Ctrl-->>Admin: HTTP 404 Not Found
    end
    Repo-->>Svc: Cargo

    opt Role is ADMIN_UNIDAD
        Svc->>Svc: Validate Cargo.unidad matches AdminUnidad.unidad
        alt Unit mismatch
            Svc-->>Ctrl: throw AccessDeniedException
            Ctrl-->>Admin: HTTP 403 Forbidden
        end
    end

    Svc->>CatRepo: Validate & fetch Origen, Caracter, Categoria, Dedicacion
    alt Any catalog reference not found
        CatRepo-->>Svc: Optional.empty()
        Svc-->>Ctrl: throw ResponseStatusException(NOT_FOUND)
        Ctrl-->>Admin: HTTP 404 Not Found
    end
    CatRepo-->>Svc: Entities

    opt Dedicacion validation (Risk R1)
        Svc->>Svc: Check if horasOcupadas > 0 and dedicacionHoras < horasOcupadas
        alt New hours lower than occupied hours
            Svc-->>Ctrl: throw ResponseStatusException(BAD_REQUEST)
            Ctrl-->>Admin: HTTP 400 Bad Request
        end
    end

    opt Has predecesorCodigo (Minor M1)
        Svc->>Repo: findByCodigo(predecesorCodigo)
        alt Predecessor not found OR self-reference
            Repo-->>Svc: Optional.empty() or Same Cargo
            Svc-->>Ctrl: throw ResponseStatusException
            Ctrl-->>Admin: HTTP 404 / 400
        end
        Repo-->>Svc: Predecessor Cargo
    end
    Note over Svc: If predecesorCodigo is omitted or null, existing predecessor is preserved

    Svc->>Svc: Update Cargo fields (fechaCreacion, origen, caracter, categoria, dedicacion, cargoPredecesor if provided)
    Svc->>Repo: save(cargo)
    Repo->>DB: UPDATE cargos SET ... WHERE cargo_id = ?
    DB-->>Repo: Updated row
    Repo-->>Svc: Cargo
    Svc-->>Ctrl: CargoDetailDTO
    Ctrl-->>Admin: HTTP 200 OK (CargoDetailDTO)
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, DTOs, controllers, services, repositories, and relationships involved in the update solution.

```mermaid
classDiagram
    class CargoController {
        -CargoService cargoService
        +modificarCargo(Integer id, CargoUpdateRequest request, Authentication auth): ResponseEntity~CargoDetailDTO~
    }

    class CargoService {
        -CargoRepository cargoRepository
        -UsuarioRepository usuarioRepository
        -AdminUnidadRepository adminUnidadRepository
        -OrigenRepository origenRepository
        -CaracterRepository caracterRepository
        -CategoriaRepository categoriaRepository
        -DedicacionRepository dedicacionRepository
        +modificarCargo(Integer id, CargoUpdateRequest request, String username): CargoDetailDTO
    }

    class CargoUpdateRequest {
        +LocalDate fechaCreacion
        +Integer origenId
        +Integer caracterId
        +Integer categoriaId
        +Integer dedicacionId
        +Long predecesorCodigo
    }

    class CargoDetailDTO {
        +Long numero
        +String categoria
        +String estado
        +LocalDate fechaCreacion
        +String dedicacion
        +String caracter
        +CargoPredecesorData cargoPredecesor
    }

    class Cargo {
        +Integer id
        +Long codigo
        +LocalDate fechaCreacion
        +Integer horasOcupadas
        +String estado
        +Origen origen
        +Caracter caracter
        +Categoria categoria
        +Dedicacion dedicacion
        +Unidad unidad
        +Cargo cargoPredecesor
    }

    CargoController --> CargoService : delegates to
    CargoController ..> CargoUpdateRequest : consumes
    CargoController ..> CargoDetailDTO : produces
    CargoService --> Cargo : modifies & persists
    CargoService ..> CargoUpdateRequest : reads
    CargoService ..> CargoDetailDTO : maps to
```

---

## 3. Additional Useful Diagram: State & Invariants Transition Flow

Illustrates which fields are mutable via `PUT /api/cargos/{id}` and which are preserved as invariants.

```mermaid
flowchart TD
    Req[Incoming PUT /api/cargos/id] --> VCheck{Exists & Authorized?}
    VCheck -- No --> Err[HTTP 403 Forbidden or 404 Not Found]
    VCheck -- Yes --> ApplyChanges[Apply Editable Changes]

    subgraph EditableFields [Mutable Attributes]
        F1[fechaCreacion]
        F2[origen_id]
        F3[caracter_id]
        F4[categoria_id]
        F5[dedicacion_id]
        F6[cargo_predecesor_id]
    end

    subgraph InvariantFields [Preserved / Immutable Attributes]
        I1[cargo_id: PK]
        I2[cargo_codigo: Unique Public Code]
        I3[unidad_id: Organizational Boundary]
        I4[cargo_estado: Managed via workflow]
        I5[cargo_horas_ocupadas: Managed via functions]
        I6[designacion_id: Managed via designation flow]
        I7[cargo_origen_division: Preserved lineage]
    end

    ApplyChanges --> EditableFields
    ApplyChanges -. Keeps intact .-> InvariantFields
    EditableFields --> SaveDB[(Save to Database)]
    SaveDB --> Ret[Return HTTP 200 OK with CargoDetailDTO]
```
