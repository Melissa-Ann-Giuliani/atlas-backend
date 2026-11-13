# Diagramas de Solución — RF-11: Asignar funciones a un cargo de una designación

## 1. Diagrama de Secuencia: Asignación de Funciones y Cierre de Designación

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin (Global / Unidad)
    participant Ctrl as CargoController
    participant Svc as FuncionService
    participant CargoRepo as CargoRepository
    participant MateriaRepo as MateriaRepository
    participant DocenteRepo as DocenteRepository
    participant ArchivoSvc as ArchivoService
    participant EmailSvc as EmailService

    Admin->>Ctrl: POST /api/cargos/{cargoId}/funciones (lista de funciones con tipo)
    Ctrl->>Svc: asignarFuncionesACargo(cargoId, request, userPrincipal)

    activate Svc
    Svc->>CargoRepo: findById(cargoId)
    CargoRepo-->>Svc: Cargo (con Dedicación, Unidad, Designación)

    Note over Svc: Valida: existencia del cargo,<br/>estado "Asignado", designación vinculada,<br/>permisos de unidad para ADMIN_UNIDAD

    Note over Svc: Valida tope de horas (Decision B2):<br/>sum(funciones.horas) menor o igual a dedicacionHoras

    loop Para cada función
        Svc->>MateriaRepo: findById(materiaId)
        MateriaRepo-->>Svc: Materia
        Note over Svc: Valida existencia y misma unidad académica (Decisión R3)
    end

    Svc->>Svc: Persistir funciones con funcion_tipo (Decisión B1)
    Svc->>CargoRepo: Actualizar cargo_horas_ocupadas = sum(horas)

    Svc->>ArchivoSvc: generarYRegistrarPdfDesignacion(cargo, designacion)
    ArchivoSvc-->>Svc: Archivo (id, nombre, url/path)

    Svc->>DocenteRepo: Activar docente (usuario.activo = true)

    Svc->>EmailSvc: sendDesignacionCompletaNotification(docente, designacion, documento)

    Svc-->>Ctrl: DesignacionCompletaResponseDTO
    deactivate Svc

    Ctrl-->>Admin: 200 OK (Funciones, Documento PDF ID, Docente Activo)
```

## 2. Diagrama de Estados del Trámite de Designación (RF-09 → RF-10 → RF-11)

```mermaid
stateDiagram-v2
    [*] --> DesignacionCreada: RF-09 (POST /api/designaciones)
    note right of DesignacionCreada: Designación persistida\nDocente permanece Inactivo\nCargo aún no vinculado

    DesignacionCreada --> CargoVinculado: RF-10 (PATCH /api/designaciones/{id}/cargo)
    note right of CargoVinculado: Cargo pasa a estado Asignado\nDocente permanece Inactivo\nSin funciones asignadas

    CargoVinculado --> DesignacionCompletada: RF-11 (POST /api/cargos/{id}/funciones)
    note right of DesignacionCompletada: Funciones asignadas (sum horas <= dedicacion)\nRegistro de tipo de funcion\nCargo.horasOcupadas actualizado\nDocente pasa a estado Activo\nDocumento PDF generado y archivado\nNotificacion por correo despachada

    DesignacionCompletada --> [*]
```
