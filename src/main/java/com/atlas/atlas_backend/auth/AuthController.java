package com.atlas.atlas_backend.auth;

import com.atlas.atlas_backend.error.ErrorResponse;
import com.atlas.atlas_backend.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest, HttpServletRequest request) {
        String ip = getClientIP(request);

        if (loginAttemptService.isBlocked(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Too many failed attempts. Please try again later."));
        }

        if (authRequest.getUsername() == null || authRequest.getUsername().trim().isEmpty() ||
            authRequest.getPassword() == null || authRequest.getPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Username and password are required"));
        }

        try {
            AuthResponse response = authService.authenticate(authRequest);
            loginAttemptService.loginSucceeded(ip);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loginAttemptService.loginFailed(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIP(httpRequest);

        if (loginAttemptService.isBlocked(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Too many failed attempts. Please try again later."));
        }

        if (request.getUsername() == null || request.getUsername().trim().isEmpty() ||
            request.getCorreo() == null || request.getCorreo().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Username and correo are required"));
        }

        try {
            authService.resetPassword(request.getUsername(), request.getCorreo());
            loginAttemptService.loginSucceeded(ip);
            return ResponseEntity.ok(java.util.Map.of("message", "Provisional password issued."));
        } catch (Exception e) {
            loginAttemptService.loginFailed(ip);
            String msg = e.getMessage();
            if (msg.contains("inactive")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(msg));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("No account found matching the provided credentials."));
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
