package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.auth.AuthService;
import com.atlas.atlas_backend.error.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminGlobalService adminGlobalService;

    @Autowired
    private AdminUnidadService adminUnidadService;

    @Autowired
    private AuditorService auditorService;

    @Autowired
    private DocenteService docenteService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminGlobalRepository adminGlobalRepository;

    @Autowired
    private AdminUnidadRepository adminUnidadRepository;

    @Autowired
    private AuditorRepository auditorRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD') or hasRole('AUDITOR')")
    public ResponseEntity<org.springframework.data.domain.Page<UsuarioListDTO>> getUsuarios(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String rol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        String cleanSearch = (search != null && search.trim().isEmpty()) ? null : (search != null ? search.trim() : null);
        String cleanRol = (rol != null && rol.trim().isEmpty()) ? null : (rol != null ? rol.trim() : null);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Usuario> usuariosPage = usuarioRepository.searchUsuarios(cleanSearch, cleanRol, pageable);

        org.springframework.data.domain.Page<UsuarioListDTO> dtoPage = usuariosPage.map(u -> {
            UsuarioListDTO dto = new UsuarioListDTO();
            dto.setId(u.getId());
            dto.setUsuarioId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setNombre(u.getNombre());
            dto.setApellido(u.getApellido());
            dto.setCorreo(u.getCorreo());
            dto.setActivo(u.getActivo());
            dto.setRol(u.getRol() != null ? u.getRol().getNombre() : null);

            // Populate specific fields if available
            adminGlobalRepository.findById(Objects.requireNonNull(u.getId())).ifPresent(ag -> {
                if (ag.getCargoNombre() != null) {
                    dto.setCargo(ag.getCargoNombre().name());
                }
            });

            adminUnidadRepository.findById(Objects.requireNonNull(u.getId())).ifPresent(au -> {
                if (au.getCargoNombre() != null) {
                    dto.setCargo(au.getCargoNombre().name());
                }
                if (au.getUnidad() != null) {
                    dto.setUnidad(au.getUnidad().getUnidadNombre());
                }
            });

            auditorRepository.findById(Objects.requireNonNull(u.getId())).ifPresent(aud -> {
                if (aud.getCargoNombre() != null) {
                    dto.setCargo(aud.getCargoNombre().name());
                }
            });

            docenteRepository.findById(Objects.requireNonNull(u.getId())).ifPresent(doc -> {
                if (doc.getDni() != null) {
                    dto.setDni(String.valueOf(doc.getDni()));
                }
            });

            return dto;
        });

        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD') or hasRole('AUDITOR')")
    public ResponseEntity<?> getUsuarioById(@PathVariable @NonNull Integer id) {
        return usuarioRepository.findById(id).map(u -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", u.getId());
            map.put("usuarioId", u.getId());
            map.put("username", u.getUsername());
            map.put("nombre", u.getNombre());
            map.put("apellido", u.getApellido());
            map.put("correo", u.getCorreo());
            map.put("activo", u.getActivo());
            map.put("rol", u.getRol() != null ? u.getRol().getNombre() : null);

            adminGlobalRepository.findById(id).ifPresent(ag -> {
                if (ag.getCargoNombre() != null) {
                    map.put("cargo", ag.getCargoNombre().name());
                    map.put("cargoNombre", ag.getCargoNombre().name());
                }
            });

            adminUnidadRepository.findById(id).ifPresent(au -> {
                if (au.getCargoNombre() != null) {
                    map.put("cargo", au.getCargoNombre().name());
                    map.put("cargoNombre", au.getCargoNombre().name());
                }
                if (au.getUnidad() != null) {
                    map.put("unidadId", au.getUnidad().getUnidadId());
                    map.put("unidad", au.getUnidad().getUnidadNombre());
                    if (au.getUnidad().getTipoUnidad() != null) {
                        map.put("dependencia", au.getUnidad().getTipoUnidad().getId());
                        map.put("tipoUnidadId", au.getUnidad().getTipoUnidad().getId());
                    }
                }
            });

            auditorRepository.findById(id).ifPresent(aud -> {
                if (aud.getCargoNombre() != null) {
                    map.put("cargo", aud.getCargoNombre().name());
                    map.put("cargoNombre", aud.getCargoNombre().name());
                }
            });

            docenteRepository.findById(id).ifPresent(doc -> {
                if (doc.getDni() != null) {
                    map.put("dni", String.valueOf(doc.getDni()));
                }
                if (doc.getCuil() != null) {
                    map.put("cuil", String.valueOf(doc.getCuil()));
                }
                if (doc.getTipoDni() != null) {
                    map.put("tipoDni", doc.getTipoDni());
                }
                if (doc.getDomicilio() != null) {
                    map.put("domicilio", doc.getDomicilio());
                }
                if (doc.getEmailInstitucional() != null) {
                    map.put("emailInstitucional", doc.getEmailInstitucional());
                }
                if (doc.getFechaIngreso() != null) {
                    map.put("fechaIngreso", doc.getFechaIngreso().toString());
                }
                if (doc.getAntigPrevia() != null) {
                    map.put("antigPrevia", doc.getAntigPrevia());
                }
            });

            return ResponseEntity.ok((Object) map);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping({ "/generar-password", "/generar-contrasenia" })
    public ResponseEntity<?> generarPassword() {
        // Genera una contraseña aleatoria y segura
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder contrasenia = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            contrasenia.append(chars.charAt(random.nextInt(chars.length())));
        }
        return ResponseEntity.ok(java.util.Map.of("password", contrasenia.toString()));
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')") // Or equivalent check
    public ResponseEntity<?> adminResetPassword(@PathVariable @NonNull Integer userId) {
        try {
            authService.adminResetPassword(userId);
            return ResponseEntity
                    .ok(java.util.Map.of("message", "Contraseña generada con éxito para usuario " + userId + "."));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg.contains("inactive")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(msg));
            } else if (msg.contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(msg));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(msg));
        }
    }

    @PostMapping("/admin-global")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<?> registrarAdminGlobal(@Valid @RequestBody AdminGlobalRequest request) {
        try {
            StringBuilder warningMessage = new StringBuilder();
            AdminGlobal user = adminGlobalService.registrarAdminGlobal(request, warningMessage);

            if (warningMessage.length() > 0) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(java.util.Map.of("message", "Usuario registrado, pero: " + warningMessage.toString(),
                                "usuario", user));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(java.util.Map.of("message", "Administrador Global registrado exitosamente", "usuario", user));
        } catch (Exception e) {
            if (e.getMessage().contains("ya existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/admin-global/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL')")
    public ResponseEntity<?> modificarAdminGlobal(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody AdminGlobalUpdateRequest request,
            java.security.Principal principal) {
        try {
            String authUsername = principal != null ? principal.getName() : null;
            if (authUsername == null) {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    authUsername = auth.getName();
                }
            }

            AdminGlobal updatedAdmin = adminGlobalService.modificarAdminGlobal(id, request, authUsername);
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Administrador Global modificado exitosamente");
            response.put("usuario", updatedAdmin);
            return ResponseEntity.ok(response);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            String msg = e.getReason() != null ? e.getReason() : e.getMessage();
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/admin-global/cargos")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<CargoAdminGlobal[]> getCargosAdminGlobal() {
        return ResponseEntity.ok(CargoAdminGlobal.values());
    }

    @GetMapping("/admin-unidad/cargos")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<java.util.List<java.util.Map<String, String>>> getCargosAdminUnidad() {
        java.util.List<java.util.Map<String, String>> cargos = java.util.Arrays.stream(CargoAdminUnidad.values())
                .map(c -> java.util.Map.of(
                        "value", c.name(),
                        "nombre", c.getLabel(),
                        "label", c.getLabel()))
                .toList();
        return ResponseEntity.ok(cargos);
    }

    @GetMapping("/auditor/cargos")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<java.util.List<java.util.Map<String, String>>> getCargosAuditor() {
        java.util.List<java.util.Map<String, String>> cargos = java.util.Arrays.stream(CargoAuditor.values())
                .map(c -> java.util.Map.of(
                        "value", c.name(),
                        "nombre", c.getLabel(),
                        "label", c.getLabel()))
                .toList();
        return ResponseEntity.ok(cargos);
    }

    @GetMapping("/docente/tipos-dni")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD')")
    public ResponseEntity<java.util.List<java.util.Map<String, String>>> getTiposDniDocente() {
        java.util.List<java.util.Map<String, String>> tipos = java.util.Arrays.stream(TipoDni.values())
                .map(t -> java.util.Map.of(
                        "value", t.name(),
                        "nombre", t.getLabel(),
                        "label", t.getLabel()))
                .toList();
        return ResponseEntity.ok(tipos);
    }

    @PostMapping("/admin-unidad")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<?> registrarAdminUnidad(@Valid @RequestBody AdminUnidadRequest request) {
        try {
            StringBuilder warningMessage = new StringBuilder();
            AdminUnidad user = adminUnidadService.registrarAdminUnidad(request, warningMessage);

            if (warningMessage.length() > 0) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(java.util.Map.of("message", "Usuario registrado, pero: " + warningMessage.toString(),
                                "usuario", user));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(java.util.Map.of("message", "Administrador de Unidad registrado exitosamente", "usuario",
                            user));
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().contains("ya existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
            }
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(msg));
        }
    }

    @PostMapping("/auditor")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<?> registrarAuditor(@Valid @RequestBody AuditorRequest request) {
        try {
            StringBuilder warningMessage = new StringBuilder();
            Auditor user = auditorService.registrarAuditor(request, warningMessage);

            if (warningMessage.length() > 0) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(java.util.Map.of("message", "Usuario registrado, pero: " + warningMessage.toString(),
                                "usuario", user));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(java.util.Map.of("message", "Auditor registrado exitosamente", "usuario", user));
        } catch (Exception e) {
            if (e.getMessage().contains("ya existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/docente")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD')")
    public ResponseEntity<?> registrarDocente(@Valid @RequestBody DocenteRequest request) {
        try {
            StringBuilder warningMessage = new StringBuilder();
            Docente user = docenteService.registrarDocente(request, warningMessage);

            if (warningMessage.length() > 0) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(java.util.Map.of("message", "Usuario registrado, pero: " + warningMessage.toString(),
                                "usuario", user));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(java.util.Map.of("message", "Docente registrado exitosamente", "usuario", user));
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().contains("ya existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
            }
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(msg));
        }
    }

}
