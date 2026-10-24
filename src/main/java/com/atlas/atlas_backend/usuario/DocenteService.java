package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.lang.NonNull;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.util.Objects;

@Service
public class DocenteService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Docente registrarDocente(DocenteRequest request, StringBuilder warningMessage) throws Exception {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Username ya existe");
        }

        // The institutional email must be unique across all users (check both tables if
        // necessary,
        // though typically they shouldn't overlap if using the same email field in
        // Usuario)
        if (usuarioRepository.existsByCorreo(request.getEmailInstitucional()) ||
                docenteRepository.existsByEmailInstitucional(request.getEmailInstitucional())) {
            throw new Exception("Correo institucional ya existe");
        }

        if (docenteRepository.existsByDni(request.getDni())) {
            throw new Exception("DNI ya existe");
        }

        if (docenteRepository.existsByCuil(request.getCuil())) {
            throw new Exception("CUIL ya existe");
        }

        Rol docenteRol = rolRepository.findByNombre("Docente")
                .or(() -> rolRepository.findByNombre("DOCENTE"))
                .orElseThrow(() -> new Exception("Rol Docente no encontrado"));

        String provisionalPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Docente docente = new Docente();
        docente.setUsername(request.getUsername());
        docente.setContrasenia(passwordEncoder.encode(provisionalPassword));
        docente.setCorreo(request.getEmailInstitucional()); // Store in Usuario as well
        docente.setApellido(request.getApellido());
        docente.setNombre(request.getNombre());
        docente.setActivo(true);
        docente.setDebeCambiarContrasenia(true);
        docente.setFechaReset(LocalDateTime.now());
        docente.setRol(docenteRol);

        // Docente specific fields
        docente.setTipoDni(request.getTipoDni() != null && !request.getTipoDni().isBlank() ? request.getTipoDni() : "DNI");
        docente.setDni(request.getDni());
        docente.setCuil(request.getCuil());
        docente.setEmailInstitucional(request.getEmailInstitucional());
        docente.setFechaIngreso(request.getFechaIngreso());
        docente.setDomicilio(request.getDomicilio());
        docente.setTelefonos(request.getTelefonos());

        // Seniority Logic
        if (request.getAntigPrevia() != null) {
            docente.setAntigPrevia(request.getAntigPrevia());
        } else {
            docente.setAntigPrevia(0);
        }

        if (request.getEstado() != null) {
            docente.setEstado(request.getEstado());
        }

        Docente savedUser = docenteRepository.save(docente);

        try {
            emailService.sendWelcomeDocente(savedUser.getEmailInstitucional(), provisionalPassword);
        } catch (Exception e) {
            if (warningMessage != null) {
                warningMessage.append("El docente fue creado, pero falló el envío del correo de bienvenida.");
            }
        }

        return savedUser;
    }

    @Autowired
    private AdminUnidadRepository adminUnidadRepository;

    @Autowired
    private com.atlas.atlas_backend.designacion.DesignacionRepository designacionRepository;

    @Autowired
    private com.atlas.atlas_backend.cargo.CargoRepository cargoRepository;

    @Autowired
    private com.atlas.atlas_backend.licencia.LicenciaRepository licenciaRepository;

    @Autowired
    private com.atlas.atlas_backend.funcion.FuncionRepository funcionRepository;

    @Autowired
    private com.atlas.atlas_backend.cambio.CambioService cambioService;

    @Autowired
    private com.atlas.atlas_backend.categoria.CategoriaRepository categoriaRepository;

    @Autowired
    private com.atlas.atlas_backend.dedicacion.DedicacionRepository dedicacionRepository;

    @Autowired
    private com.atlas.atlas_backend.caracter.CaracterRepository caracterRepository;

    @Autowired
    private com.atlas.atlas_backend.origen.OrigenRepository origenRepository;

    @Autowired
    private com.atlas.atlas_backend.materia.MateriaRepository materiaRepository;

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DocenteListDTO> getActiveTeachers(
            String username,
            String searchTerm,
            String origen,
            String dedicacion,
            String categoria,
            String caracter,
            String tipoUnidad,
            String estado,
            org.springframework.data.domain.Pageable pageable) throws Exception {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        Integer unidadId = null;
        String rolNombre = usuario.getRol().getNombre();
        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD")) {
            if (usuario.getId() != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    unidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                } else {
                    throw new Exception("La Unidad del Administrador no fue encontrada");
                }
            } else {
                throw new Exception("ID de usuario nulo");
            }
        }

        return docenteRepository.findActiveDocentes(unidadId, searchTerm, origen, dedicacion, categoria, caracter,
                tipoUnidad, estado, pageable);
    }

    @Transactional(readOnly = true)
    public DocenteDetailsDTO getTeacherProfile(@NonNull Integer id) throws Exception {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new Exception("Docente no encontrado"));

        DocenteDetailsDTO dto = new DocenteDetailsDTO();

        DocenteDetailsDTO.PersonalData pd = new DocenteDetailsDTO.PersonalData();
        pd.setTipoDni(docente.getTipoDni());
        pd.setDni(docente.getDni());
        pd.setEmailInstitucional(docente.getEmailInstitucional());
        pd.setTelefonos(docente.getTelefonos());
        pd.setNombre(docente.getNombre());
        pd.setApellido(docente.getApellido());
        pd.setEstado(docente.getEstado());
        dto.setDatosPersonales(pd);

        java.util.List<com.atlas.atlas_backend.designacion.Designacion> designaciones = designacionRepository
                .findByUsuarioId(id);
        java.util.List<Integer> designacionIds = designaciones.stream()
                .filter(d -> d != null && d.getDesignacionId() != null)
                .map(d -> d.getDesignacionId())
                .collect(java.util.stream.Collectors.toList());

        java.util.List<DocenteDetailsDTO.DesignacionData> designacionDataList = new java.util.ArrayList<>();
        java.util.List<DocenteDetailsDTO.ActividadData> actividadDataList = new java.util.ArrayList<>();
        java.util.List<DocenteDetailsDTO.LicenciaData> licenciaDataList = new java.util.ArrayList<>();

        if (!designacionIds.isEmpty()) {
            java.util.List<com.atlas.atlas_backend.cargo.Cargo> cargos = cargoRepository
                    .findByDesignacionDesignacionIdIn(designacionIds);

            if (!cargos.isEmpty()) {
                com.atlas.atlas_backend.unidad.Unidad unidad = cargos.get(0).getUnidad();
                if (unidad != null) {
                    pd.setUnidadNombre(unidad.getUnidadNombre());
                    if (unidad.getTipoUnidad() != null) {
                        pd.setTipoUnidadNombre(unidad.getTipoUnidad().getNombre());
                    }
                }
            }

            for (com.atlas.atlas_backend.cargo.Cargo cargo : cargos) {
                DocenteDetailsDTO.DesignacionData dd = new DocenteDetailsDTO.DesignacionData();
                dd.setLegajo(null); // Docente doesn't have legajo explicitly mapped in requirements, assuming null
                                    // or from designacion
                dd.setNroResolucion(
                        cargo.getDesignacion() != null ? cargo.getDesignacion().getDesignacionNumeroResolucion()
                                : null);
                dd.setFechaInicio(
                        cargo.getDesignacion() != null ? cargo.getDesignacion().getDesignacionFechaInicio() : null);
                dd.setFechaFin(cargo.getDesignacion() != null ? cargo.getDesignacion().getDesignacionFechaFin() : null);
                dd.setCargoId(cargo.getId());
                dd.setNroCargo(cargo.getCodigo());
                dd.setCategoria(cargo.getCategoria() != null ? cargo.getCategoria().getCategoriaNombre() : null);
                dd.setDedicacion(cargo.getDedicacion() != null ? cargo.getDedicacion().getDedicacionNombre() : null);
                dd.setCaracter(cargo.getCaracter() != null ? cargo.getCaracter().getCaracterNombre() : null);
                dd.setEstado(
                        cargo.getDesignacion() != null ? cargo.getDesignacion().getDesignacionEstadoActual() : null);
                dd.setOrigen(cargo.getOrigen() != null ? cargo.getOrigen().getOrigenNombre() : null);
                designacionDataList.add(dd);
            }

            java.util.List<Integer> cargoIds = cargos.stream()
                    .filter(c -> c != null && c.getId() != null)
                    .map(c -> c.getId())
                    .collect(java.util.stream.Collectors.toList());

            if (!cargoIds.isEmpty()) {
                java.util.List<com.atlas.atlas_backend.funcion.Funcion> funciones = funcionRepository
                        .findByCargoIdIn(cargoIds);
                for (com.atlas.atlas_backend.funcion.Funcion f : funciones) {
                    DocenteDetailsDTO.ActividadData ad = new DocenteDetailsDTO.ActividadData();
                    ad.setFuncionId(f.getId());
                    ad.setFuncionNombre(f.getNombre());
                    ad.setFuncionHoras(f.getHoras());
                    ad.setCargoId(f.getCargo() != null ? f.getCargo().getId() : null);
                    ad.setMateriaId(f.getMateria() != null ? f.getMateria().getId() : null);
                    ad.setMateriaNombre(f.getMateria() != null ? f.getMateria().getNombre() : null);
                    actividadDataList.add(ad);
                }
            }

            java.util.List<com.atlas.atlas_backend.licencia.Licencia> licencias = licenciaRepository
                    .findByDesignacionDesignacionIdIn(designacionIds);
            for (com.atlas.atlas_backend.licencia.Licencia l : licencias) {
                DocenteDetailsDTO.LicenciaData ld = new DocenteDetailsDTO.LicenciaData();
                ld.setNroResolucion(l.getNroResolucion());
                ld.setFechaInicio(l.getFechaInicio());
                ld.setFechaFin(l.getFechaFinEstim() != null ? l.getFechaFinEstim() : l.getFechaFin());
                ld.setMotivo(l.getMotivo() != null ? l.getMotivo().getNombre() : null);
                ld.setCantidadDias(l.getCantidadDias());
                licenciaDataList.add(ld);
            }
        }

        dto.setDesignacionesYCargos(designacionDataList);
        dto.setActividadesActuales(actividadDataList);
        dto.setLicenciasActuales(licenciaDataList);

        return dto;
    }

    private Integer getAdminUnidadId(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            return null;
        }
        String rolNombre = usuario.getRol().getNombre();
        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMINISTRADOR_DE_UNIDAD")) {
            if (usuario.getId() != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    return adminUnidadOpt.get().getUnidad().getUnidadId();
                } else {
                    throw new AccessDeniedException("La Unidad del Administrador no fue encontrada");
                }
            } else {
                throw new AccessDeniedException("ID de usuario nulo");
            }
        }
        return null; // Global Admin or other roles
    }

    public void validarAccesoDocente(Usuario usuario, Integer docenteId) {
        Integer unidadId = getAdminUnidadId(usuario);
        if (unidadId != null) {
            List<com.atlas.atlas_backend.designacion.Designacion> designaciones = designacionRepository.findByUsuarioId(docenteId);
            List<Integer> designacionIds = designaciones.stream()
                    .filter(d -> d != null && d.getDesignacionId() != null)
                    .map(d -> d.getDesignacionId())
                    .toList();

            boolean perteneceAUnidad = false;
            if (!designacionIds.isEmpty()) {
                List<com.atlas.atlas_backend.cargo.Cargo> cargos = cargoRepository.findByDesignacionDesignacionIdIn(designacionIds);
                perteneceAUnidad = cargos.stream()
                        .anyMatch(c -> c.getUnidad() != null && Objects.equals(c.getUnidad().getUnidadId(), unidadId));
            }

            if (!perteneceAUnidad) {
                throw new AccessDeniedException("No tiene permisos para modificar o acceder a docentes de otra unidad");
            }
        }
    }

    @Transactional
    public DocenteDetailsDTO modificarPerfilDocente(@NonNull Integer id, DocenteProfileUpdateRequest request, String authUsername) throws Exception {
        Usuario usuarioResponsable = usuarioRepository.findByUsername(authUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));

        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));

        validarAccesoDocente(usuarioResponsable, id);

        // Immutability validation: DNI and CUIL cannot be altered
        if (request.getDni() != null && !Objects.equals(request.getDni(), docente.getDni())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El DNI es inmutable y no puede ser modificado");
        }
        if (request.getCuil() != null && !Objects.equals(request.getCuil(), docente.getCuil())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El CUIL es inmutable y no puede ser modificado");
        }

        // Uniqueness validation: emailInstitucional
        String nuevoEmail = request.getEmailInstitucional() != null ? request.getEmailInstitucional().trim() : null;
        if (nuevoEmail != null && !nuevoEmail.equalsIgnoreCase(docente.getEmailInstitucional())) {
            if (docenteRepository.existsByEmailInstitucionalAndIdNot(nuevoEmail, id) ||
                    usuarioRepository.existsByCorreoAndIdNot(nuevoEmail, id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo institucional ya se encuentra registrado por otro usuario");
            }
        }

        // Audit deltas in cambios table
        cambioService.registrarCambio("docentes", "nombre", docente.getNombre(), request.getNombre(), usuarioResponsable);
        cambioService.registrarCambio("docentes", "apellido", docente.getApellido(), request.getApellido(), usuarioResponsable);
        cambioService.registrarCambio("docentes", "emailInstitucional", docente.getEmailInstitucional(), nuevoEmail, usuarioResponsable);
        cambioService.registrarCambio("docentes", "fechaIngreso", docente.getFechaIngreso(), request.getFechaIngreso(), usuarioResponsable);
        cambioService.registrarCambio("docentes", "domicilio", docente.getDomicilio(), request.getDomicilio(), usuarioResponsable);
        cambioService.registrarCambio("docentes", "antigPrevia", docente.getAntigPrevia(), request.getAntigPrevia(), usuarioResponsable);
        if (request.getTipoDni() != null) {
            cambioService.registrarCambio("docentes", "tipoDni", docente.getTipoDni(), request.getTipoDni(), usuarioResponsable);
        }
        if (request.getEstado() != null) {
            cambioService.registrarCambio("docentes", "estado", docente.getEstado(), request.getEstado(), usuarioResponsable);
        }
        if (request.getTelefonos() != null) {
            String telsAnt = docente.getTelefonos() != null ? String.join(", ", docente.getTelefonos()) : null;
            String telsNuevos = String.join(", ", request.getTelefonos());
            cambioService.registrarCambio("docentes", "telefonos", telsAnt, telsNuevos, usuarioResponsable);
        }

        // Apply changes
        docente.setNombre(request.getNombre());
        docente.setApellido(request.getApellido());
        docente.setEmailInstitucional(nuevoEmail);
        docente.setCorreo(nuevoEmail);
        docente.setFechaIngreso(request.getFechaIngreso());
        docente.setDomicilio(request.getDomicilio());
        if (request.getAntigPrevia() != null) {
            docente.setAntigPrevia(request.getAntigPrevia());
        }
        if (request.getTipoDni() != null && !request.getTipoDni().isBlank()) {
            docente.setTipoDni(request.getTipoDni());
        }
        if (request.getEstado() != null && !request.getEstado().isBlank()) {
            docente.setEstado(request.getEstado());
        }
        if (request.getTelefonos() != null) {
            docente.setTelefonos(request.getTelefonos());
        }

        docenteRepository.save(docente);

        return getTeacherProfile(id);
    }

    @Transactional
    public DocenteDetailsDTO modificarCargoDocente(@NonNull Integer docenteId, @NonNull Integer cargoId, DocenteCargoUpdateRequest request, String authUsername) throws Exception {
        Usuario usuarioResponsable = usuarioRepository.findByUsername(authUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));

        Docente docente = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));

        validarAccesoDocente(usuarioResponsable, docenteId);

        com.atlas.atlas_backend.cargo.Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado"));

        if (cargo.getDesignacion() == null || !Objects.equals(cargo.getDesignacion().getUsuarioId(), docenteId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cargo no está asignado al docente especificado");
        }

        Integer unidadId = getAdminUnidadId(usuarioResponsable);
        if (unidadId != null && (cargo.getUnidad() == null || !Objects.equals(cargo.getUnidad().getUnidadId(), unidadId))) {
            throw new AccessDeniedException("No tiene permisos para modificar cargos de otra unidad");
        }

        com.atlas.atlas_backend.categoria.Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));

        com.atlas.atlas_backend.dedicacion.Dedicacion dedicacion = dedicacionRepository.findById(request.getDedicacionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dedicación no encontrada"));

        com.atlas.atlas_backend.caracter.Caracter caracter = caracterRepository.findById(request.getCaracterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carácter no encontrado"));

        com.atlas.atlas_backend.origen.Origen origen = origenRepository.findById(request.getOrigenId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Origen no encontrado"));

        if (cargo.getHorasOcupadas() != null && cargo.getHorasOcupadas() > 0) {
            if (dedicacion.getDedicacionHoras() != null && dedicacion.getDedicacionHoras() < cargo.getHorasOcupadas()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Las horas de la dedicación (" + dedicacion.getDedicacionHoras() + ") no pueden ser inferiores a las horas ocupadas (" + cargo.getHorasOcupadas() + ")");
            }
        }

        com.atlas.atlas_backend.designacion.Designacion designacion = cargo.getDesignacion();

        // Audit deltas for Designacion
        cambioService.registrarCambio("designaciones", "designacionNumeroResolucion",
                designacion.getDesignacionNumeroResolucion(), request.getNroResolucion(), usuarioResponsable);
        cambioService.registrarCambio("designaciones", "designacionFechaInicio",
                designacion.getDesignacionFechaInicio(), request.getFechaInicio(), usuarioResponsable);
        cambioService.registrarCambio("designaciones", "designacionFechaFin",
                designacion.getDesignacionFechaFin(), request.getFechaFin(), usuarioResponsable);
        if (request.getEstadoDesignacion() != null) {
            cambioService.registrarCambio("designaciones", "designacionEstadoActual",
                    designacion.getDesignacionEstadoActual(), request.getEstadoDesignacion(), usuarioResponsable);
            designacion.setDesignacionEstadoActual(request.getEstadoDesignacion());
        }

        designacion.setDesignacionNumeroResolucion(request.getNroResolucion());
        designacion.setDesignacionFechaInicio(request.getFechaInicio());
        designacion.setDesignacionFechaFin(request.getFechaFin());
        designacionRepository.save(designacion);

        // Audit deltas for Cargo
        String antCat = cargo.getCategoria() != null ? cargo.getCategoria().getCategoriaNombre() : null;
        cambioService.registrarCambio("cargos", "categoria", antCat, categoria.getCategoriaNombre(), usuarioResponsable);

        String antDed = cargo.getDedicacion() != null ? cargo.getDedicacion().getDedicacionNombre() : null;
        cambioService.registrarCambio("cargos", "dedicacion", antDed, dedicacion.getDedicacionNombre(), usuarioResponsable);

        String antCar = cargo.getCaracter() != null ? cargo.getCaracter().getCaracterNombre() : null;
        cambioService.registrarCambio("cargos", "caracter", antCar, caracter.getCaracterNombre(), usuarioResponsable);

        String antOrig = cargo.getOrigen() != null ? cargo.getOrigen().getOrigenNombre() : null;
        cambioService.registrarCambio("cargos", "origen", antOrig, origen.getOrigenNombre(), usuarioResponsable);

        cargo.setCategoria(categoria);
        cargo.setDedicacion(dedicacion);
        cargo.setCaracter(caracter);
        cargo.setOrigen(origen);
        cargoRepository.save(cargo);

        return getTeacherProfile(docenteId);
    }

    @Transactional
    public DocenteDetailsDTO modificarFuncionDocente(@NonNull Integer docenteId, @NonNull Integer funcionId, DocenteFuncionUpdateRequest request, String authUsername) throws Exception {
        Usuario usuarioResponsable = usuarioRepository.findByUsername(authUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));

        Docente docente = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));

        validarAccesoDocente(usuarioResponsable, docenteId);

        com.atlas.atlas_backend.funcion.Funcion funcion = funcionRepository.findById(funcionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Función no encontrada"));

        com.atlas.atlas_backend.cargo.Cargo cargo = funcion.getCargo();
        if (cargo == null || cargo.getDesignacion() == null || !Objects.equals(cargo.getDesignacion().getUsuarioId(), docenteId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La función no pertenece a un cargo del docente indicado");
        }

        Integer unidadId = getAdminUnidadId(usuarioResponsable);
        if (unidadId != null && (cargo.getUnidad() == null || !Objects.equals(cargo.getUnidad().getUnidadId(), unidadId))) {
            throw new AccessDeniedException("No tiene permisos para modificar funciones de un cargo de otra unidad");
        }

        com.atlas.atlas_backend.materia.Materia materia = materiaRepository.findById(request.getMateriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Materia no encontrada"));

        // Validate dedication hours limit
        int totalHorasOtrasFunciones = 0;
        List<com.atlas.atlas_backend.funcion.Funcion> funcionesDelCargo = funcionRepository.findByCargoIdIn(List.of(cargo.getId()));
        for (com.atlas.atlas_backend.funcion.Funcion f : funcionesDelCargo) {
            if (!Objects.equals(f.getId(), funcionId)) {
                totalHorasOtrasFunciones += (f.getHoras() != null ? f.getHoras() : 0);
            }
        }

        int maxHoras = cargo.getDedicacion() != null && cargo.getDedicacion().getDedicacionHoras() != null
                ? cargo.getDedicacion().getDedicacionHoras()
                : Integer.MAX_VALUE;

        if (totalHorasOtrasFunciones + request.getHoras() > maxHoras) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cantidad de horas (" + (totalHorasOtrasFunciones + request.getHoras()) +
                            ") excede el límite de dedicación del cargo (" + maxHoras + " horas)");
        }

        // Audit deltas in cambios table
        cambioService.registrarCambio("funciones", "funcion_nombre", funcion.getNombre(), request.getNombre(), usuarioResponsable);
        cambioService.registrarCambio("funciones", "funcion_horas", funcion.getHoras(), request.getHoras(), usuarioResponsable);
        String antMat = funcion.getMateria() != null ? funcion.getMateria().getNombre() : null;
        cambioService.registrarCambio("funciones", "materia", antMat, materia.getNombre(), usuarioResponsable);

        funcion.setNombre(request.getNombre());
        funcion.setHoras(request.getHoras());
        funcion.setMateria(materia);
        funcionRepository.save(funcion);

        // Update cargo horasOcupadas
        cargo.setHorasOcupadas(totalHorasOtrasFunciones + request.getHoras());
        cargoRepository.save(cargo);

        return getTeacherProfile(docenteId);
    }
}

