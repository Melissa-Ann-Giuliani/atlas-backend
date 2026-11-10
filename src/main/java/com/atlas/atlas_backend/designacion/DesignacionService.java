package com.atlas.atlas_backend.designacion;

import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.cargo.CargoRepository;
import com.atlas.atlas_backend.notification.EmailService;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DesignacionService {

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

    @Autowired
    private EmailService emailService;

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

        Cargo cargo = cargoRepository.findById(Objects.requireNonNull(request.getCargoId()))
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

        Designacion designacion = new Designacion();
        designacion.setUsuarioId(docente.getId() != null ? docente.getId() : request.getDocenteId());
        designacion.setDesignacionNumeroResolucion(request.getNroResolucion());
        designacion.setDesignacionFechaInicio(request.getFechaInicio());
        designacion.setDesignacionFechaFin(request.getFechaFin());
        designacion.setDesignacionEstadoActual("Activo");

        Designacion savedDesignacion = designacionRepository.save(designacion);

        cargo.setDesignacion(savedDesignacion);
        cargo.setEstado("Asignado");
        cargoRepository.save(cargo);

        docente.setEstado("Activo");
        docente.setActivo(true);
        docenteRepository.save(docente);

        String emailDestino = docente.getEmailInstitucional() != null
                ? docente.getEmailInstitucional()
                : docente.getCorreo();
        try {
            emailService.sendDesignacionNotification(
                    emailDestino,
                    savedDesignacion.getDesignacionNumeroResolucion(),
                    savedDesignacion.getDesignacionFechaInicio(),
                    savedDesignacion.getDesignacionFechaFin());
        } catch (Exception e) {
            System.err.println("Advertencia: falló el envío de correo de notificación de designación: " + e.getMessage());
        }

        return buildResponseDTO(savedDesignacion, docente, cargo);
    }

    @Transactional(readOnly = true)
    public DesignacionResponseDTO obtenerPorId(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID de designación nulo");
        }

        Designacion designacion = designacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designación no encontrada"));

        Docente docente = null;
        Integer docenteUsuarioId = designacion.getUsuarioId();
        if (docenteUsuarioId != null) {
            docente = docenteRepository.findById(docenteUsuarioId).orElse(null);
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
}

