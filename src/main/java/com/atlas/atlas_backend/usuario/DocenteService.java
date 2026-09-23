package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocenteService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Docente registrarDocente(DocenteRequest request, StringBuilder warningMessage) throws Exception {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Username ya existe");
        }

        // The institutional email must be unique across all users (check both tables if necessary,
        // though typically they shouldn't overlap if using the same email field in Usuario)
        if (usuarioRepository.existsByCorreo(request.getEmailInstitucional()) || 
            docenteRepository.existsByEmailInstitucional(request.getEmailInstitucional())) {
            throw new Exception("Correo institucional ya existe");
        }

        if (docenteRepository.existsByDni(request.getDni())) {
            throw new Exception("DNI ya existe");
        }

        if (docenteRepository.existsByCuil(request.getCuil())) {
            throw new Exception("CUIL ya existe");
        }

        Rol docenteRol = rolRepository.findByNombre("DOCENTE")
                .orElseThrow(() -> new Exception("Rol DOCENTE no encontrado"));

        String provisionalPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Docente docente = new Docente();
        docente.setUsername(request.getUsername());
        docente.setContrasenia(passwordEncoder.encode(provisionalPassword));
        docente.setCorreo(request.getEmailInstitucional()); // Store in Usuario as well
        docente.setApellido(request.getApellido());
        docente.setNombre(request.getNombre());
        docente.setActivo(true);
        docente.setDebeCambiarContrasenia(true);
        docente.setFechaReset(LocalDateTime.now());
        docente.setRol(docenteRol);

        // Docente specific fields
        docente.setDni(request.getDni());
        docente.setCuil(request.getCuil());
        docente.setFechaNac(request.getFechaNac());
        docente.setEmailInstitucional(request.getEmailInstitucional());
        docente.setFechaIngreso(request.getFechaIngreso());
        docente.setDomicilio(request.getDomicilio());
        docente.setTelefonos(request.getTelefonos());

        // Seniority Logic
        if (request.getAntigPrevia() != null) {
            docente.setAntigPrevia(request.getAntigPrevia());
        } else {
            docente.setAntigPrevia(0);
        }

        Docente savedUser = docenteRepository.save(docente);

        try {
            emailService.sendWelcomeDocente(savedUser.getEmailInstitucional(), provisionalPassword);
        } catch (Exception e) {
            if (warningMessage != null) {
                warningMessage.append("El docente fue creado, pero falló el envío del correo de bienvenida.");
            }
        }

        return savedUser;
    }
}
