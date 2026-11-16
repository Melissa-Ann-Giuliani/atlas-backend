# Raw Requirements
 
## RF11: Asignar funciones a un cargo perteneciente a una designación
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe permitir asignar funciones o actividades a un cargo perteneciente a una designación de un docente |
| **Actor** | Administrador Global, Administrador de Unidad |
| **Entrada** | Entra a Trámite, luego Designaciones, ingresa los datos del docente y la designación (RF09), y selecciona el botón “Asignar cargo”, luego haber elegido un cargo “Libre” (RF10)
Para la primera función, ingresa:
Cantidad de horas
Nombre de Materia
Al crear una nueva función, ingresa nombre de función, tipo, y cantidad de horas, seleccionando la materia vinculada a la función
Una vez creadas las funciones, presiona sobre el botón “Generar Documento” |
| **Condición** | La suma de horas de todas las funciones asignadas al cargo no puede superar la cantidad de horas correspondientes a la dedicación del cargo |
| **Salida** | Las funciones creadas quedan asignadas al cargo, actualizando la cantidad de horas ocupadas del cargo como la sumatoria de horas de todas las funciones.
Consecuentemente, queda registrado en el sistema la designación del docente, generando la documentación adecuada, vinculándola al docente, y dando la posibilidad de imprimir dicha documentación. 
El estado del docente se actualiza automáticamente, quedando en el sistema con el nuevo estado “Activo” y se manda una notificación al docente solicitante.  |