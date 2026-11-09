package com.atlas.atlas_backend.catedra;

import com.atlas.atlas_backend.catedra.dto.CatedraRequestDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraResponseDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraUpdateRequestDTO;
import com.atlas.atlas_backend.materia.Materia;
import com.atlas.atlas_backend.materia.MateriaRepository;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.DocenteRepository;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class CatedraService {

    private static final String ESTADO_DE_BAJA = "De baja";

    private final CatedraRepository catedraRepository;
    private final MateriaRepository materiaRepository;
    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminUnidadRepository adminUnidadRepository;

    public CatedraService(
            CatedraRepository catedraRepository,
            MateriaRepository materiaRepository,
            DocenteRepository docenteRepository,
            UsuarioRepository usuarioRepository,
            AdminUnidadRepository adminUnidadRepository) {
        this.catedraRepository = catedraRepository;
        this.materiaRepository = materiaRepository;
        this.docenteRepository = docenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminUnidadRepository = adminUnidadRepository;
    }

    private Integer resolverUnidadId(String username) {
        if (username == null) {
            return null;
        }
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMINISTRADOR_DE_UNIDAD")) {
            Integer usuarioId = usuario.getId();
            if (usuarioId != null) {
                return adminUnidadRepository.findById(usuarioId)
                        .filter(au -> au.getUnidad() != null)
                        .map(au -> au.getUnidad().getUnidadId())
                        .orElseThrow(() -> new AccessDeniedException("La unidad del administrador no fue encontrada"));
            } else {
                throw new AccessDeniedException("ID de usuario nulo");
            }
        }
        return null;
    }

    private CatedraResponseDTO mapToDTO(Catedra c) {
        Docente d = c.getDirector();
        String directorNombre = "";
        if (d != null) {
            directorNombre = ((d.getNombre() != null ? d.getNombre() : "") +
                    (d.getApellido() != null ? " " + d.getApellido() : "")).trim();
        }

        Materia m = c.getMateria();
        Integer materiaId = m != null ? m.getId() : null;
        String materiaNombre = m != null ? m.getNombre() : null;
        Integer unidadId = (m != null && m.getUnidad() != null) ? m.getUnidad().getUnidadId() : null;
        String unidadNombre = (m != null && m.getUnidad() != null) ? m.getUnidad().getUnidadNombre() : null;
        Integer tipoUnidadId = (m != null && m.getUnidad() != null && m.getUnidad().getTipoUnidad() != null)
                ? m.getUnidad().getTipoUnidad().getTipoUnidadId()
                : null;
        String tipoUnidadNombre = (m != null && m.getUnidad() != null && m.getUnidad().getTipoUnidad() != null)
                ? m.getUnidad().getTipoUnidad().getTipoUnidadNombre()
                : null;

        return CatedraResponseDTO.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .tipologia(c.getTipologia())
                .estado(c.getEstado())
                .directorId(d != null ? d.getId() : null)
                .directorNombre(directorNombre)
                .materiaId(materiaId)
                .materiaNombre(materiaNombre)
                .unidadId(unidadId)
                .unidadNombre(unidadNombre)
                .tipoUnidadId(tipoUnidadId)
                .tipoUnidadNombre(tipoUnidadNombre)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CatedraResponseDTO> listarCatedras(String search, String username) {
        Integer unidadId = resolverUnidadId(username);
        String term = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        List<Catedra> list = catedraRepository.searchCatedrasList(unidadId, term);
        return list.stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public Page<CatedraResponseDTO> listarCatedras(String search, Pageable pageable, String username) {
        Integer unidadId = resolverUnidadId(username);
        String term = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Catedra> page = catedraRepository.searchCatedras(unidadId, term, pageable);
        return page.map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public CatedraResponseDTO obtenerCatedraPorId(Integer id, String username) {
        Objects.requireNonNull(id, "Catedra ID must not be null");
        Catedra c = catedraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cátedra no encontrada"));

        Integer adminUnidadId = resolverUnidadId(username);
        if (adminUnidadId != null) {
            Integer materiaUnidadId = (c.getMateria() != null && c.getMateria().getUnidad() != null)
                    ? c.getMateria().getUnidad().getUnidadId()
                    : null;
            if (materiaUnidadId == null || !adminUnidadId.equals(materiaUnidadId)) {
                throw new AccessDeniedException("No tiene permisos para ver cátedras de otra unidad");
            }
        }

        return mapToDTO(c);
    }

    @Transactional
    public CatedraResponseDTO crearCatedra(CatedraRequestDTO request, String username) {
        Objects.requireNonNull(request, "Request must not be null");

        // 1. Validar existencia de la Materia
        Materia materia = materiaRepository.findById(Objects.requireNonNull(request.getMateriaId(), "Materia ID must not be null"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Materia no encontrada"));

        // 2. Validar permisos si el usuario es Administrador de Unidad
        Integer adminUnidadId = resolverUnidadId(username);
        if (adminUnidadId != null) {
            Integer materiaUnidadId = materia.getUnidad() != null ? materia.getUnidad().getUnidadId() : null;
            if (materiaUnidadId == null || !adminUnidadId.equals(materiaUnidadId)) {
                throw new AccessDeniedException("No tiene permisos para crear cátedras en materias de otra unidad");
            }
        }

        // 3. Validar existencia y estado del Docente
        Docente docente = docenteRepository.findById(Objects.requireNonNull(request.getDirectorId(), "Director ID must not be null"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));

        if (docente.getEstado() == null || !"Activo".equalsIgnoreCase(docente.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El docente debe estar en estado activo");
        }

        // 4. Validar unicidad de nombre de cátedra en la materia seleccionada
        String nombreCatedra = request.getNombre() != null ? request.getNombre().trim() : "";
        boolean existe = catedraRepository.existsByNombreIgnoreCaseAndMateria_Id(nombreCatedra, materia.getId());
        if (existe) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una cátedra con ese nombre para la materia seleccionada");
        }

        // 5. Construir y persistir la entidad Catedra
        Catedra catedra = new Catedra();
        catedra.setNombre(nombreCatedra);
        catedra.setTipologia(request.getTipologia());
        catedra.setEstado("Activa");
        catedra.setDirector(docente);
        catedra.setMateria(materia);

        Catedra saved = catedraRepository.save(catedra);
        return mapToDTO(saved);
    }

    @Transactional
    public CatedraResponseDTO modificarCatedra(Integer id, CatedraUpdateRequestDTO request, String username) {
        Objects.requireNonNull(id, "Catedra ID must not be null");
        Objects.requireNonNull(request, "Request must not be null");

        // 1. Validar existencia de la cátedra
        Catedra catedra = catedraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cátedra no encontrada"));

        Materia materia = catedra.getMateria();

        // 2. Validar permisos si el usuario es Administrador de Unidad
        Integer adminUnidadId = resolverUnidadId(username);
        if (adminUnidadId != null) {
            Integer materiaUnidadId = (materia != null && materia.getUnidad() != null)
                    ? materia.getUnidad().getUnidadId()
                    : null;
            if (materiaUnidadId == null || !adminUnidadId.equals(materiaUnidadId)) {
                throw new AccessDeniedException("No tiene permisos para modificar cátedras en materias de otra unidad");
            }
        }

        // 3. Validar que la cátedra se encuentre en estado "Activa"
        if (catedra.getEstado() == null || !"Activa".equalsIgnoreCase(catedra.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede modificar una cátedra que no se encuentra en estado activa");
        }

        // 4. Validar existencia y estado del Docente director
        Docente docente = docenteRepository.findById(Objects.requireNonNull(request.getDirectorId(), "Director ID must not be null"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));

        if (docente.getEstado() == null || !"Activo".equalsIgnoreCase(docente.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El docente debe estar en estado activo");
        }

        // 5. Validar unicidad de nombre de cátedra en la materia excluyendo el propio ID
        String nombreCatedra = request.getNombre() != null ? request.getNombre().trim() : "";
        Integer materiaId = materia != null ? materia.getId() : null;
        boolean existe = catedraRepository.existsByNombreIgnoreCaseAndMateria_IdAndIdNot(nombreCatedra, materiaId, catedra.getId());
        if (existe) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una cátedra con ese nombre para la materia seleccionada");
        }

        // 6. Actualizar atributos editables y persistir
        catedra.setNombre(nombreCatedra);
        catedra.setTipologia(request.getTipologia());
        catedra.setDirector(docente);

        Catedra saved = catedraRepository.save(catedra);
        return mapToDTO(saved);
    }

    @Transactional
    public CatedraResponseDTO darDeBajaCatedra(Integer id, String username) {
        Objects.requireNonNull(id, "Catedra ID must not be null");

        // 1. Validar existencia de la cátedra
        Catedra catedra = catedraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cátedra no encontrada"));

        // 2. Validar permisos si el usuario es Administrador de Unidad
        Integer adminUnidadId = resolverUnidadId(username);
        if (adminUnidadId != null) {
            Materia materia = catedra.getMateria();
            Integer materiaUnidadId = (materia != null && materia.getUnidad() != null)
                    ? materia.getUnidad().getUnidadId()
                    : null;
            if (materiaUnidadId == null || !adminUnidadId.equals(materiaUnidadId)) {
                throw new AccessDeniedException("No tiene permisos para dar de baja cátedras en materias de otra unidad");
            }
        }

        // 3. Validar que la cátedra no se encuentre ya dada de baja
        if (ESTADO_DE_BAJA.equalsIgnoreCase(catedra.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cátedra ya se encuentra dada de baja");
        }

        // 4. Baja lógica: se preservan director (histórico), materia, nombre y tipología
        catedra.setEstado(ESTADO_DE_BAJA);

        Catedra saved = catedraRepository.save(catedra);
        return mapToDTO(saved);
    }
}