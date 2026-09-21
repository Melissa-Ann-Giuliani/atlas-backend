package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.unidad.UnidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AdminUnidadService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminUnidadRepository adminUnidadRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UnidadRepository unidadRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Transactional
    public AdminUnidad registrarAdminUnidad(AdminUnidadRequest request, StringBuilder warningMessage) throws Exception {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Username ya existe");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new Exception("Correo ya existe");
        }

        Rol adminUnidadRol = rolRepository.findByNombre("ADMIN_UNIDAD")
                .orElseThrow(() -> new Exception("Rol ADMIN_UNIDAD no encontrado"));

        Unidad unidad = unidadRepository.findById(request.getUnidadId())
                .orElseThrow(() -> new Exception("Unidad no encontrada"));

        String provisionalPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        AdminUnidad adminUnidad = new AdminUnidad();
        adminUnidad.setUsername(request.getUsername());
        adminUnidad.setContrasenia(passwordEncoder.encode(provisionalPassword));
        adminUnidad.setCorreo(request.getCorreo());
        adminUnidad.setApellido(request.getApellido());
        adminUnidad.setNombre(request.getNombre());
        adminUnidad.setCargoNombre(request.getCargoNombre());
        adminUnidad.setUnidad(unidad);
        adminUnidad.setActivo(true);
        adminUnidad.setDebeCambiarContrasenia(true);
        adminUnidad.setFechaReset(LocalDateTime.now());
        adminUnidad.setRol(adminUnidadRol);

        AdminUnidad savedUser = adminUnidadRepository.save(adminUnidad);

        try {
            emailService.sendWelcomeAdminUnidad(savedUser.getCorreo(), provisionalPassword);
        } catch (Exception e) {
            if (warningMessage != null) {
                warningMessage.append("El usuario fue creado, pero falló el envío del correo de bienvenida.");
            }
        }

        return savedUser;
    }
}
