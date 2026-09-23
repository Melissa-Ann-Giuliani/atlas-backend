package com.atlas.atlas_backend.auth;

import com.atlas.atlas_backend.security.LoginAttemptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import java.util.Objects;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@org.springframework.test.context.ActiveProfiles("test")
@Import(LoginAttemptService.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.atlas.atlas_backend.security.JwtUtil jwtUtil;

    @MockBean
    private com.atlas.atlas_backend.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @BeforeEach
    public void setup() {
        loginAttemptService.loginSucceeded("127.0.0.1");
    }

    @Test
    public void testScenario1_Success() throws Exception {
        AuthRequest request = new AuthRequest("jdoe", "secret123");
        when(authService.authenticate(any(AuthRequest.class))).thenReturn(new AuthResponse("fake-jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));
    }

    @Test
    public void testScenario2_InvalidCredentials() throws Exception {
        AuthRequest request = new AuthRequest("jdoe", "wrongpassword");
        when(authService.authenticate(any(AuthRequest.class))).thenThrow(new Exception("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    public void testScenario3_AccountInactive() throws Exception {
        AuthRequest request = new AuthRequest("jsmith", "any");
        when(authService.authenticate(any(AuthRequest.class)))
                .thenThrow(new Exception("Account is inactive, please contact support."));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is inactive, please contact support."));
    }

    @Test
    public void testScenario4_EmptyCredentials() throws Exception {
        AuthRequest request = new AuthRequest("", "secret123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username and password are required"));
    }

    @Test
    public void testScenario5_RateLimitExceeded() throws Exception {
        AuthRequest request = new AuthRequest("jdoe", "wrongpassword");
        when(authService.authenticate(any(AuthRequest.class))).thenThrow(new Exception("Invalid username or password"));

        // 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should return 429
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many failed attempts. Please try again later."));
    }

    @Test
    public void testScenario1_ResetPassword_Success() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setUsername("jdoe");
        request.setCorreo("jdoe@example.com");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Provisional password issued."));
    }

    @Test
    public void testScenario2_ResetPassword_InvalidCredentials() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setUsername("jdoe");
        request.setCorreo("wrong@example.com");
        
        org.mockito.Mockito.doThrow(new Exception("No account found matching the provided credentials."))
                .when(authService).resetPassword(any(String.class), any(String.class));

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No account found matching the provided credentials."));
    }

    @Test
    public void testScenario3_ResetPassword_InactiveAccount() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setUsername("jsmith");
        request.setCorreo("jsmith@example.com");

        org.mockito.Mockito.doThrow(new Exception("Account is inactive, please contact support."))
                .when(authService).resetPassword(any(String.class), any(String.class));

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is inactive, please contact support."));
    }
    
    @Test
    public void testScenario4_ResetPassword_EmptyFields() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setUsername("");
        request.setCorreo("jsmith@example.com");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username and correo are required"));
    }
}
