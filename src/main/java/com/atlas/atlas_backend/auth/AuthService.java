package com.atlas.atlas_backend.auth;

import com.atlas.atlas_backend.security.CustomUserDetailsService;
import com.atlas.atlas_backend.security.JwtUtil;
import com.atlas.atlas_backend.usuario.Rol;
import com.atlas.atlas_backend.usuario.RolRepository;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.atlas.atlas_backend.notification.EmailService;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public AuthResponse authenticate(AuthRequest authRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
        } catch (DisabledException e) {
            throw new Exception("Account is inactive, please contact support.", e);
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new Exception("Invalid username or password", e);
        }

        Usuario usuario = usuarioRepository.findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new Exception("User not found"));

        if (Boolean.TRUE.equals(usuario.getDebeCambiarContrasenia()) && usuario.getFechaReset() != null) {
            if (usuario.getFechaReset().plusHours(24).isBefore(LocalDateTime.now())) {
                throw new Exception("Provisional password has expired. Please request a new one.");
            }
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        return new AuthResponse(jwt);
    }

    public void register(RegisterRequest request) throws Exception {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Username already exists");
        }

        Usuario newUsuario = new Usuario();
        newUsuario.setUsername(request.getUsername());
        newUsuario.setContrasenia(passwordEncoder.encode(request.getPassword()));
        newUsuario.setCorreo(request.getEmail());
        newUsuario.setNombre(request.getNombre());
        newUsuario.setApellido(request.getApellido());
        newUsuario.setActivo(true);

        // Assign a default role, usually "USER" (or ID 1). Assuming ID 1 exists or
        // fetching by name.
        Rol defaultRol = rolRepository.findById(1)
                .orElseThrow(() -> new Exception("Default role not found"));
        newUsuario.setRol(defaultRol);

        usuarioRepository.save(newUsuario);
    }

    public void resetPassword(String username, String correo) throws Exception {
        Usuario usuario = usuarioRepository.findByUsernameAndCorreo(username, correo)
                .orElseThrow(() -> new Exception("No account found matching the provided credentials."));

        if (!usuario.getActivo()) {
            throw new Exception("Account is inactive, please contact support.");
        }

        String provisionalPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        usuario.setContrasenia(passwordEncoder.encode(provisionalPassword));
        usuario.setDebeCambiarContrasenia(true);
        usuario.setFechaReset(LocalDateTime.now());

        usuarioRepository.save(usuario);
        emailService.sendProvisionalPassword(usuario.getCorreo(), provisionalPassword);
    }

    public void adminResetPassword(Integer userId) throws Exception {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found."));

        if (!usuario.getActivo()) {
            throw new Exception("Cannot reset password for an inactive account.");
        }

        String provisionalPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        usuario.setContrasenia(passwordEncoder.encode(provisionalPassword));
        usuario.setDebeCambiarContrasenia(true);
        usuario.setFechaReset(LocalDateTime.now());

        usuarioRepository.save(usuario);
        emailService.sendProvisionalPassword(usuario.getCorreo(), provisionalPassword);
    }
}
