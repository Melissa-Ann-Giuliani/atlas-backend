# spec-02: Alerta y resumen de designaciones por vencer al ingresar al sistema

## Overview
Proveer un endpoint de agregación liviano (`GET /api/designaciones/vencimientos/resumen` o `/api/dashboard/vencimientos/resumen`) para notificar de manera reactiva e inmediata al Administrador (Global o de Unidad) al momento de ingresar a la plataforma (Dashboard / Inicio), indicando la cantidad total de designaciones que expirarán dentro del período umbral fijo de 30 días (Decisión B1 - Opción B y Decisión R1 - Opción A).

---

## User Story

**Como** Administrador del sistema  
**Quiero** ver una alerta o tarjeta informativa con el número de designaciones próximas a vencer inmediatamente al ingresar al Dashboard del sistema  
**Para** tomar conocimiento rápido del estado de caducidad con acceso directo a la vista de gestión sin requerir navegación manual anticipada.

---

## Contract & Endpoints

### Endpoint: `GET /api/designaciones/vencimientos/resumen`

#### Request Query Parameters:
*No requiere parámetros obligatorios.* El umbral evaluado es fijo en **30 días**. Opcionalmente acepta `unidadId` (solo para Administrador Global).

#### Responses:
- **`200 OK`**: Retorna el conteo y metadatos del cálculo de vencimientos aplicable al usuario autenticado.
- **`401 Unauthorized`**: Usuario no autenticado.

#### Payload de Respuesta (`DesignacionVencimientosResumenDTO`):
```json
{
  "totalPorVencer": 5,
  "umbralDias": 30,
  "unidadId": 2,
  "unidadNombre": "Facultad de Ingeniería",
  "tieneVencimientosCriticos": true,
  "totalCriticos": 2
}
```
*Nota: `tieneVencimientosCriticos` es verdadero si hay vencimientos en los próximos 7 días.*

---

## Scenarios (Acceptance Criteria in Gherkin)

### Scenario 1: Administrador Global ingresa al sistema y tiene designaciones por vencer en los próximos 30 días
```gherkin
Given que el usuario "admin_global" se autentica exitosamente
And existen 7 designaciones en toda la universidad en estado "Activa" con fechaFin dentro de los próximos 30 días
When el cliente solicita "GET /api/designaciones/vencimientos/resumen"
Then el sistema responde 200 OK
And el campo "totalPorVencer" es 7
And el campo "umbralDias" es 30
And el campo "unidadId" es null
```

### Scenario 2: Administrador de Unidad solo contabiliza designaciones de su unidad
```gherkin
Given que el usuario "admin_exactas" pertenece a la unidad "Facultad de Ciencias Exactas" (ID: 4)
And existen 10 designaciones por vencer en toda la universidad, de las cuales 3 pertenecen a la unidad 4
When el cliente solicita "GET /api/designaciones/vencimientos/resumen"
Then el sistema responde 200 OK
And el campo "totalPorVencer" es 3
And el campo "unidadId" es 4
And el campo "unidadNombre" es "Facultad de Ciencias Exactas"
```

### Scenario 3: Administrador sin designaciones próximas a vencer
```gherkin
Given que el usuario es Administrador de Unidad (ID: 5)
And ninguna designación activa de la unidad 5 vence en los próximos 30 días
When el cliente solicita "GET /api/designaciones/vencimientos/resumen"
Then el sistema responde 200 OK
And el campo "totalPorVencer" es 0
And "tieneVencimientosCriticos" es false
And "totalCriticos" es 0
```
