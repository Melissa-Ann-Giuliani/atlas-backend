package com.atlas.atlas_backend.materia;

import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.unidad.UnidadRepository;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
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

    public MateriaService(
            MateriaRepository materiaRepository,
            UnidadRepository unidadRepository,
            UsuarioRepository usuarioRepository,
            AdminUnidadRepository adminUnidadRepository) {
        this.materiaRepository = materiaRepository;
        this.unidadRepository = unidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminUnidadRepository = adminUnidadRepository;
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
            if (usuario.getId() != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId());
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
}
