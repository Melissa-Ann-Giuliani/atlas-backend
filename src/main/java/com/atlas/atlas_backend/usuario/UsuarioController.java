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

    @GetMapping("/generar-password")
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

    @GetMapping("/admin-global/cargos")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<CargoAdminGlobal[]> getCargosAdminGlobal() {
        return ResponseEntity.ok(CargoAdminGlobal.values());
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
            if (e.getMessage().contains("ya existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
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
            if (e.getMessage().contains("ya existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

}
