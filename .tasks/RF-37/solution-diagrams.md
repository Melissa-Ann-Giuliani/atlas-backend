# Solution Diagrams — RF-37

Documentación técnica y diagramas de interacción para la re-designación simple de docentes con designación próxima a vencer.

---

## 1. Diagrama de Secuencia: Flujo de Re-designación Simple

Muestra el flujo completo desde la vista de Vencimientos, pasando por la precarga de datos, edición y persistencia de la nueva designación con baja/renovación de la anterior.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador (Global o Unidad)
    participant UI as Frontend (Vencimientos / Trámites)
    participant Controller as DesignacionController
    participant Service as DesignacionService
    participant RepoDesig as DesignacionRepository
    participant RepoCargo as CargoRepository

    Admin->>UI: Clic en "Renovar" sobre docente por vencer
    Note over UI: Pre-carga los datos de la fila (docente, cargo, unidad) y abre formulario de trámite
    Admin->>UI: Ajusta fechas (inicio, fin), ingresa nueva resolución y confirma
    UI->>Controller: POST /api/designaciones/{id}/redesignar (payload con fechas, res, cargoId, funciones)
    
    Controller->>Service: redesignarDocente(id, request, username)
    Service->>RepoDesig: findById(id)
    Service->>Service: Validar que designación esté "Activa"
    Service->>Service: Validar permisos multi-tenant (si es ADMIN_UNIDAD, cargo pertenece a su unidad)
    Service->>RepoDesig: existsByUsuarioIdAndDesignacionNumeroResolucion(...)
    
    alt Número de resolución ya existe o fecha fin inválida
        Service-->>Controller: 400 Bad Request
        Controller-->>UI: Error de validación
    else Validación exitosa
        Service->>RepoDesig: save(nuevaDesignacion con estado "Activa")
        Service->>RepoCargo: Vincular cargo a la nueva designación
        Service->>RepoDesig: Marcar designación anterior con estado = "Renovada"
        Service->>RepoDesig: save(designacionAnterior)
        Service-->>Controller: Retorna DesignacionResponseDTO
        Controller-->>UI: 201 Created
        UI-->>Admin: Notificación de éxito y redirección / refresco de lista (alerta desactivada)
    end
```

---

## 2. Diagrama de Estado: Ciclo de Vida de la Designación

Ilustra la transición de estados de la entidad `Designacion` al ser renovada.

```mermaid
stateDiagram-v2
    [*] --> Activa: Alta de Designación
    Activa --> PorVencer: fechaFin <= hoy + 30 días (Alerta RF-36)
    PorVencer --> Renovada: Acción Re-designar (RF-37)
    Activa --> Renovada: Acción Re-designar anticipada (RF-37)
    PorVencer --> Vencida: Transcurre fechaFin sin renovación
    Renovada --> [*]: Histórico (Sin alerta)
    Vencida --> [*]
```

---

## 3. Diagrama de Arquitectura de Clases y DTOs

```mermaid
classDiagram
    class DesignacionController {
        +redesignar(id: Integer, request: DesignacionRedesignarRequest): ResponseEntity
        +obtenerVencimientos(incluirVencidas: Boolean, unidadId: Integer, dias: Integer, sinLimite: Boolean): ResponseEntity
    }

    class DesignacionService {
        +redesignarDocente(id: Integer, request: DesignacionRedesignarRequest, username: String): DesignacionResponseDTO
        +obtenerVencimientos(unidadId: Integer, incluirVencidas: Boolean, dias: Integer, sinLimite: Boolean, username: String): List~DesignacionVencimientoDTO~
    }

    class DesignacionRedesignarRequest {
        +nroResolucion: String
        +fechaInicio: LocalDate
        +fechaFin: LocalDate
        +cargoId: Integer
        +funciones: List~FuncionItemRequest~
    }

    class Designacion {
        +designacionId: Integer
        +designacionNumeroResolucion: String
        +designacionFechaInicio: LocalDate
        +designacionFechaFin: LocalDate
        +designacionEstadoActual: String
        +usuarioId: Integer
    }

    DesignacionController --> DesignacionService
    DesignacionService --> DesignacionRedesignarRequest
    DesignacionService --> Designacion
```
