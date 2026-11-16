package com.atlas.atlas_backend.designacion;

import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.cargo.CargoRepository;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.unidad.UnidadRepository;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.DocenteRepository;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class DesignacionService {

    public static final int UMBRAL_VENCIMIENTO_DIAS = 30;
    public static final int UMBRAL_CRITICO_DIAS = 7;

    @Autowired
    private DesignacionRepository designacionRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminUnidadRepository adminUnidadRepository;

    @Autowired(required = false)
    private UnidadRepository unidadRepository;

    @Transactional
    public DesignacionResponseDTO registrarDesignacion(DesignacionCreateRequest request, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Docente docente = docenteRepository.findById(Objects.requireNonNull(request.getDocenteId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));

        if (designacionRepository.existsByUsuarioIdAndDesignacionNumeroResolucion(
                request.getDocenteId(), request.getNroResolucion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El docente ya posee una designación registrada con el número de resolución indicado");
        }

        if (request.getFechaFin() != null && request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de finalización no puede ser anterior a la fecha de inicio");
        }

        Cargo cargo = null;
        if (request.getCargoId() != null) {
            cargo = cargoRepository.findById(Objects.requireNonNull(request.getCargoId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado"));

            String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
            if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                    rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                    rolNombre.equalsIgnoreCase("ADMIN_UNIDAD") ||
                    rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD") ||
                    rolNombre.equalsIgnoreCase("ADMINISTRADOR_DE_UNIDAD")) {
                if (usuario.getId() != null) {
                    Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
                    if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                        Integer userUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                        if (cargo.getUnidad() == null
                                || !Objects.equals(userUnidadId, cargo.getUnidad().getUnidadId())) {
                            throw new AccessDeniedException(
                                    "No tiene permisos para registrar designaciones en cargos de otra unidad");
                        }
                    } else {
                        throw new AccessDeniedException("La Unidad del Administrador no fue encontrada");
                    }
                } else {
                    throw new AccessDeniedException("ID de usuario nulo");
                }
            }

            if (cargo.getEstado() == null || !cargo.getEstado().equalsIgnoreCase("Libre")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El cargo ya se encuentra asignado o en licencia");
            }
        }

        Designacion designacion = new Designacion();
        designacion.setUsuarioId(docente.getId() != null ? docente.getId() : request.getDocenteId());
        designacion.setDesignacionNumeroResolucion(request.getNroResolucion());
        designacion.setDesignacionFechaInicio(request.getFechaInicio());
        designacion.setDesignacionFechaFin(request.getFechaFin());
        designacion.setDesignacionEstadoActual("Activo");

        Designacion savedDesignacion = designacionRepository.save(designacion);

        if (cargo != null) {
            cargo.setDesignacion(savedDesignacion);
            cargo.setEstado("Asignado");
            cargoRepository.save(cargo);

            docente.setEstado("Activo");
            docente.setActivo(true);
            docenteRepository.save(docente);
        }

        // El envío de notificación por correo electrónico queda diferido a RF-11
        // (Decisión R1)

        return buildResponseDTO(savedDesignacion, docente, cargo);
    }

    @Transactional
    public DesignacionResponseDTO vincularCargo(Integer designacionId, Integer cargoId, String username) {
        if (designacionId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Designación no encontrada");
        }
        if (cargoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del cargo es obligatorio");
        }

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Designacion designacion = designacionRepository.findById(Objects.requireNonNull(designacionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designación no encontrada"));

        List<Cargo> cargosExistentes = cargoRepository.findByDesignacionDesignacionIdIn(List.of(designacionId));
        if (cargosExistentes != null && !cargosExistentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La designación ya posee un cargo vinculado");
        }

        Cargo cargo = cargoRepository.findById(Objects.requireNonNull(cargoId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado"));

        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMINISTRADOR_DE_UNIDAD")) {
            if (usuario.getId() != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    Integer userUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                    if (cargo.getUnidad() == null || !Objects.equals(userUnidadId, cargo.getUnidad().getUnidadId())) {
                        throw new AccessDeniedException(
                                "No tiene permisos para vincular cargos de otra unidad");
                    }
                } else {
                    throw new AccessDeniedException("La Unidad del Administrador no fue encontrada");
                }
            } else {
                throw new AccessDeniedException("ID de usuario nulo");
            }
        }

        if (cargo.getEstado() == null || !cargo.getEstado().equalsIgnoreCase("Libre")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cargo ya se encuentra asignado o en licencia");
        }

        cargo.setDesignacion(designacion);
        cargo.setEstado("Asignado");
        cargoRepository.save(cargo);

        Docente docente = null;
        if (designacion.getUsuarioId() != null) {
            docente = docenteRepository.findById(Objects.requireNonNull(designacion.getUsuarioId())).orElse(null);
        }

        return buildResponseDTO(designacion, docente, cargo);
    }

    @Transactional(readOnly = true)
    public DesignacionResponseDTO obtenerPorId(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID de designación nulo");
        }

        Designacion designacion = designacionRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designación no encontrada"));

        Docente docente = null;
        Integer docenteUsuarioId = designacion.getUsuarioId();
        if (docenteUsuarioId != null) {
            docente = docenteRepository.findById(Objects.requireNonNull(docenteUsuarioId)).orElse(null);
        }

        Cargo cargo = null;
        Integer designacionId = designacion.getDesignacionId();
        if (designacionId != null) {
            List<Cargo> cargos = cargoRepository.findByDesignacionDesignacionIdIn(List.of(designacionId));
            if (cargos != null && !cargos.isEmpty()) {
                cargo = cargos.get(0);
            }
        }

        return buildResponseDTO(designacion, docente, cargo);
    }

    private DesignacionResponseDTO buildResponseDTO(Designacion designacion, Docente docente, Cargo cargo) {
        DesignacionResponseDTO dto = new DesignacionResponseDTO();
        dto.setId(designacion.getDesignacionId());
        dto.setNroResolucion(designacion.getDesignacionNumeroResolucion());
        dto.setFechaInicio(designacion.getDesignacionFechaInicio());
        dto.setFechaFin(designacion.getDesignacionFechaFin());
        dto.setEstado(designacion.getDesignacionEstadoActual());

        if (docente != null) {
            DesignacionResponseDTO.DocenteResumenDTO docenteDto = new DesignacionResponseDTO.DocenteResumenDTO();
            docenteDto.setId(docente.getId());
            docenteDto.setLegajo(docente.getLegajo());
            docenteDto.setNombre(docente.getNombre());
            docenteDto.setApellido(docente.getApellido());
            docenteDto.setDni(docente.getDni());
            docenteDto.setEmail(docente.getEmailInstitucional() != null
                    ? docente.getEmailInstitucional()
                    : docente.getCorreo());
            dto.setDocente(docenteDto);
        }

        if (cargo != null) {
            DesignacionResponseDTO.CargoResumenDTO cargoDto = new DesignacionResponseDTO.CargoResumenDTO();
            cargoDto.setId(cargo.getId());
            cargoDto.setCodigo(cargo.getCodigo());
            cargoDto.setCategoria(cargo.getCategoria() != null ? cargo.getCategoria().getCategoriaNombre() : null);
            cargoDto.setDedicacion(cargo.getDedicacion() != null ? cargo.getDedicacion().getDedicacionNombre() : null);
            cargoDto.setUnidad(cargo.getUnidad() != null ? cargo.getUnidad().getUnidadNombre() : null);
            dto.setCargo(cargoDto);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<DesignacionVencimientoDTO> obtenerVencimientos(
            Integer requestedUnidadId,
            Boolean incluirVencidas,
            String username) {
        return obtenerVencimientos(requestedUnidadId, incluirVencidas, username, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<DesignacionVencimientoDTO> obtenerVencimientos(
            Integer requestedUnidadId,
            Boolean incluirVencidas,
            String username,
            LocalDate fechaReferencia) {
        UnidadScope scope = resolverUnidadScope(username, requestedUnidadId);
        Integer unidadEfectivaId = scope.unidadId;

        LocalDate hoy = fechaReferencia != null ? fechaReferencia : LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(UMBRAL_VENCIMIENTO_DIAS);
        boolean incluirVencidasFlag = Boolean.TRUE.equals(incluirVencidas);

        List<Designacion> designaciones = incluirVencidasFlag
                ? designacionRepository.findVencimientosIncluyendoVencidas(unidadEfectivaId, fechaLimite)
                : designacionRepository.findProximosVencimientos(unidadEfectivaId, hoy, fechaLimite);

        if (designaciones == null || designaciones.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> designacionIds = designaciones.stream()
                .filter(d -> d != null && d.getDesignacionId() != null)
                .map(d -> d.getDesignacionId())
                .distinct()
                .toList();

        List<Cargo> allCargos = designacionIds.isEmpty()
                ? Collections.emptyList()
                : cargoRepository.findByDesignacionDesignacionIdIn(designacionIds);

        Map<Integer, List<Cargo>> cargosPorDesignacion = new HashMap<>();
        if (allCargos != null) {
            for (Cargo c : allCargos) {
                if (c != null && c.getDesignacion() != null && c.getDesignacion().getDesignacionId() != null) {
                    cargosPorDesignacion
                            .computeIfAbsent(c.getDesignacion().getDesignacionId(), k -> new ArrayList<>())
                            .add(c);
                }
            }
        }

        List<Integer> docenteIds = designaciones.stream()
                .filter(d -> d != null && d.getUsuarioId() != null)
                .map(d -> d.getUsuarioId())
                .distinct()
                .toList();

        Map<Integer, Docente> docentesPorId = new HashMap<>();
        if (!docenteIds.isEmpty()) {
            Iterable<Docente> docentes = docenteRepository.findAllById(Objects.requireNonNull(docenteIds));
            if (docentes != null) {
                for (Docente doc : docentes) {
                    if (doc != null && doc.getId() != null) {
                        docentesPorId.put(doc.getId(), doc);
                    }
                }
            }
            for (Integer docId : docenteIds) {
                if (docId != null && !docentesPorId.containsKey(docId)) {
                    docenteRepository.findById(docId).ifPresent(doc -> docentesPorId.put(docId, doc));
                }
            }
        }

        List<DesignacionVencimientoDTO> resultado = new ArrayList<>();
        for (Designacion d : designaciones) {
            if (d == null || d.getDesignacionFechaFin() == null) {
                continue;
            }
            String estado = d.getDesignacionEstadoActual() != null ? d.getDesignacionEstadoActual().trim() : "";
            if (!estado.equalsIgnoreCase("Activa") && !estado.equalsIgnoreCase("Activo")) {
                continue;
            }

            long diasRestantes = ChronoUnit.DAYS.between(hoy, d.getDesignacionFechaFin());
            if (!incluirVencidasFlag && diasRestantes < 0) {
                continue;
            }
            if (diasRestantes > UMBRAL_VENCIMIENTO_DIAS) {
                continue;
            }

            List<Cargo> cargosDesignacion = cargosPorDesignacion.getOrDefault(
                    d.getDesignacionId(), Collections.emptyList());

            if (unidadEfectivaId != null && !cargosDesignacion.isEmpty()) {
                boolean perteneceAUnidad = cargosDesignacion.stream()
                        .anyMatch(c -> c.getUnidad() != null
                                && Objects.equals(c.getUnidad().getUnidadId(), unidadEfectivaId));
                if (!perteneceAUnidad) {
                    continue;
                }
            }

            DesignacionVencimientoDTO.DocenteResumenDTO docenteDto = null;
            Docente doc = d.getUsuarioId() != null ? docentesPorId.get(d.getUsuarioId()) : null;
            if (doc != null) {
                docenteDto = DesignacionVencimientoDTO.DocenteResumenDTO.builder()
                        .docenteId(doc.getId())
                        .nombre(doc.getNombre())
                        .apellido(doc.getApellido())
                        .documento(doc.getDni() != null ? String.valueOf(doc.getDni()) : null)
                        .legajo(doc.getLegajo() != null ? String.valueOf(doc.getLegajo()) : null)
                        .build();
            }

            DesignacionVencimientoDTO.UnidadResumenDTO unidadDto = null;
            List<DesignacionVencimientoDTO.CargoResumenDTO> cargosDto = new ArrayList<>();
            for (Cargo c : cargosDesignacion) {
                if (unidadDto == null && c.getUnidad() != null) {
                    unidadDto = DesignacionVencimientoDTO.UnidadResumenDTO.builder()
                            .unidadId(c.getUnidad().getUnidadId())
                            .nombre(c.getUnidad().getUnidadNombre())
                            .build();
                }
                cargosDto.add(DesignacionVencimientoDTO.CargoResumenDTO.builder()
                        .cargoId(c.getId())
                        .codigo(c.getCodigo())
                        .categoria(c.getCategoria() != null ? c.getCategoria().getCategoriaNombre() : null)
                        .dedicacion(c.getDedicacion() != null ? c.getDedicacion().getDedicacionNombre() : null)
                        .caracter(c.getCaracter() != null ? c.getCaracter().getCaracterNombre() : null)
                        .build());
            }

            if (unidadDto == null && scope.unidadId != null) {
                unidadDto = DesignacionVencimientoDTO.UnidadResumenDTO.builder()
                        .unidadId(scope.unidadId)
                        .nombre(scope.unidadNombre)
                        .build();
            }

            DesignacionVencimientoDTO item = DesignacionVencimientoDTO.builder()
                    .designacionId(d.getDesignacionId())
                    .numeroResolucion(d.getDesignacionNumeroResolucion())
                    .fechaInicio(d.getDesignacionFechaInicio())
                    .fechaFin(d.getDesignacionFechaFin())
                    .diasRestantes(diasRestantes)
                    .estadoActual(d.getDesignacionEstadoActual())
                    .docente(docenteDto)
                    .unidad(unidadDto)
                    .cargos(cargosDto)
                    .build();
            resultado.add(item);
        }

        resultado.sort((a, b) -> {
            long diasA = a != null && a.getDiasRestantes() != null ? a.getDiasRestantes() : 0L;
            long diasB = b != null && b.getDiasRestantes() != null ? b.getDiasRestantes() : 0L;
            int cmp = Long.compare(diasA, diasB);
            if (cmp != 0) {
                return cmp;
            }
            int idA = a != null && a.getDesignacionId() != null ? a.getDesignacionId() : 0;
            int idB = b != null && b.getDesignacionId() != null ? b.getDesignacionId() : 0;
            return Integer.compare(idA, idB);
        });
        return resultado;
    }

    @Transactional(readOnly = true)
    public DesignacionVencimientosResumenDTO obtenerResumenVencimientos(
            Integer requestedUnidadId,
            String username) {
        return obtenerResumenVencimientos(requestedUnidadId, username, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public DesignacionVencimientosResumenDTO obtenerResumenVencimientos(
            Integer requestedUnidadId,
            String username,
            LocalDate fechaReferencia) {
        UnidadScope scope = resolverUnidadScope(username, requestedUnidadId);

        LocalDate hoy = fechaReferencia != null ? fechaReferencia : LocalDate.now();
        LocalDate fechaLimite30 = hoy.plusDays(UMBRAL_VENCIMIENTO_DIAS);
        LocalDate fechaLimiteCritico = hoy.plusDays(UMBRAL_CRITICO_DIAS);

        long totalPorVencer = designacionRepository.countVencimientos(scope.unidadId, hoy, fechaLimite30);
        long totalCriticos = designacionRepository.countVencimientos(scope.unidadId, hoy, fechaLimiteCritico);

        return DesignacionVencimientosResumenDTO.builder()
                .totalPorVencer(totalPorVencer)
                .umbralDias(UMBRAL_VENCIMIENTO_DIAS)
                .unidadId(scope.unidadId)
                .unidadNombre(scope.unidadNombre)
                .tieneVencimientosCriticos(totalCriticos > 0)
                .totalCriticos(totalCriticos)
                .build();
    }

    private UnidadScope resolverUnidadScope(String username, Integer requestedUnidadId) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        boolean esAdminUnidad = rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMINISTRADOR_DE_UNIDAD");

        if (esAdminUnidad) {
            if (usuario.getId() == null) {
                throw new AccessDeniedException("ID de usuario nulo");
            }
            Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
            if (adminUnidadOpt.isEmpty() || adminUnidadOpt.get().getUnidad() == null) {
                throw new AccessDeniedException("La Unidad del Administrador no fue encontrada");
            }
            Unidad userUnidad = adminUnidadOpt.get().getUnidad();
            Integer userUnidadId = userUnidad.getUnidadId();
            if (requestedUnidadId != null && !Objects.equals(requestedUnidadId, userUnidadId)) {
                throw new AccessDeniedException("No tiene permisos para consultar designaciones de otra unidad");
            }
            return new UnidadScope(userUnidadId, userUnidad.getUnidadNombre());
        }

        if (requestedUnidadId != null) {
            String nombreUnidad = null;
            if (unidadRepository != null) {
                nombreUnidad = unidadRepository.findById(Objects.requireNonNull(requestedUnidadId))
                        .map(u -> u.getUnidadNombre())
                        .orElse(null);
            }
            return new UnidadScope(requestedUnidadId, nombreUnidad);
        }

        return new UnidadScope(null, null);
    }

    private static class UnidadScope {
        private final Integer unidadId;
        private final String unidadNombre;

        private UnidadScope(Integer unidadId, String unidadNombre) {
            this.unidadId = unidadId;
            this.unidadNombre = unidadNombre;
        }
    }
}
