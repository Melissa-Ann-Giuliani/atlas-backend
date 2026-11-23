package com.atlas.atlas_backend.auth;

import com.atlas.atlas_backend.usuario.Rol;
import com.atlas.atlas_backend.usuario.RolRepository;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@Transactional
public class JwtSecurityIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UsuarioRepository usuarioRepository;

        @Autowired
        private RolRepository rolRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Value("${jwt.secret:ThisIsAVerySecretKeyThatShouldBeStoredInEnvironmentVariablesAndNotHere}")
        private String secretKey;

        private Usuario testUser;
        private Rol rolAdminGlobal;
        private Rol rolAuditor;

        @BeforeEach
        void setUp() {
                rolAdminGlobal = rolRepository.findByNombre("Administrador Global")
                                .orElseGet(() -> {
                                        Rol r = new Rol();
                                        r.setNombre("Administrador Global");
                                        return rolRepository.save(r);
                                });

                rolAuditor = rolRepository.findByNombre("Auditor")
                                .orElseGet(() -> {
                                        Rol r = new Rol();
                                        r.setNombre("Auditor");
                                        return rolRepository.save(r);
                                });

                testUser = new Usuario();
                testUser.setUsername("jwt_test_user");
                testUser.setContrasenia(passwordEncoder.encode("secret123"));
                testUser.setCorreo("jwt_test@example.com");
                testUser.setNombre("Test");
                testUser.setApellido("User");
                testUser.setActivo(true);
                testUser.setRol(rolAdminGlobal);
                testUser.setTokenVersion(0);
                testUser = usuarioRepository.save(Objects.requireNonNull(testUser));
        }

        private String buildToken(String subject, Integer tokenVersion, Date issuedAt, Date expiration,
                        String signingSecret) {
                SecretKey key = Keys.hmacShaKeyFor(signingSecret.getBytes());
                Map<String, Object> claims = new HashMap<>();
                claims.put("role", "ROLE_ADMIN_GLOBAL");
                if (tokenVersion != null) {
                        claims.put("tokenVersion", tokenVersion);
                }
                return Jwts.builder()
                                .claims(claims)
                                .subject(subject)
                                .issuedAt(issuedAt)
                                .expiration(expiration)
                                .signWith(key)
                                .compact();
        }

        @Test
        void protectedEndpoint_missingToken_returns401() throws Exception {
                mockMvc.perform(get("/api/usuarios"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void protectedEndpoint_malformedToken_returns401() throws Exception {
                mockMvc.perform(get("/api/usuarios")
                                .header("Authorization", "Bearer not-a-valid-jwt"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void protectedEndpoint_tamperedSignature_returns401() throws Exception {
                String wrongSecret = "AnotherCompletelyDifferentSecretKeyForTestingInvalidSignature123456";
                String tamperedToken = buildToken(
                                testUser.getUsername(),
                                0,
                                new Date(System.currentTimeMillis() - 1000),
                                new Date(System.currentTimeMillis() + 60_000),
                                wrongSecret);

                mockMvc.perform(get("/api/usuarios")
                                .header("Authorization", "Bearer " + tamperedToken))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void protectedEndpoint_expiredToken_returns401() throws Exception {
                String expiredToken = buildToken(
                                testUser.getUsername(),
                                0,
                                new Date(System.currentTimeMillis() - 120_000),
                                new Date(System.currentTimeMillis() - 1_000),
                                secretKey);

                mockMvc.perform(get("/api/usuarios")
                                .header("Authorization", "Bearer " + expiredToken))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void protectedEndpoint_outdatedTokenVersion_returns401() throws Exception {
                String validTokenV0 = buildToken(
                                testUser.getUsername(),
                                0,
                                new Date(System.currentTimeMillis() - 1000),
                                new Date(System.currentTimeMillis() + 60_000),
                                secretKey);

                testUser.incrementTokenVersion();
                usuarioRepository.save(Objects.requireNonNull(testUser));

                mockMvc.perform(get("/api/usuarios")
                                .header("Authorization", "Bearer " + validTokenV0))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void renew_missingOrBadFormatHeader_returns401() throws Exception {
                mockMvc.perform(post("/api/auth/renew"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));

                mockMvc.perform(post("/api/auth/renew")
                                .header("Authorization", "Basic dXNlcjpwYXNz"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void renew_tamperedSignature_returns401() throws Exception {
                String wrongSecret = "AnotherCompletelyDifferentSecretKeyForTestingInvalidSignature123456";
                String tamperedToken = buildToken(
                                testUser.getUsername(),
                                0,
                                new Date(System.currentTimeMillis() - 1000),
                                new Date(System.currentTimeMillis() + 60_000),
                                wrongSecret);

                mockMvc.perform(post("/api/auth/renew")
                                .header("Authorization", "Bearer " + tamperedToken))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void renew_expiredToken_returns401() throws Exception {
                String expiredToken = buildToken(
                                testUser.getUsername(),
                                0,
                                new Date(System.currentTimeMillis() - 120_000),
                                new Date(System.currentTimeMillis() - 1_000),
                                secretKey);

                mockMvc.perform(post("/api/auth/renew")
                                .header("Authorization", "Bearer " + expiredToken))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void renew_inactiveUser_returns401() throws Exception {
                String validToken = buildToken(
                                testUser.getUsername(),
                                0,
                                new Date(System.currentTimeMillis() - 1000),
                                new Date(System.currentTimeMillis() + 60_000),
                                secretKey);

                testUser.setActivo(false);
                usuarioRepository.save(Objects.requireNonNull(testUser));

                mockMvc.perform(post("/api/auth/renew")
                                .header("Authorization", "Bearer " + validToken))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void renew_outdatedTokenVersion_returns401() throws Exception {
                String validToken = buildToken(
                                testUser.getUsername(),
                                0,
                                new Date(System.currentTimeMillis() - 1000),
                                new Date(System.currentTimeMillis() + 60_000),
                                secretKey);

                testUser.incrementTokenVersion();
                usuarioRepository.save(Objects.requireNonNull(testUser));

                mockMvc.perform(post("/api/auth/renew")
                                .header("Authorization", "Bearer " + validToken))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
        }

        @Test
        void renew_validToken_returns200WithFreshRoleFromDb() throws Exception {
                String validToken = buildToken(
                                testUser.getUsername(),
                                0,
                                new Date(System.currentTimeMillis() - 1000),
                                new Date(System.currentTimeMillis() + 60_000),
                                secretKey);

                // Update role in DB to verify renew reads fresh role from DB instead of copying
                // old claims
                testUser.setRol(rolAuditor);
                usuarioRepository.save(Objects.requireNonNull(testUser));

                mockMvc.perform(post("/api/auth/renew")
                                .header("Authorization", "Bearer " + validToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").isNotEmpty())
                                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                                .andExpect(jsonPath("$.rol").value("AUDITOR"));
        }
}
