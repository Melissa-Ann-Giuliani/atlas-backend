package com.atlas.atlas_backend.usuario;

import com.atlas.atlas_backend.notification.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.lang.NonNull;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

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

        Rol docenteRol = rolRepository.findByNombre("DOCENTE")
                .orElseThrow(() -> new Exception("Rol DOCENTE no encontrado"));

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
        docente.setDni(request.getDni());
        docente.setCuil(request.getCuil());
        docente.setFechaNac(request.getFechaNac());
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

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DocenteListDTO> getActiveTeachers(
            String username,
            String searchTerm,
            String origen,
            String dedicacion,
            String categoria,
            String caracter,
            String tipoUnidad,
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

        return docenteRepository.findActiveDocentes(unidadId, searchTerm, origen, dedicacion, categoria, caracter, tipoUnidad, pageable);
    }

    @Transactional(readOnly = true)
    public DocenteDetailsDTO getTeacherProfile(@NonNull Integer id) throws Exception {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new Exception("Docente no encontrado"));

        DocenteDetailsDTO dto = new DocenteDetailsDTO();

        DocenteDetailsDTO.PersonalData pd = new DocenteDetailsDTO.PersonalData();
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
                dd.setNroCargo(cargo.getCodigo());
                dd.setCategoria(cargo.getCategoria() != null ? cargo.getCategoria().getCategoriaNombre() : null);
                dd.setDedicacion(cargo.getDedicacion() != null ? cargo.getDedicacion().getDedicacionNombre() : null);
                dd.setCaracter(cargo.getCaracter() != null ? cargo.getCaracter().getCaracterNombre() : null);
                dd.setEstado(
                        cargo.getDesignacion() != null ? cargo.getDesignacion().getDesignacionEstadoActual() : null);
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
                    ad.setMateriaProyecto(f.getMateria() != null ? f.getMateria().getNombre() : f.getNombre());
                    ad.setOrigen(f.getCargo() != null && f.getCargo().getOrigen() != null
                            ? f.getCargo().getOrigen().getOrigenNombre()
                            : null);
                    ad.setHoras(f.getHoras());
                    ad.setEstado(f.getCargo() != null && f.getCargo().getDesignacion() != null
                            ? f.getCargo().getDesignacion().getDesignacionEstadoActual()
                            : null);
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
}
