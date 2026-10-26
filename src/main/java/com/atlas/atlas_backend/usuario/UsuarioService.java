package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.cambio.CambioService;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.designacion.DesignacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private AdminGlobalRepository adminGlobalRepository;

    @Autowired
    private AdminUnidadRepository adminUnidadRepository;

    @Autowired
    private AuditorRepository auditorRepository;

    @Autowired
    private DesignacionRepository designacionRepository;

    @Autowired
    private CambioService cambioService;

    private static final Set<String> INACTIVE_DESIGNACION_ESTADOS = Set.of("finalizada", "cancelada", "baja");

    @Transactional
    public Map<String, Object> darDeBajaUsuario(@NonNull Integer id, String authUsername) {
        if (authUsername == null || authUsername.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        Usuario targetUser = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Usuario usuarioResponsable = usuarioRepository.findByUsername(authUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));

        // R1: Self-decommission check
        if (targetUser.getUsername() != null && targetUser.getUsername().equalsIgnoreCase(authUsername.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puede darse de baja a sí mismo");
        }

        // Check if already inactive
        if (Boolean.FALSE.equals(targetUser.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario ya se encuentra inactivo");
        }

        // B1: Cargos vigentes check for Docente
        Optional<Docente> docenteOpt = docenteRepository.findById(id);
        if (docenteOpt.isPresent()) {
            List<Designacion> designaciones = designacionRepository.findByUsuarioId(id);
            LocalDate today = LocalDate.now();

            boolean tieneCargosVigentes = designaciones.stream().anyMatch(d -> {
                String estado = d.getDesignacionEstadoActual() != null ? d.getDesignacionEstadoActual().trim().toLowerCase() : "";
                boolean estadoActivo = !INACTIVE_DESIGNACION_ESTADOS.contains(estado);
                boolean fechaVigente = d.getDesignacionFechaFin() == null || !d.getDesignacionFechaFin().isBefore(today);
                return estadoActivo && fechaVigente;
            });

            if (tieneCargosVigentes) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede dar de baja el usuario porque posee cargos vigentes");
            }
        }

        // Perform deactivation and audit on usuarios table
        cambioService.registrarCambio("usuarios", "usuario_activo", "true", "false", usuarioResponsable);
        targetUser.setActivo(false);
        usuarioRepository.save(targetUser);

        // Sub-entity synchronization for Docente
        if (docenteOpt.isPresent()) {
            Docente docente = docenteOpt.get();
            String prevEstado = docente.getEstado();
            cambioService.registrarCambio("docentes", "docente_estado", prevEstado, "Inactivo", usuarioResponsable);
            docente.setEstado("Inactivo");
            docenteRepository.save(docente);
        }

        // Build response map
        Map<String, Object> usuarioMap = buildUsuarioSummaryMap(targetUser, docenteOpt.orElse(null));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Usuario dado de baja exitosamente");
        response.put("usuario", usuarioMap);

        return response;
    }

    private Map<String, Object> buildUsuarioSummaryMap(Usuario u, Docente doc) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("usuarioId", u.getId());
        map.put("username", u.getUsername());
        map.put("nombre", u.getNombre());
        map.put("apellido", u.getApellido());
        map.put("correo", u.getCorreo());
        map.put("activo", u.getActivo());
        if (u.getRol() != null) {
            map.put("rol", u.getRol().getNombre());
        }

        if (doc != null) {
            map.put("estado", doc.getEstado());
            if (doc.getDni() != null) {
                map.put("dni", String.valueOf(doc.getDni()));
            }
            if (doc.getCuil() != null) {
                map.put("cuil", String.valueOf(doc.getCuil()));
            }
        } else if (u.getId() != null) {
            Integer userId = Objects.requireNonNull(u.getId());
            adminGlobalRepository.findById(userId).ifPresent(ag -> {
                if (ag.getCargoNombre() != null) {
                    map.put("cargo", ag.getCargoNombre().name());
                    map.put("cargoNombre", ag.getCargoNombre().name());
                }
            });

            adminUnidadRepository.findById(userId).ifPresent(au -> {
                if (au.getCargoNombre() != null) {
                    map.put("cargo", au.getCargoNombre().name());
                    map.put("cargoNombre", au.getCargoNombre().name());
                }
                if (au.getUnidad() != null) {
                    map.put("unidadId", au.getUnidad().getUnidadId());
                    map.put("unidad", au.getUnidad().getUnidadNombre());
                }
            });

            auditorRepository.findById(userId).ifPresent(aud -> {
                if (aud.getCargoNombre() != null) {
                    map.put("cargo", aud.getCargoNombre().name());
                    map.put("cargoNombre", aud.getCargoNombre().name());
                }
            });
        }

        return map;
    }
}
