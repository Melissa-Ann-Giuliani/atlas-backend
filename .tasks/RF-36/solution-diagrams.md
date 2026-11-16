# Solution Diagrams — RF-36: Control de Próximos Vencimientos de Designaciones

## 1. Arquitectura y Flujo de Control de Acceso (RBAC y Multi-tenant)

```mermaid
flowchart TD
    subgraph Cliente["Frontend (Web UI)"]
        UI_Login["Ingreso al Sistema (Dashboard / Inicio)"]
        UI_Vencimientos["Módulo Gestión > Vencimientos"]
    end

    subgraph API["Atlas Backend API"]
        C_Resumen["GET /api/designaciones/vencimientos/resumen"]
        C_List["GET /api/designaciones/vencimientos?incluirVencidas=false"]
        SecFilter["Spring Security Context (Roles & Claims)"]
        Service["DesignacionService"]
    end

    subgraph Persistencia["Base de Datos PostgreSQL"]
        DB[(designaciones, cargos, docentes, unidades)]
    end

    UI_Login -->|1. Consulta resumen para tarjeta/alerta en Dashboard (30 días)| C_Resumen
    UI_Vencimientos -->|2. Consulta grilla detallada con opción de incluir vencidas| C_List

    C_Resumen --> SecFilter
    C_List --> SecFilter

    SecFilter -->|Validar Rol e Inyectar scope de Unidad| Service
    Service -->|Query filtrando estado='Activa' y rango de fechaFin| DB
    DB -->|Retorna entidades| Service
    Service -->|DTO enriquecido con días restantes| UI_Vencimientos
```

---

## 2. Diagrama de Secuencia — Consulta de Alertas al Ingresar al Sistema (Dashboard Card)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador (Global o Unidad)
    participant UI as Frontend (Dashboard / Home)
    participant Auth as Security Context
    participant Controller as DesignacionController
    participant Service as DesignacionService
    participant Repo as DesignacionRepository

    Admin->>UI: Ingresa al sistema (Login exitoso)
    UI->>Controller: GET /api/designaciones/vencimientos/resumen
    Controller->>Auth: Extraer usuario y roles

    alt Es Administrador de Unidad
        Auth-->>Controller: Rol ADMIN_UNIDAD, unidadId = X
        Controller->>Service: contarProximosVencimientos(unidadId=X)
        Service->>Repo: countProximosVencimientos(unidadId=X, hoy, hoy+30días, estado='Activa')
    else Es Administrador Global / Auditor
        Auth-->>Controller: Rol ADMIN_GLOBAL, unidadId = null
        Controller->>Service: contarProximosVencimientos(unidadId=null)
        Service->>Repo: countProximosVencimientos(unidadId=null, hoy, hoy+30días, estado='Activa')
    end

    Repo-->>Service: Conteo total (ej. 5) y críticos (<= 7 días)
    Service-->>Controller: DesignacionVencimientosResumenDTO(totalPorVencer=5, umbralDias=30, ...)
    Controller-->>UI: 200 OK con JSON
    UI-->>Admin: Muestra Tarjeta / Alerta destacada en Dashboard con botón a Gestión > Vencimientos
```

---

## 3. Diagrama de Secuencia — Consulta del Listado en Gestión > Vencimientos

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador
    participant UI as Vista Gestión > Vencimientos
    participant Controller as DesignacionController
    participant Service as DesignacionService
    participant Repo as DesignacionRepository

    Admin->>UI: Navega a "Gestión > Vencimientos"
    UI->>Controller: GET /api/designaciones/vencimientos?incluirVencidas=false
    Controller->>Service: obtenerProximosVencimientos(usuario, incluirVencidas=false)

    Service->>Repo: findDesignacionesVencimiento(unidadId, hoy, hoy+30días, estado='Activa', incluirVencidas)
    Repo-->>Service: List<Designacion> + Cargos asociados

    loop Por cada designación activa
        Service->>Service: Calcular diasRestantes = ChronoUnit.DAYS.between(now, fechaFin)
    end

    Service-->>Controller: List<DesignacionVencimientoDTO> enriquecida y ordenada por diasRestantes ASC
    Controller-->>UI: 200 OK [ { designacionId, resolucion, docente, fechaFin, diasRestantes, unidad, cargos... } ]
    UI-->>Admin: Renderiza tabla con semáforo de días restantes y filtros
```
