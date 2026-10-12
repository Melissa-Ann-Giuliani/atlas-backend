package com.atlas.atlas_backend.licencia;

import com.atlas.atlas_backend.archivo.Archivo;
import com.atlas.atlas_backend.archivo.ArchivoRepository;
import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.cargo.CargoRepository;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.DocenteRepository;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class LicenciaService {

    @Autowired
    private LicenciaRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminUnidadRepository adminUnidadRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private ArchivoRepository archivoRepository;

    @Transactional(readOnly = true)
    public Page<LicenciaListDTO> getActiveLicencias(
            String username,
            String searchTerm,
            Integer motivoId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer unidadIdParam,
            Pageable pageable) throws Exception {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        Integer effectiveUnidadId = null;
        String rolNombre = usuario.getRol().getNombre();

        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD")) {
            if (usuario.getId() != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    effectiveUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                } else {
                    throw new Exception("La Unidad del Administrador no fue encontrada");
                }
            } else {
                throw new Exception("ID de usuario nulo");
            }
        } else {
            effectiveUnidadId = unidadIdParam;
        }

        String safeSearch = (searchTerm != null) ? searchTerm.trim() : null;
        return repository.findActiveLicencias(effectiveUnidadId, safeSearch, motivoId, fechaDesde, fechaHasta, pageable);
    }

    @Transactional(readOnly = true)
    public LicenciaDetailDTO getLicenciaDetails(Integer id, String username) throws Exception {
        if (id == null) {
            throw new IllegalArgumentException("ID de licencia nulo");
        }

        Licencia licencia = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Licencia no encontrada con ID: " + id));

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        Integer designacionId = (licencia.getDesignacion() != null)
                ? licencia.getDesignacion().getDesignacionId()
                : null;

        List<Cargo> cargos = (designacionId != null)
                ? cargoRepository.findByDesignacionDesignacionIdIn(List.of(designacionId))
                : new ArrayList<>();

        String rolNombre = usuario.getRol().getNombre();
        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD")) {

            Integer adminUnidadId = null;
            if (usuario.getId() != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    adminUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                }
            }

            final Integer targetUnidadId = adminUnidadId;
            boolean perteneceAUnidad = cargos.stream().anyMatch(c ->
                    c.getUnidad() != null && c.getUnidad().getUnidadId().equals(targetUnidadId));

            if (!perteneceAUnidad) {
                throw new AccessDeniedException("No tiene permisos para ver esta licencia");
            }
        }

        LicenciaDetailDTO dto = new LicenciaDetailDTO();

        // Licencia Data
        dto.setId(licencia.getId());
        dto.setNroResolucion(licencia.getNroResolucion());
        dto.setFechaInicio(licencia.getFechaInicio());
        dto.setFechaFin(licencia.getFechaFin());
        dto.setFechaFinEstim(licencia.getFechaFinEstim());
        dto.setCantidadDias(licencia.getCantidadDias());
        dto.setEstado("Activa");

        // Motivo Data
        if (licencia.getMotivo() != null) {
            dto.setMotivoId(licencia.getMotivo().getId());
            dto.setMotivoNombre(licencia.getMotivo().getNombre());
            dto.setMotivoDiasCorresp(licencia.getMotivo().getDiasCorresp());
        }

        // Designación Data
        if (licencia.getDesignacion() != null) {
            dto.setDesignacionId(licencia.getDesignacion().getDesignacionId());
            dto.setDesignacionNroResolucion(licencia.getDesignacion().getDesignacionNumeroResolucion());
            dto.setDesignacionFechaInicio(licencia.getDesignacion().getDesignacionFechaInicio());
            dto.setDesignacionFechaFin(licencia.getDesignacion().getDesignacionFechaFin());
            dto.setDesignacionEstado(licencia.getDesignacion().getDesignacionEstadoActual());

            // Docente Data
            Integer docenteUsuarioId = licencia.getDesignacion().getUsuarioId();
            if (docenteUsuarioId != null) {
                Optional<Docente> docenteOpt = docenteRepository.findById(docenteUsuarioId);
                if (docenteOpt.isPresent()) {
                    Docente docente = docenteOpt.get();
                    dto.setDocenteNombreCompleto(docente.getApellido() + ", " + docente.getNombre());
                    dto.setDocenteDni(docente.getDni());
                    dto.setDocenteCuil(docente.getCuil());
                    dto.setDocenteEmail(docente.getEmailInstitucional() != null
                            ? docente.getEmailInstitucional()
                            : docente.getCorreo());
                    dto.setDocenteTelefonos(docente.getTelefonos());
                }
            }
        }

        // Cargos Data
        List<String> cargosVinculados = new ArrayList<>();
        List<LicenciaDetailDTO.CargoVinculadoData> cargosDetalle = new ArrayList<>();
        Integer firstUnidadId = null;
        String firstUnidadNombre = null;

        for (Cargo cargo : cargos) {
            LicenciaDetailDTO.CargoVinculadoData cd = new LicenciaDetailDTO.CargoVinculadoData();
            cd.setId(cargo.getId());
            cd.setCodigo(cargo.getCodigo());
            cd.setCategoria(cargo.getCategoria() != null ? cargo.getCategoria().getCategoriaNombre() : null);
            cd.setDedicacion(cargo.getDedicacion() != null ? cargo.getDedicacion().getDedicacionNombre() : null);
            cd.setCaracter(cargo.getCaracter() != null ? cargo.getCaracter().getCaracterNombre() : null);
            cd.setEstado(cargo.getEstado());

            if (cargo.getUnidad() != null) {
                cd.setUnidadNombre(cargo.getUnidad().getUnidadNombre());
                if (firstUnidadId == null) {
                    firstUnidadId = cargo.getUnidad().getUnidadId();
                    firstUnidadNombre = cargo.getUnidad().getUnidadNombre();
                }
            }
            cargosDetalle.add(cd);

            String resumen = "Cargo #" + cargo.getCodigo()
                    + (cd.getCategoria() != null ? " - " + cd.getCategoria() : "")
                    + (cd.getUnidadNombre() != null ? " (" + cd.getUnidadNombre() + ")" : "");
            cargosVinculados.add(resumen);
        }

        dto.setUnidadId(firstUnidadId);
        dto.setUnidadNombre(firstUnidadNombre);
        dto.setCargosVinculados(cargosVinculados);
        dto.setCargosDetalle(cargosDetalle);

        // Archivos Adjuntos
        List<Archivo> archivos = archivoRepository.findByTipoTramiteAndTramiteId("LICENCIA", licencia.getId());
        List<LicenciaDetailDTO.ArchivoAdjuntoData> archivosData = new ArrayList<>();
        for (Archivo a : archivos) {
            LicenciaDetailDTO.ArchivoAdjuntoData ad = new LicenciaDetailDTO.ArchivoAdjuntoData();
            ad.setId(a.getArchivoId());
            ad.setNombreOriginal(a.getNombreOriginal());
            ad.setNombreEncriptado(a.getNombreEncriptado());
            ad.setFechaSubida(a.getFechaSubida());
            ad.setTamano(a.getTamano());
            ad.setTipoContenido(a.getTipoContenido());
            archivosData.add(ad);
        }
        dto.setDocumentacionAdjunta(archivosData);

        return dto;
    }
}
