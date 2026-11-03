# Raw Requirements
 
## RF26: Baja de una materia
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de generar la baja lógica de una materia, pasando su estado a "Dada de baja" para inhabilitarla de futuras ofertas académicas y asignaciones, preservando la integridad referencial y el histórico académico previo. |
| **Actor** | Administrador Global o Administrador de Unidad |
| **Entrada** | El administrador ingresa a Gestión. Dentro del cuadro Materia, selecciona Baja de Materia y confirma la operación sobre la materia seleccionada. |
| **Condición** | 1. La materia debe existir previamente. 2. La materia no debe encontrarse ya en estado "Dada de baja". 3. La materia no debe tener cátedras activas asociadas. 4. Si el actor es Administrador de Unidad, la materia debe pertenecer a su unidad académica. |
| **Salida** | Queda registrada la baja lógica de la materia en el sistema (estado "Dada de baja"), mostrando un mensaje de confirmación y retornando el detalle actualizado de la materia. |