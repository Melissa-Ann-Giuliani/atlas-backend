package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Transactional
    public Auditor registrarAuditor(AuditorRequest request, StringBuilder warningMessage) throws Exception {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Username ya existe");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new Exception("Correo ya existe");
        }

        Rol auditorRol = rolRepository.findByNombre("AUDITOR")
                .orElseThrow(() -> new Exception("Rol AUDITOR no encontrado"));

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
}
