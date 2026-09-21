# Solution Diagrams — RF-27

## 1. Sequence Diagram
This diagram details the interaction flow between the actors and system components when registering a new Global Administrator.

```mermaid
sequenceDiagram
    actor AdminGlobal as Admin Global (Actor)
    participant Auth as Spring Security
    participant Controller as UsuarioController
    participant Service as AdminGlobalService
    participant RepoUser as UsuarioRepository
    participant RepoAdmin as AdminGlobalRepository
    participant Email as EmailService
    participant SMTP as External SMTP

    AdminGlobal->>Auth: POST /api/usuarios/admin-global (JWT Token)
    Auth-->>Controller: Forward Request (if authorized)
    
    Controller->>Controller: Validate input fields
    alt Missing fields or invalid email
        Controller-->>AdminGlobal: 400 Bad Request
    else Input is valid
        Controller->>Service: registrarAdminGlobal(request)
        
        Service->>RepoUser: existsByUsername(username)
        RepoUser-->>Service: exists (Boolean)
        Service->>RepoUser: existsByCorreo(correo)
        RepoUser-->>Service: exists (Boolean)
        
        alt Username or Correo exists
            Service-->>Controller: throw Exception("already exists")
            Controller-->>AdminGlobal: 409 Conflict
        else User is new
            Service->>Service: Generate provisionalPassword
            Service->>Service: Encode password
            Service->>RepoAdmin: save(AdminGlobal)
            RepoAdmin-->>Service: AdminGlobal saved instance
            
            Service->>Email: sendWelcomeAdminGlobal(correo, provisionalPassword)
            alt Email sent successfully
                Email->>SMTP: mailSender.send(message)
                SMTP-->>Email: OK
                Email-->>Service: return
                Service-->>Controller: AdminGlobal saved instance
                Controller-->>AdminGlobal: 201 Created ("Admin Global registered successfully")
            else Email fails
                Email--xSMTP: Connection failed / Exception
                Email-->>Service: Exception caught
                Service-->>Controller: AdminGlobal saved instance (with warning flag)
                Controller-->>AdminGlobal: 201 Created ("User created but email failed to send")
            end
        end
    end
```

---

## 2. Class Diagram
This diagram outlines the static structure and main classes involved in the solution.

```mermaid
classDiagram
    class UsuarioController {
        - AuthService authService
        - AdminGlobalService adminGlobalService
        + registrarAdminGlobal(AdminGlobalRequest): ResponseEntity
    }

    class AdminGlobalService {
        - UsuarioRepository usuarioRepository
        - AdminGlobalRepository adminGlobalRepository
        - RolRepository rolRepository
        - PasswordEncoder passwordEncoder
        - EmailService emailService
        + registrarAdminGlobal(AdminGlobalRequest): AdminGlobal
    }

    class EmailService {
        - JavaMailSender mailSender
        + sendWelcomeAdminGlobal(to: String, provisionalPassword: String)
    }

    class AdminGlobalRequest {
        + String username
        + String correo
        + String apellido
        + String nombre
        + CargoAdminGlobal cargoNombre
    }

    class Usuario {
        <<Entity>>
        + Integer id
        + String username
        + String correo
        + String apellido
        + String nombre
        + String contrasenia
        + Boolean activo
        + Boolean debeCambiarContrasenia
        + LocalDateTime fechaReset
        + Rol rol
    }

    class AdminGlobal {
        <<Entity>>
        + CargoAdminGlobal cargoNombre
    }
    
    class CargoAdminGlobal {
        <<Enumeration>>
        DIRECCION_DE_PERSONAL
        DESPACHO
    }
    
    Usuario <|-- AdminGlobal : inheritance

    UsuarioController ..> AdminGlobalRequest : receives
    UsuarioController --> AdminGlobalService : delegates to
    AdminGlobalService --> AdminGlobal : creates
    AdminGlobalService --> EmailService : uses
```

---

## 3. Additional Diagrams
*(No additional diagrams are necessary for this specific flow at this time).*
