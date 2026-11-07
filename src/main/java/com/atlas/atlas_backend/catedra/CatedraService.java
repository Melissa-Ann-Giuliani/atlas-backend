package com.atlas.atlas_backend.catedra;

import com.atlas.atlas_backend.catedra.dto.CatedraRequestDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraResponseDTO;
import com.atlas.atlas_backend.catedra.dto.CatedraUpdateRequestDTO;
import com.atlas.atlas_backend.materia.Materia;
import com.atlas.atlas_backend.materia.MateriaRepository;
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

import java.util.Objects;
import java.util.Optional;

@Service
public class CatedraService {

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

    @Transactional
    public CatedraResponseDTO crearCatedra(CatedraRequestDTO request, String username) {
        Objects.requireNonNull(request, "Request must not be null");

        // 1. Validar existencia de la materia
        Materia materia = materiaRepository.findById(Objects.requireNonNull(request.getMateriaId(), "Materia ID must not be null"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Materia no encontrada"));

        // 2. Validar permisos si el usuario es Administrador de Unidad
        if (username != null) {
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
                        Integer adminUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                        Integer materiaUnidadId = materia.getUnidad() != null ? materia.getUnidad().getUnidadId() : null;
                        if (materiaUnidadId == null || !adminUnidadId.equals(materiaUnidadId)) {
                            throw new AccessDeniedException("No tiene permisos para crear cátedras en materias de otra unidad");
                        }
                    } else {
                        throw new AccessDeniedException("La unidad del administrador no fue encontrada");
                    }
                } else {
                    throw new AccessDeniedException("ID de usuario nulo");
                }
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

        // 6. Mapear a CatedraResponseDTO
        String directorNombre = (docente.getNombre() != null ? docente.getNombre() : "") +
                (docente.getApellido() != null ? " " + docente.getApellido() : "");
        directorNombre = directorNombre.trim();

        return CatedraResponseDTO.builder()
                .id(saved.getId())
                .nombre(saved.getNombre())
                .tipologia(saved.getTipologia())
                .estado(saved.getEstado())
                .directorId(docente.getId())
                .directorNombre(directorNombre)
                .materiaId(materia.getId())
                .materiaNombre(materia.getNombre())
                .build();
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
        if (username != null) {
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
                        Integer adminUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                        Integer materiaUnidadId = (materia != null && materia.getUnidad() != null)
                                ? materia.getUnidad().getUnidadId()
                                : null;
                        if (materiaUnidadId == null || !adminUnidadId.equals(materiaUnidadId)) {
                            throw new AccessDeniedException("No tiene permisos para modificar cátedras en materias de otra unidad");
                        }
                    } else {
                        throw new AccessDeniedException("La unidad del administrador no fue encontrada");
                    }
                } else {
                    throw new AccessDeniedException("ID de usuario nulo");
                }
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

        // 7. Mapear a CatedraResponseDTO
        String directorNombre = (docente.getNombre() != null ? docente.getNombre() : "") +
                (docente.getApellido() != null ? " " + docente.getApellido() : "");
        directorNombre = directorNombre.trim();

        return CatedraResponseDTO.builder()
                .id(saved.getId())
                .nombre(saved.getNombre())
                .tipologia(saved.getTipologia())
                .estado(saved.getEstado())
                .directorId(docente.getId())
                .directorNombre(directorNombre)
                .materiaId(materia != null ? materia.getId() : null)
                .materiaNombre(materia != null ? materia.getNombre() : null)
                .build();
    }
}
