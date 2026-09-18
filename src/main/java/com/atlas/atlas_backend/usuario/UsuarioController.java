package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.auth.AuthService;
import com.atlas.atlas_backend.error.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private AuthService authService;

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')") // Or equivalent check
    public ResponseEntity<?> adminResetPassword(@PathVariable Integer userId) {
        try {
            authService.adminResetPassword(userId);
            return ResponseEntity.ok(java.util.Map.of("message", "Password reset successfully for user " + userId + "."));
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
}
