package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Transactional
    public AdminGlobal registrarAdminGlobal(AdminGlobalRequest request, StringBuilder warningMessage) throws Exception {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Username ya existe");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new Exception("Correo ya existe");
        }

        Rol adminGlobalRol = rolRepository.findByNombre("ADMIN_GLOBAL")
                .orElseThrow(() -> new Exception("Rol ADMIN_GLOBAL no encontrado"));

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
}
