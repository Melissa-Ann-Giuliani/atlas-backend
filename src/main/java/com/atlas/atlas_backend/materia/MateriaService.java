package com.atlas.atlas_backend.materia;

import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.unidad.UnidadRepository;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import com.atlas.atlas_backend.catedra.CatedraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class MateriaService {

    private static final Set<String> DESPLIEGUES_PERMITIDOS = Set.of("Anual", "Primer Semestre", "Segundo Semestre");

    private final MateriaRepository materiaRepository;
    private final UnidadRepository unidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminUnidadRepository adminUnidadRepository;
    private final CatedraRepository catedraRepository;

    public MateriaService(
            MateriaRepository materiaRepository,
            UnidadRepository unidadRepository,
            UsuarioRepository usuarioRepository,
            AdminUnidadRepository adminUnidadRepository,
            CatedraRepository catedraRepository) {
        this.materiaRepository = materiaRepository;
        this.unidadRepository = unidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminUnidadRepository = adminUnidadRepository;
        this.catedraRepository = catedraRepository;
    }

    @Transactional
    public MateriaDetailDTO crearMateria(MateriaCreateRequest request, String username) {
        Objects.requireNonNull(request, "Request must not be null");

        // 1. Validar despliegue
        if (request.getDespliegue() == null || !DESPLIEGUES_PERMITIDOS.contains(request.getDespliegue())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El despliegue debe ser 'Anual', 'Primer Semestre' o 'Segundo Semestre'");
        }

        // 2. Validar horas
        if (request.getHoras() == null || request.getHoras() < 1 || request.getHoras() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las horas semanales deben ser un valor entre 1 y 40");
        }

        // 3. Validar permisos y scoping de unidad para ADMIN_UNIDAD
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD")) {
            Integer usuarioId = usuario.getId();
            if (usuarioId != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(Objects.requireNonNull(usuarioId));
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    Integer userUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                    if (!userUnidadId.equals(request.getUnidadId())) {
                        throw new AccessDeniedException("No tiene permisos para registrar materias en esta unidad académica");
                    }
                } else {
                    throw new AccessDeniedException("La unidad del administrador no fue encontrada");
                }
            } else {
                throw new AccessDeniedException("ID de usuario nulo");
            }
        }

        // 4. Validar existencia de la unidad académica
        Unidad unidad = unidadRepository.findById(Objects.requireNonNull(request.getUnidadId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad académica no encontrada"));

        // 5. Si se proporcionó dependenciaId, validar concordancia con el TipoUnidad de la Unidad
        if (request.getDependenciaId() != null) {
            if (unidad.getTipoUnidad() == null || !request.getDependenciaId().equals(unidad.getTipoUnidad().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La dependencia especificada no coincide con la unidad académica seleccionada");
            }
        }

        // 6. Validar unicidad (nombre case-insensitive, unidad_id, planEstudio)
        boolean exists = materiaRepository.existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudio(
                request.getNombre().trim(),
                request.getUnidadId(),
                request.getPlanEstudio()
        );
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una materia con ese nombre para esta unidad y plan de estudio");
        }

        // 7. Persistir entidad
        Materia materia = new Materia();
        materia.setNombre(request.getNombre().trim());
        materia.setHoras(request.getHoras());
        materia.setAnio(request.getAnio().trim());
        materia.setDespliegue(request.getDespliegue());
        materia.setPlanEstudio(request.getPlanEstudio());
        materia.setUnidad(unidad);

        Materia saved = materiaRepository.save(materia);

        // 8. Retornar DTO
        MateriaDetailDTO dto = new MateriaDetailDTO();
        dto.setId(saved.getId());
        dto.setNombre(saved.getNombre());
        dto.setHoras(saved.getHoras());
        dto.setAnio(saved.getAnio());
        dto.setDespliegue(saved.getDespliegue());
        dto.setPlanEstudio(saved.getPlanEstudio());
        dto.setUnidadId(unidad.getUnidadId());
        dto.setUnidadNombre(unidad.getUnidadNombre());
        dto.setDependenciaId(unidad.getTipoUnidad() != null ? unidad.getTipoUnidad().getId() : null);
        dto.setDependenciaNombre(unidad.getTipoUnidad() != null ? unidad.getTipoUnidad().getNombre() : null);
        return dto;
    }

    @Transactional
    public MateriaDetailDTO modificarMateria(Integer id, MateriaPatchRequest request, String username) {
        Objects.requireNonNull(id, "ID must not be null");
        Objects.requireNonNull(request, "Request must not be null");

        // 1. Validar existencia de la materia
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Materia no encontrada"));

        // 2. Validar permisos y scoping de unidad para ADMIN_UNIDAD
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD")) {
            Integer usuarioId = usuario.getId();
            if (usuarioId != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(Objects.requireNonNull(usuarioId));
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    Integer userUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                    Integer materiaUnidadId = materia.getUnidad() != null ? materia.getUnidad().getUnidadId() : null;
                    if (!userUnidadId.equals(materiaUnidadId)) {
                        throw new AccessDeniedException("No tiene permisos para modificar materias en esta unidad académica");
                    }
                } else {
                    throw new AccessDeniedException("La unidad del administrador no fue encontrada");
                }
            } else {
                throw new AccessDeniedException("ID de usuario nulo");
            }
        }

        // 3. Validar horas si se proporcionan
        if (request.getHoras() != null) {
            if (request.getHoras() < 1 || request.getHoras() > 40) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las horas semanales deben ser un valor entre 1 y 40");
            }
        }

        // 4. Validar despliegue si se proporciona
        if (request.getDespliegue() != null) {
            if (!DESPLIEGUES_PERMITIDOS.contains(request.getDespliegue())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El despliegue debe ser 'Anual', 'Primer Semestre' o 'Segundo Semestre'");
            }
        }

        // 5. Validar impacto en cátedras activas si cambian horas o despliegue
        boolean modifiesHoras = request.getHoras() != null && !request.getHoras().equals(materia.getHoras());
        boolean modifiesDespliegue = request.getDespliegue() != null && !request.getDespliegue().equals(materia.getDespliegue());

        if (modifiesHoras || modifiesDespliegue) {
            boolean hasActiveCatedras = catedraRepository.existsByMateria_IdAndEstadoIgnoreCase(id, "ACTIVA");
            if (hasActiveCatedras) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pueden modificar horas o despliegue porque la materia tiene cátedras activas asociadas");
            }
        }

        // 6. Validar unicidad si se modifica el nombre
        if (request.getNombre() != null) {
            String trimmedNombre = request.getNombre().trim();
            if (trimmedNombre.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la materia no puede estar vacío");
            }
            if (!trimmedNombre.equalsIgnoreCase(materia.getNombre())) {
                Integer unidadId = materia.getUnidad() != null ? materia.getUnidad().getUnidadId() : null;
                boolean duplicate = materiaRepository.existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot(
                        trimmedNombre,
                        unidadId,
                        materia.getPlanEstudio(),
                        materia.getId()
                );
                if (duplicate) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra materia con ese nombre para esta unidad y plan de estudio");
                }
            }
        }

        // 7. Aplicar modificaciones parciales
        if (request.getNombre() != null) {
            materia.setNombre(request.getNombre().trim());
        }
        if (request.getHoras() != null) {
            materia.setHoras(request.getHoras());
        }
        if (request.getAnio() != null) {
            materia.setAnio(request.getAnio().trim());
        }
        if (request.getDespliegue() != null) {
            materia.setDespliegue(request.getDespliegue());
        }

        Materia saved = materiaRepository.save(Objects.requireNonNull(materia));

        // 8. Retornar DTO
        Unidad unidad = saved.getUnidad();
        MateriaDetailDTO dto = new MateriaDetailDTO();
        dto.setId(saved.getId());
        dto.setNombre(saved.getNombre());
        dto.setHoras(saved.getHoras());
        dto.setAnio(saved.getAnio());
        dto.setDespliegue(saved.getDespliegue());
        dto.setPlanEstudio(saved.getPlanEstudio());
        if (unidad != null) {
            dto.setUnidadId(unidad.getUnidadId());
            dto.setUnidadNombre(unidad.getUnidadNombre());
            dto.setDependenciaId(unidad.getTipoUnidad() != null ? unidad.getTipoUnidad().getId() : null);
            dto.setDependenciaNombre(unidad.getTipoUnidad() != null ? unidad.getTipoUnidad().getNombre() : null);
        }
        return dto;
    }
}
