# Raw Requirements
 
## RF30: Registrar un nuevo docente al sistema
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de registrar un nuevo docente al sistema. |
| **Actor** | Administrador Global o Administrador de Unidad |
| **Entrada** | El administrador ingresa a Gestión
Selecciona la opción Nuevo Usuario
Ingresa:
nombre de usuario, 
apellidos y nombres del nuevo usuario
Selecciona para Rol la opción Docente
Ingresa luego: 
dni, 
cuil, 
género, 
fecha de nacimiento, 
domicilio, 
email institucional, 
teléfonos, 
fecha de ingreso a la facultad, 
antigüedad |
| **Condición** | El docente no debe estar previamente registrado en el sistema 
Si el docente es de categoría regular, si tiene antigüedad previa, debe cargarse el valor correspondiente, caso contrario la antigüedad se inicia en 0 años. |
| **Salida** | El docente es cargado al sistema, y figura entre la lista de docentes de la facultad.
Se le enviará al correo un mail de confirmación, con su contraseña temporal, para que el usuario lo cambie luego dentro de su Perfil. |