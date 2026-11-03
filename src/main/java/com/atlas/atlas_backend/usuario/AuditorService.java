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
public class AuditorService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditorRepository auditorRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CambioService cambioService;

    @Transactional
    public Auditor registrarAuditor(AuditorRequest request, StringBuilder warningMessage) throws Exception {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Username ya existe");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new Exception("Correo ya existe");
        }

        Rol auditorRol = rolRepository.findByNombre("Auditor")
                .or(() -> rolRepository.findByNombre("AUDITOR"))
                .orElseThrow(() -> new Exception("Rol Auditor no encontrado"));

        String provisionalPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Auditor auditor = new Auditor();
        auditor.setUsername(request.getUsername());
        auditor.setContrasenia(passwordEncoder.encode(provisionalPassword));
        auditor.setCorreo(request.getCorreo());
        auditor.setApellido(request.getApellido());
        auditor.setNombre(request.getNombre());
        auditor.setCargoNombre(request.getCargoNombre());
        auditor.setActivo(true);
        auditor.setDebeCambiarContrasenia(true);
        auditor.setFechaReset(LocalDateTime.now());
        auditor.setRol(auditorRol);

        Auditor savedUser = auditorRepository.save(auditor);

        try {
            emailService.sendWelcomeAuditor(savedUser.getCorreo(), provisionalPassword);
        } catch (Exception e) {
            if (warningMessage != null) {
                warningMessage.append("El usuario fue creado, pero falló el envío del correo de bienvenida.");
            }
        }

        return savedUser;
    }

    @Transactional
    public Auditor modificarAuditor(@NonNull Integer id, AuditorUpdateRequest request, String authUsername) {
        Usuario usuarioResponsable = usuarioRepository.findByUsername(authUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));

        Auditor auditor = auditorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auditor no encontrado"));

        String nuevoUsername = request.getUsername() != null ? request.getUsername().trim() : null;
        String nuevoCorreo = request.getCorreo() != null ? request.getCorreo().trim() : null;

        if (nuevoUsername != null && !nuevoUsername.equalsIgnoreCase(auditor.getUsername())) {
            Optional<Usuario> existingUser = usuarioRepository.findByUsername(nuevoUsername);
            if (existingUser.isPresent() && !Objects.equals(existingUser.get().getId(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya se encuentra en uso");
            }
        }

        if (nuevoCorreo != null && !nuevoCorreo.equalsIgnoreCase(auditor.getCorreo())) {
            if (usuarioRepository.existsByCorreoAndIdNot(nuevoCorreo, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya se encuentra en uso");
            }
        }

        // Auditar diferencias en cambios
        cambioService.registrarCambio("auditores", "username", auditor.getUsername(), nuevoUsername, usuarioResponsable);
        cambioService.registrarCambio("auditores", "nombre", auditor.getNombre(), request.getNombre(), usuarioResponsable);
        cambioService.registrarCambio("auditores", "apellido", auditor.getApellido(), request.getApellido(), usuarioResponsable);
        cambioService.registrarCambio("auditores", "correo", auditor.getCorreo(), nuevoCorreo, usuarioResponsable);
        cambioService.registrarCambio("auditores", "cargo_au_nombre", auditor.getCargoNombre(), request.getCargoNombre(), usuarioResponsable);

        // Aplicar cambios
        auditor.setUsername(nuevoUsername);
        auditor.setNombre(request.getNombre() != null ? request.getNombre().trim() : null);
        auditor.setApellido(request.getApellido() != null ? request.getApellido().trim() : null);
        auditor.setCorreo(nuevoCorreo);
        auditor.setCargoNombre(request.getCargoNombre());

        return auditorRepository.save(auditor);
    }
}
