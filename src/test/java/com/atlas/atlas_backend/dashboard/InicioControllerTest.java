package com.atlas.atlas_backend.dashboard;

import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Rol;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import com.atlas.atlas_backend.unidad.Unidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class InicioControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminUnidadRepository adminUnidadRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private InicioController inicioController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetTeachingMap_NotAuthenticated_Returns401() {
        when(authentication.isAuthenticated()).thenReturn(false);

        ResponseEntity<TeachingMapResponseDTO> response = inicioController.getTeachingMap(null, null, null, null, null);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void testGetTeachingMap_AdminGlobal_ReturnsUnfilteredData() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");

        Usuario mockUser = new Usuario();
        mockUser.setId(1);
        Rol rol = new Rol();
        rol.setNombre("Admin Global");
        mockUser.setRol(rol);

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(mockUser));
        when(dashboardService.getTeachingMapData(null, null, null, null, null, null))
                .thenReturn(new TeachingMapResponseDTO());

        ResponseEntity<TeachingMapResponseDTO> response = inicioController.getTeachingMap(null, null, null, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        verify(dashboardService, times(1)).getTeachingMapData(null, null, null, null, null, null);
    }

    @Test
    void testGetTeachingMap_AdminUnidad_ReturnsFilteredData() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin_unidad");

        Usuario mockUser = new Usuario();
        mockUser.setId(2);
        Rol rol = new Rol();
        rol.setNombre("Admin de unidad");
        mockUser.setRol(rol);

        AdminUnidad adminUnidad = new AdminUnidad();
        Unidad unidad = new Unidad();
        unidad.setUnidadId(10);
        adminUnidad.setUnidad(unidad);

        when(usuarioRepository.findByUsername("admin_unidad")).thenReturn(Optional.of(mockUser));
        when(adminUnidadRepository.findById(2)).thenReturn(Optional.of(adminUnidad));
        when(dashboardService.getTeachingMapData(10, null, null, null, null, null))
                .thenReturn(new TeachingMapResponseDTO());

        ResponseEntity<TeachingMapResponseDTO> response = inicioController.getTeachingMap(null, null, null, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        verify(dashboardService, times(1)).getTeachingMapData(10, null, null, null, null, null);
    }
}
