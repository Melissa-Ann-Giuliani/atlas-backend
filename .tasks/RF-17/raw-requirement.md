# Raw Requirements
 
## RF17: Alta de un cargo
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de generar el alta de un cargo |
| **Actor** | Administrador Global o Administrador de Unidad |
| **Entrada** | El administrador ingresa a Gestión
Dentro del cuadro Cargos, selecciona Alta de Cargo
Ingresa:
Número de cargo
fecha de creación, 
origen, 
caracter, 
categoría, 
Dedicación (la cantidad de horas se auto completará dependiendo del valor de dedicación)
unidad, número de predecesor (por si se vincula con otro cargo antiguo) |
| **Condición** | El cargo no debe existir previamente (número de cargo único)
El cargo automáticamente se registra como “Libre” y 0 horas ocupadas
El cargo debe generarse sin actividades vinculadas, para su futura asignación en una designación de cargo |
| **Salida** | Queda registrado en el sistema el nuevo cargo, en estado “Libre”, disponible para su futura asignación  |