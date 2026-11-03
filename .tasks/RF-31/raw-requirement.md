# Raw Requirements
 
## RF31: Realizar modificaciones sobre información de cada docente
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe permitir realizar modificaciones o actualizaciones sobre información de cada docente. |
| **Actor** | Administrador Global o Administrador de Unidad |
| **Entrada** | Ingresa a Gestión, selecciona Modificar Usuario
Busca al docente a modificar
Selecciona sector de modificación (datos personales, datos de contacto, cargo, funciones)
Ingresa nuevo valor |
| **Condición** | El registro seleccionado debe existir previamente en el sistema 
El Administrador de Unidad sólo puede modificar información de docentes de su propia unidad. |
| **Salida** | La información queda actualizada y se genera un registro en el historial de cambios, indicando valor anterior, valor nuevo, usuario responsable y fecha/hora del cambio. |