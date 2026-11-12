# Raw Requirements
 
## RF10: Vincular un cargo a una designación de un docente
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe permitir vincular un cargo a una designación de un docente |
| **Actor** | Administrador Global, Administrador de Unidad |
| **Entrada** | Entra a Trámite, luego Designaciones, 
Ingresa los datos del docente y la designación, y selecciona el botón “Asignar cargo”
Elige uno de los cargos visibles en la lista |
| **Condición** | Solo se puede asignar cargos ya creados
El administrador, luego de haber generado una nueva designación en  Trámites, elige entre los distintos cargos en estado “Libre” disponibles
En caso de necesitar un nuevo cargo, puede ser redirigido a Gestión para un alta de cargo (RF17) a través del botón “Crear Cargo” |
| **Salida** | La designación creada se le vincula el cargo elegido, el cual pasa a estado “Asignado” y queda registrado en el sistema  |