package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.unidad.UnidadRepository;
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

    @Autowired
    private com.atlas.atlas_backend.cambio.CambioService cambioService;

    @Transactional
    public AdminUnidad registrarAdminUnidad(AdminUnidadRequest request, StringBuilder warningMessage) throws Exception {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Username ya existe");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new Exception("Correo ya existe");
        }

        Rol adminUnidadRol = rolRepository.findByNombre("Administrador de Unidad")
                .or(() -> rolRepository.findByNombre("ADMIN_UNIDAD"))
                .orElseThrow(() -> new Exception("Rol Administrador de Unidad no encontrado"));

        Integer unidadId = request.getUnidadId();
        if (unidadId == null) {
            throw new Exception("El ID de la unidad no puede ser nulo");
        }

        Unidad unidad = unidadRepository.findById(unidadId)
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

    @Transactional
    public AdminUnidad modificarAdminUnidad(@NonNull Integer id, AdminUnidadUpdateRequest request,
            String authUsername) {
        Usuario usuarioResponsable = usuarioRepository.findByUsername(authUsername)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));

        AdminUnidad adminUnidad = adminUnidadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Administrador de Unidad no encontrado"));

        String nuevoUsername = request.getUsername() != null ? request.getUsername().trim() : null;
        String nuevoCorreo = request.getCorreo() != null ? request.getCorreo().trim() : null;

        if (nuevoUsername != null && !nuevoUsername.equalsIgnoreCase(adminUnidad.getUsername())) {
            Optional<Usuario> existingUser = usuarioRepository.findByUsername(nuevoUsername);
            if (existingUser.isPresent() && !Objects.equals(existingUser.get().getId(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya se encuentra en uso");
            }
        }

        if (nuevoCorreo != null && !nuevoCorreo.equalsIgnoreCase(adminUnidad.getCorreo())) {
            if (usuarioRepository.existsByCorreoAndIdNot(nuevoCorreo, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya se encuentra en uso");
            }
        }

        Integer nuevaUnidadId = request.getUnidadId();
        if (nuevaUnidadId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La unidad es obligatoria");
        }

        Unidad nuevaUnidad = unidadRepository.findById(nuevaUnidadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad no encontrada"));

        // Auditar diferencias en cambios
        cambioService.registrarCambio("admin_de_unidad", "username", adminUnidad.getUsername(), nuevoUsername,
                usuarioResponsable);
        cambioService.registrarCambio("admin_de_unidad", "nombre", adminUnidad.getNombre(), request.getNombre(),
                usuarioResponsable);
        cambioService.registrarCambio("admin_de_unidad", "apellido", adminUnidad.getApellido(), request.getApellido(),
                usuarioResponsable);
        cambioService.registrarCambio("admin_de_unidad", "correo", adminUnidad.getCorreo(), nuevoCorreo,
                usuarioResponsable);
        cambioService.registrarCambio("admin_de_unidad", "cargo_ad_nombre", adminUnidad.getCargoNombre(),
                request.getCargoNombre(), usuarioResponsable);
        cambioService.registrarCambio("admin_de_unidad", "unidad_id",
                adminUnidad.getUnidad() != null ? adminUnidad.getUnidad().getUnidadId() : null, nuevaUnidadId,
                usuarioResponsable);

        // Aplicar cambios
        adminUnidad.setUsername(nuevoUsername);
        adminUnidad.setNombre(request.getNombre() != null ? request.getNombre().trim() : null);
        adminUnidad.setApellido(request.getApellido() != null ? request.getApellido().trim() : null);
        adminUnidad.setCorreo(nuevoCorreo);
        adminUnidad.setCargoNombre(request.getCargoNombre());
        adminUnidad.setUnidad(nuevaUnidad);

        return adminUnidadRepository.save(adminUnidad);
    }
}
