package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.cambio.CambioService;
import com.atlas.atlas_backend.notification.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminGlobalService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminGlobalRepository adminGlobalRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CambioService cambioService;

    @Transactional
    public AdminGlobal registrarAdminGlobal(AdminGlobalRequest request, StringBuilder warningMessage) throws Exception {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Username ya existe");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new Exception("Correo ya existe");
        }

        Rol adminGlobalRol = rolRepository.findByNombre("Administrador Global")
                .or(() -> rolRepository.findByNombre("ADMIN_GLOBAL"))
                .orElseThrow(() -> new Exception("Rol Administrador Global no encontrado"));

        String provisionalPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        AdminGlobal adminGlobal = new AdminGlobal();
        adminGlobal.setUsername(request.getUsername());
        adminGlobal.setContrasenia(passwordEncoder.encode(provisionalPassword));
        adminGlobal.setCorreo(request.getCorreo());
        adminGlobal.setApellido(request.getApellido());
        adminGlobal.setNombre(request.getNombre());
        adminGlobal.setCargoNombre(request.getCargoNombre());
        adminGlobal.setActivo(true);
        adminGlobal.setDebeCambiarContrasenia(true);
        adminGlobal.setFechaReset(LocalDateTime.now());
        adminGlobal.setRol(adminGlobalRol);

        AdminGlobal savedUser = adminGlobalRepository.save(adminGlobal);

        try {
            emailService.sendWelcomeAdminGlobal(savedUser.getCorreo(), provisionalPassword);
        } catch (Exception e) {
            if (warningMessage != null) {
                warningMessage.append("El usuario fue creado, pero falló el envío del correo de bienvenida.");
            }
        }

        return savedUser;
    }

    @Transactional
    public AdminGlobal modificarAdminGlobal(@NonNull Integer id, AdminGlobalUpdateRequest request, String authUsername) {
        Usuario usuarioResponsable = usuarioRepository.findByUsername(authUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));

        AdminGlobal adminGlobal = adminGlobalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrador Global no encontrado"));

        String nuevoUsername = request.getUsername() != null ? request.getUsername().trim() : null;
        String nuevoCorreo = request.getCorreo() != null ? request.getCorreo().trim() : null;

        if (nuevoUsername != null && !nuevoUsername.equalsIgnoreCase(adminGlobal.getUsername())) {
            Optional<Usuario> existingUser = usuarioRepository.findByUsername(nuevoUsername);
            if (existingUser.isPresent() && !Objects.equals(existingUser.get().getId(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya se encuentra en uso");
            }
        }

        if (nuevoCorreo != null && !nuevoCorreo.equalsIgnoreCase(adminGlobal.getCorreo())) {
            if (usuarioRepository.existsByCorreoAndIdNot(nuevoCorreo, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya se encuentra en uso");
            }
        }

        // Auditar diferencias en cambios
        cambioService.registrarCambio("admin_globales", "username", adminGlobal.getUsername(), nuevoUsername, usuarioResponsable);
        cambioService.registrarCambio("admin_globales", "nombre", adminGlobal.getNombre(), request.getNombre(), usuarioResponsable);
        cambioService.registrarCambio("admin_globales", "apellido", adminGlobal.getApellido(), request.getApellido(), usuarioResponsable);
        cambioService.registrarCambio("admin_globales", "correo", adminGlobal.getCorreo(), nuevoCorreo, usuarioResponsable);
        cambioService.registrarCambio("admin_globales", "cargo_ag_nombre", adminGlobal.getCargoNombre(), request.getCargoNombre(), usuarioResponsable);

        // Aplicar cambios
        adminGlobal.setUsername(nuevoUsername);
        adminGlobal.setNombre(request.getNombre() != null ? request.getNombre().trim() : null);
        adminGlobal.setApellido(request.getApellido() != null ? request.getApellido().trim() : null);
        adminGlobal.setCorreo(nuevoCorreo);
        adminGlobal.setCargoNombre(request.getCargoNombre());

        return adminGlobalRepository.save(adminGlobal);
    }
}
