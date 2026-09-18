# Raw Requirements
 
## RF03: Generar una nueva contraseña para usuario
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe permitir al usuario generar una nueva contraseña en caso de que se lo haya olvidado. |
| **Actor** | Usuario, Administrador Global |
| **Entrada** | Nombre de usuario, correo electrónico, contraseña nueva |
| **Condición** | El usuario solicita una nueva contraseña, ingresando su nombre de usuario y correo electrónico, y el sistema verifica que exista en la base de datos.
De forma alternativa, el administrador global autoriza el reseteo desde Gestión -> Modificar Usuario, pero no puede verlo en texto claro. |
| **Salida** | Se le otorga una contraseña provisoria al usuario, para que pueda cambiarla a una nueva. |