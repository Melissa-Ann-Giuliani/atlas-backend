# Raw Requirements
 
## RF39: Mantener un historial actualizado de todo cambio generado sobre la información el sistema
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de mostrar un historial actualizado de todo cambio generado sobre el sistema. |
| **Actor** | Administrador Global, Administrador de Unidad o Auditor |
| **Entrada** | Ingresa a Historial, selecciona Cambios |
| **Condición** | El usuario debe estar autenticado en el sistema y contar con el rol de Administrador Global, Administrador de Unidad o Auditor.
Si es Administrador de Unidad o Auditor, sólo podrá visualizar los cambios correspondientes a los docentes de su propia unidad
Debe existir al menos un registro de cambio sobre la información del/los docente(s) consultado(s).
Puede aplicar criterios de filtrado: apellidos o nombres, rango de fechas, tipo de cambio (alta, modificación, baja), usuario que realizó el cambio.  |
| **Salida** | Listado cronológico (por defecto, del más reciente al más antiguo) de los cambios realizados, mostrando para cada registro: Fecha y hora del cambio, Administrador responsable del cambio, Categoría, Campo(s) modificado(s). Valor anterior y valor nuevo. Tipo de operación (creación, actualización, eliminación). |