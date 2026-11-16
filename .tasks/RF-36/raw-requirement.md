# Raw Requirements
 
## RF36: Mantener un control sobre los próximos vencimientos de designaciones
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe mantener un control constante sobre los próximos vencimientos de ciertas designaciones, mostrando alertas de cuánto falta para el vencimiento. |
| **Actor** | Administrador Global o Administrador de Unidad |
| **Entrada** | El administrador ingresa a Gestión, selecciona Vencimientos |
| **Condición** | Las designaciones deben tener fecha de finalización definidas
La fecha de finalización debe encontrarse dentro de un umbral configurado (por ejemplo, 30 días antes del vencimiento)
El administrador de unidad solamente puede visualizar información de docentes de su unidad  |
| **Salida** | Se le muestra al administrador una lista de todas las designaciones a vencer, indicando los docentes pertenecientes, y los días restantes antes de vencer la designación correspondiente.
Además, al ingresar un administrador al sistema, se le muestra un alerta, indicando la cantidad de designaciones por vencer. |