# Solution Diagrams — RF-22: Modificar los datos de una cátedra

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram (Modificación de Cátedra)

Shows the sequence flow of modifying an existing catedra (`PUT /api/catedras/{id}`), illustrating the interactions between Actor, Controller, Service, Repositories, and the Database.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin (Global / Unidad)
    participant Controller as CatedraController
    participant Service as CatedraService
    participant CatedraRepo as CatedraRepository
    participant DocenteRepo as DocenteRepository
    participant UsuarioRepo as UsuarioRepository
    participant AdminUnidadRepo as AdminUnidadRepository
    participant DB as PostgreSQL Database

    Admin->>Controller: PUT /api/catedras/{id} (CatedraUpdateRequestDTO)
    activate Controller
    Controller->>Service: modificarCatedra(id, request, username)
    activate Service

    Service->>CatedraRepo: findById(id)
    activate CatedraRepo
    CatedraRepo-->>Service: Catedra (or 404 Not Found)
    deactivate CatedraRepo

    alt Caller is ADMIN_UNIDAD
        Service->>UsuarioRepo: findByUsername(username)
        UsuarioRepo-->>Service: Usuario
        Service->>AdminUnidadRepo: findById(usuario.id)
        AdminUnidadRepo-->>Service: AdminUnidad
        Service->>Service: Validar catedra.materia.unidad.id == adminUnidad.unidad.id
        opt Unidad no coincide
            Service-->>Controller: 403 Forbidden ("No tiene permisos para modificar cátedras en materias de otra unidad")
        end
    end

    opt Cátedra no está en estado "Activa"
        Service-->>Controller: 400 Bad Request ("Solo se pueden modificar cátedras en estado activa")
    end

    Service->>DocenteRepo: findById(request.directorId)
    activate DocenteRepo
    DocenteRepo-->>Service: Docente (or 404 Not Found)
    deactivate DocenteRepo

    opt Docente no está en estado "Activo"
        Service-->>Controller: 400 Bad Request ("El docente debe estar en estado activo")
    end

    Service->>CatedraRepo: existsByNombreIgnoreCaseAndMateria_IdAndIdNot(nombre, catedra.materia.id, id)
    activate CatedraRepo
    CatedraRepo-->>Service: boolean (true/false)
    deactivate CatedraRepo
    opt Otra cátedra ya tiene ese nombre en la materia
        Service-->>Controller: 400 Bad Request ("Ya existe una cátedra con ese nombre para la materia seleccionada")
    end

    Service->>Service: Actualizar atributos editables (nombre, tipologia, director)
    Service->>CatedraRepo: save(catedra)
    activate CatedraRepo
    CatedraRepo->>DB: UPDATE catedras SET ... WHERE catedra_id = ?
    DB-->>CatedraRepo: Catedra actualizada
    CatedraRepo-->>Service: Catedra entidad
    deactivate CatedraRepo

    Service-->>Controller: CatedraResponseDTO
    deactivate Service
    Controller-->>Admin: 200 OK (CatedraResponseDTO)
    deactivate Controller
```

---

## 2. Class Diagram

Shows the classes, interfaces, DTOs, and relationships involved in the modification of a `Catedra`.

```mermaid
classDiagram
    class CatedraController {
        -CatedraService catedraService
        +crearCatedra(CatedraRequestDTO request, Authentication auth) ResponseEntity~?~
        +modificarCatedra(Integer id, CatedraUpdateRequestDTO request, Authentication auth) ResponseEntity~?~
    }

    class CatedraService {
        -CatedraRepository catedraRepository
        -MateriaRepository materiaRepository
        -DocenteRepository docenteRepository
        -UsuarioRepository usuarioRepository
        -AdminUnidadRepository adminUnidadRepository
        +crearCatedra(CatedraRequestDTO request, String username) CatedraResponseDTO
        +modificarCatedra(Integer id, CatedraUpdateRequestDTO request, String username) CatedraResponseDTO
    }

    class CatedraRepository {
        <<interface>>
        +existsByMateria_IdAndEstadoIgnoreCase(Integer materiaId, String estado) boolean
        +existsByNombreIgnoreCaseAndMateria_Id(String nombre, Integer materiaId) boolean
        +existsByNombreIgnoreCaseAndMateria_IdAndIdNot(String nombre, Integer materiaId, Integer id) boolean
    }

    class Catedra {
        -Integer id
        -String nombre
        -Integer tipologia
        -String estado
        -Docente director
        -Materia materia
    }

    class CatedraUpdateRequestDTO {
        -String nombre
        -Integer tipologia
        -Integer directorId
    }

    class CatedraResponseDTO {
        -Integer id
        -String nombre
        -Integer tipologia
        -String estado
        -Integer directorId
        -String directorNombre
        -Integer materiaId
        -String materiaNombre
    }

    CatedraController --> CatedraService
    CatedraService --> CatedraRepository
    CatedraService ..> CatedraUpdateRequestDTO
    CatedraService ..> CatedraResponseDTO
    CatedraRepository ..> Catedra
```

---

## 3. Validation & Decision Flow Diagram

Shows the validation pipeline executed during `PUT /api/catedras/{id}`.

```mermaid
flowchart TD
    A["PUT /api/catedras/{id} (CatedraUpdateRequestDTO)"] --> B{"¿Existe Cátedra (id)?"}
    B -- No --> E404A["404 Not Found: Cátedra no encontrada"]
    B -- Sí --> C{"¿Usuario es ADMIN_UNIDAD?"}
    C -- Sí --> D{"¿Materia pertenece a su Unidad?"}
    D -- No --> E403["403 Forbidden: Sin permisos en otra unidad"]
    D -- Sí --> S{"¿Cátedra en estado 'Activa'?"}
    C -- No (ADMIN_GLOBAL) --> S
    S -- No --> E400S["400 Bad Request: Cátedra no está activa"]
    S -- Sí --> E{"¿Existe Docente (directorId)?"}
    E -- No --> E404B["404 Not Found: Docente no encontrado"]
    E -- Sí --> F{"¿Docente en estado 'Activo'?"}
    F -- No --> E400A["400 Bad Request: Docente inactivo"]
    F -- Sí --> G{"¿Existe otra cátedra (id != actual) con igual nombre en la materia?"}
    G -- Sí --> E400B["400 Bad Request: Nombre duplicado en la materia"]
    G -- No --> H["Actualizar nombre, tipología y director en Catedra y guardar en BD"]
    H --> I["200 OK: CatedraResponseDTO"]
```
