package com.atlas.atlas_backend.funcion;

import com.atlas.atlas_backend.archivo.Archivo;
import com.atlas.atlas_backend.archivo.ArchivoService;
import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.cargo.CargoRepository;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.designacion.DesignacionPdfService;
import com.atlas.atlas_backend.materia.Materia;
import com.atlas.atlas_backend.materia.MateriaRepository;
import com.atlas.atlas_backend.notification.EmailService;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.DocenteRepository;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class FuncionService {

    private static final String TIPO_TRAMITE_DESIGNACION = "DESIGNACION";

    private final FuncionRepository funcionRepository;
    private final CargoRepository cargoRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminUnidadRepository adminUnidadRepository;
    private final DocenteRepository docenteRepository;
    private final ArchivoService archivoService;
    private final DesignacionPdfService designacionPdfService;
    private final EmailService emailService;

    public FuncionService(
            FuncionRepository funcionRepository,
            CargoRepository cargoRepository,
            MateriaRepository materiaRepository,
            UsuarioRepository usuarioRepository,
            AdminUnidadRepository adminUnidadRepository,
            DocenteRepository docenteRepository,
            ArchivoService archivoService,
            DesignacionPdfService designacionPdfService,
            EmailService emailService) {
        this.funcionRepository = funcionRepository;
        this.cargoRepository = cargoRepository;
        this.materiaRepository = materiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminUnidadRepository = adminUnidadRepository;
        this.docenteRepository = docenteRepository;
        this.archivoService = archivoService;
        this.designacionPdfService = designacionPdfService;
        this.emailService = emailService;
    }

    /**
     * Assigns functions to a designated cargo, updates occupied hours, generates the official PDF,
     * activates the docente, and sends a confirmation email.
     */
    @Transactional
    public DesignacionCompletaResponseDTO asignarFuncionesACargo(
            Integer cargoId,
            AsignarFuncionesCargoRequest request,
            String username) {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (cargoId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El cargo especificado no existe");
        }

        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El cargo especificado no existe"));

        Designacion designacion = cargo.getDesignacion();
        if (designacion == null || cargo.getEstado() == null || !"Asignado".equalsIgnoreCase(cargo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cargo debe estar vinculado a una designación");
        }

        validarAlcanceUnidad(usuario, cargo);

        int maxHoras = validarTopeHoras(request.getFunciones(), cargo);

        List<Funcion> funcionesAGuardar = construirFunciones(request.getFunciones(), cargo);
        List<Funcion> funcionesGuardadas = funcionRepository.saveAll(Objects.requireNonNull(funcionesAGuardar));

        cargo.setHorasOcupadas(maxHoras);
        cargoRepository.save(cargo);

        Docente docente = activarDocente(designacion);

        Archivo documento = generarYGuardarPdf(designacion, cargo, docente, funcionesGuardadas);

        boolean notificacionEnviada = enviarNotificacion(docente, designacion, cargo, funcionesGuardadas);

        return construirResponseDTO(cargo, designacion, funcionesGuardadas, documento, docente, notificacionEnviada);
    }

    private void validarAlcanceUnidad(Usuario usuario, Cargo cargo) {
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!esAdminUnidad(rolNombre)) {
            return;
        }

        if (usuario.getId() == null) {
            throw new AccessDeniedException("ID de usuario nulo");
        }

        Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(Objects.requireNonNull(usuario.getId()));
        if (adminUnidadOpt.isEmpty() || adminUnidadOpt.get().getUnidad() == null) {
            throw new AccessDeniedException("La Unidad del Administrador no fue encontrada");
        }

        Integer userUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
        Integer cargoUnidadId = cargo.getUnidad() != null ? cargo.getUnidad().getUnidadId() : null;
        if (!Objects.equals(userUnidadId, cargoUnidadId)) {
            throw new AccessDeniedException("No tiene permisos para modificar cargos de otra unidad");
        }
    }

    private boolean esAdminUnidad(String rolNombre) {
        return rolNombre.equalsIgnoreCase("Admin de unidad")
                || rolNombre.equalsIgnoreCase("Administrador de Unidad")
                || rolNombre.equalsIgnoreCase("ADMIN_UNIDAD")
                || rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD")
                || rolNombre.equalsIgnoreCase("ADMINISTRADOR_DE_UNIDAD");
    }

    private int validarTopeHoras(List<FuncionItemRequest> items, Cargo cargo) {
        int totalHoras = 0;
        for (FuncionItemRequest item : items) {
            if (item != null && item.getHoras() != null) {
                totalHoras += item.getHoras();
            }
        }

        Integer dedicacionHoras = cargo.getDedicacion() != null ? cargo.getDedicacion().getDedicacionHoras() : null;
        if (dedicacionHoras == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cargo no posee una dedicación con horas configuradas");
        }

        if (totalHoras > dedicacionHoras) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La suma de horas (" + totalHoras + ") supera la dedicación máxima del cargo ("
                            + dedicacionHoras + ")");
        }
        return totalHoras;
    }

    private List<Funcion> construirFunciones(List<FuncionItemRequest> items, Cargo cargo) {
        Integer cargoUnidadId = cargo.getUnidad() != null ? cargo.getUnidad().getUnidadId() : null;
        List<Funcion> funciones = new ArrayList<>();

        for (FuncionItemRequest item : items) {
            Integer materiaId = Objects.requireNonNull(item.getMateriaId());
            Materia materia = materiaRepository.findById(materiaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "La materia especificada no existe"));

            Integer materiaUnidadId = materia.getUnidad() != null ? materia.getUnidad().getUnidadId() : null;
            if (cargoUnidadId == null || !Objects.equals(cargoUnidadId, materiaUnidadId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La materia no pertenece a la misma unidad académica del cargo");
            }

            Funcion funcion = new Funcion();
            funcion.setNombre(item.getNombre());
            funcion.setTipo(item.getTipo());
            funcion.setHoras(item.getHoras());
            funcion.setCargo(cargo);
            funcion.setMateria(materia);
            funciones.add(funcion);
        }
        return funciones;
    }

    private Docente activarDocente(Designacion designacion) {
        if (designacion.getUsuarioId() == null) {
            return null;
        }
        Docente docente = docenteRepository.findById(Objects.requireNonNull(designacion.getUsuarioId())).orElse(null);
        if (docente != null) {
            docente.setEstado("Activo");
            docente.setActivo(true);
            return docenteRepository.save(docente);
        }
        return null;
    }

    private Archivo generarYGuardarPdf(Designacion designacion, Cargo cargo, Docente docente,
            List<Funcion> funciones) {
        byte[] pdfBytes = designacionPdfService.generarPdfDesignacion(designacion, cargo, docente, funciones);
        String filename = "designacion-" + designacion.getDesignacionId() + ".pdf";
        try {
            return archivoService.guardarBytes(
                    pdfBytes,
                    filename,
                    "application/pdf",
                    TIPO_TRAMITE_DESIGNACION,
                    designacion.getDesignacionId());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo almacenar el documento PDF de la designación", e);
        }
    }

    private boolean enviarNotificacion(Docente docente, Designacion designacion, Cargo cargo,
            List<Funcion> funciones) {
        String email = resolveDocenteEmail(docente);
        if (email == null) {
            return false;
        }
        emailService.sendDesignacionCompletaNotification(
                email,
                designacion.getDesignacionNumeroResolucion(),
                designacion.getDesignacionFechaInicio(),
                designacion.getDesignacionFechaFin(),
                cargo,
                funciones);
        return true;
    }

    private String resolveDocenteEmail(Docente docente) {
        if (docente == null) {
            return null;
        }
        if (docente.getEmailInstitucional() != null && !docente.getEmailInstitucional().isBlank()) {
            return docente.getEmailInstitucional();
        }
        if (docente.getCorreo() != null && !docente.getCorreo().isBlank()) {
            return docente.getCorreo();
        }
        return null;
    }

    private DesignacionCompletaResponseDTO construirResponseDTO(
            Cargo cargo,
            Designacion designacion,
            List<Funcion> funciones,
            Archivo documento,
            Docente docente,
            boolean notificacionEnviada) {

        DesignacionCompletaResponseDTO dto = new DesignacionCompletaResponseDTO();
        dto.setCargoId(cargo.getId());
        dto.setHorasDedicacion(cargo.getDedicacion() != null ? cargo.getDedicacion().getDedicacionHoras() : null);
        dto.setHorasOcupadas(cargo.getHorasOcupadas());
        dto.setDesignacionId(designacion.getDesignacionId());
        dto.setDocumentoId(documento != null ? documento.getArchivoId() : null);
        dto.setDocenteEstado(docente != null ? docente.getEstado() : "Activo");
        dto.setNotificacionEnviada(notificacionEnviada);

        List<DesignacionCompletaResponseDTO.FuncionDTO> funcionDtos = new ArrayList<>();
        for (Funcion funcion : funciones) {
            DesignacionCompletaResponseDTO.FuncionDTO fDto = new DesignacionCompletaResponseDTO.FuncionDTO();
            fDto.setId(funcion.getId());
            fDto.setNombre(funcion.getNombre());
            fDto.setTipo(funcion.getTipo());
            fDto.setHoras(funcion.getHoras());

            if (funcion.getMateria() != null) {
                DesignacionCompletaResponseDTO.MateriaResumenDTO mDto =
                        new DesignacionCompletaResponseDTO.MateriaResumenDTO();
                mDto.setId(funcion.getMateria().getId());
                mDto.setNombre(funcion.getMateria().getNombre());
                fDto.setMateria(mDto);
            }
            funcionDtos.add(fDto);
        }
        dto.setFunciones(funcionDtos);

        return dto;
    }
}
