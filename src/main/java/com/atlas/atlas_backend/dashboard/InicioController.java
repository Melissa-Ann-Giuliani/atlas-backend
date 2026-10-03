package com.atlas.atlas_backend.dashboard;

import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/inicio")
public class InicioController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminUnidadRepository adminUnidadRepository;

    @PreAuthorize("hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_GLOBAL') or hasRole('AUDITOR') or hasRole('ADMIN_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD') or hasRole('ADMIN_DE_UNIDAD')")
    @GetMapping("/mapa-docente")
    public ResponseEntity<TeachingMapResponseDTO> getTeachingMap(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer caracterId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer categoriaId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer dedicacionId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer origenId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String estadoActual) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = auth.getName();
        Optional<Usuario> userOpt = usuarioRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        Usuario user = userOpt.get();
        Integer unidadId = null;

        if (user.getRol().getNombre().equalsIgnoreCase("Admin de unidad") || 
            user.getRol().getNombre().equalsIgnoreCase("Administrador de Unidad")) {
            Integer userId = user.getId();
            if (userId != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(userId);
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    unidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                }
            }
        }

        TeachingMapResponseDTO map = dashboardService.getTeachingMapData(unidadId, caracterId, categoriaId, dedicacionId, origenId, estadoActual);
        return ResponseEntity.ok(map);
    }
}
