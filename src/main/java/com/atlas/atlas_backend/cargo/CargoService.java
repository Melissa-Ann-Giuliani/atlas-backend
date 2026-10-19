package com.atlas.atlas_backend.cargo;

import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.DocenteRepository;
import com.atlas.atlas_backend.funcion.Funcion;
import com.atlas.atlas_backend.funcion.FuncionRepository;

import com.atlas.atlas_backend.origen.Origen;
import com.atlas.atlas_backend.origen.OrigenRepository;
import com.atlas.atlas_backend.caracter.Caracter;
import com.atlas.atlas_backend.caracter.CaracterRepository;
import com.atlas.atlas_backend.categoria.Categoria;
import com.atlas.atlas_backend.categoria.CategoriaRepository;
import com.atlas.atlas_backend.dedicacion.Dedicacion;
import com.atlas.atlas_backend.dedicacion.DedicacionRepository;
import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.unidad.UnidadRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CargoService {

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminUnidadRepository adminUnidadRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private FuncionRepository funcionRepository;

    @Autowired
    private OrigenRepository origenRepository;

    @Autowired
    private CaracterRepository caracterRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private DedicacionRepository dedicacionRepository;

    @Autowired
    private UnidadRepository unidadRepository;

    @Transactional(readOnly = true)
    public Page<CargoListDTO> getActiveCargos(String username, String searchTerm, String estado, Integer categoriaId,
            Integer dedicacionId, Integer caracterId, Pageable pageable) throws Exception {
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

        return cargoRepository.findActiveCargos(unidadId, searchTerm, estado, categoriaId, dedicacionId, caracterId,
                pageable);
    }

    @Transactional(readOnly = true)
    public CargoDetailDTO getCargoDetails(Integer cargoId, String username) throws Exception {
        if (cargoId == null) {
            throw new Exception("ID de cargo nulo");
        }
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new Exception("Cargo no encontrado"));

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));
        String rolNombre = usuario.getRol().getNombre();

        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD")) {

            Integer adminUnidadId = null;
            Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
            if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                adminUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
            }

            if (cargo.getUnidad() == null || !cargo.getUnidad().getUnidadId().equals(adminUnidadId)) {
                throw new Exception("No tiene permisos para ver este cargo");
            }
        }

        CargoDetailDTO dto = new CargoDetailDTO();
        dto.setNumero(cargo.getCodigo());
        dto.setCategoria(cargo.getCategoria() != null ? cargo.getCategoria().getCategoriaNombre() : null);
        dto.setEstado(cargo.getEstado());

        if (cargo.getDesignacion() != null && cargo.getDesignacion().getUsuarioId() != null) {
            Optional<Docente> docenteOpt = docenteRepository.findById(cargo.getDesignacion().getUsuarioId().intValue());
            if (docenteOpt.isPresent()) {
                Docente docente = docenteOpt.get();
                dto.setAsignadoANombre(docente.getApellido() + " " + docente.getNombre());
                dto.setAsignadoADni(docente.getDni());
            }
        }

        if (cargo.getDesignacion() != null) {
            dto.setNroResolucionDesignacion(cargo.getDesignacion().getDesignacionNumeroResolucion());
            dto.setDesignacionNroResolucion(cargo.getDesignacion().getDesignacionNumeroResolucion());
            dto.setDesignacionFechaInicio(cargo.getDesignacion().getDesignacionFechaInicio());
            dto.setDesignacionFechaFin(cargo.getDesignacion().getDesignacionFechaFin());
        }

        dto.setFechaCreacion(cargo.getFechaCreacion());
        dto.setDedicacion(cargo.getDedicacion() != null ? cargo.getDedicacion().getDedicacionNombre() : null);
        dto.setCaracter(cargo.getCaracter() != null ? cargo.getCaracter().getCaracterNombre() : null);

        if (cargo.getOrigen() != null) {
            dto.setOrigenId(cargo.getOrigen().getOrigenId());
            dto.setOrigen(cargo.getOrigen().getOrigenNombre());
        }

        if (cargo.getUnidad() != null) {
            dto.setUnidadId(cargo.getUnidad().getUnidadId());
            dto.setUnidadNombre(cargo.getUnidad().getUnidadNombre());
            if (cargo.getUnidad().getTipoUnidad() != null) {
                dto.setTipoUnidadNombre(cargo.getUnidad().getTipoUnidad().getTipoUnidadNombre());
            }
        }

        List<Funcion> funciones = funcionRepository.findByCargoIdIn(List.of(cargo.getId()));
        List<CargoDetailDTO.ActividadData> actividades = new ArrayList<>();
        for (Funcion f : funciones) {
            CargoDetailDTO.ActividadData ad = new CargoDetailDTO.ActividadData();
            ad.setMateriaProyecto(f.getMateria() != null ? f.getMateria().getNombre() : f.getNombre());
            ad.setOrigen(f.getCargo() != null && f.getCargo().getOrigen() != null
                    ? f.getCargo().getOrigen().getOrigenNombre()
                    : null);
            ad.setHoras(f.getHoras());
            ad.setEstado(f.getCargo() != null && f.getCargo().getDesignacion() != null
                    ? f.getCargo().getDesignacion().getDesignacionEstadoActual()
                    : null);
            actividades.add(ad);
        }
        dto.setActividadesVinculadas(actividades);

        if (cargo.getCargoPredecesor() != null) {
            Cargo predecesor = cargo.getCargoPredecesor();
            CargoDetailDTO.CargoPredecesorData pd = new CargoDetailDTO.CargoPredecesorData();
            pd.setNumero(predecesor.getCodigo());
            pd.setEstado(predecesor.getEstado());
            pd.setCategoria(predecesor.getCategoria() != null ? predecesor.getCategoria().getCategoriaNombre() : null);
            dto.setCargoPredecesor(pd);
        }

        if (cargo.getCargoOrigenDivision() != null) {
            Cargo origenDivision = cargo.getCargoOrigenDivision();
            CargoDetailDTO.CargoOrigenDivisionData od = new CargoDetailDTO.CargoOrigenDivisionData();
            od.setNumero(origenDivision.getCodigo());
            od.setEstado(origenDivision.getEstado());
            od.setCategoria(
                    origenDivision.getCategoria() != null ? origenDivision.getCategoria().getCategoriaNombre() : null);
            dto.setCargoOrigenDivision(od);
        }

        List<Cargo> subcargosPredecesor = cargoRepository.findByCargoPredecesorId(cargo.getId());
        List<CargoDetailDTO.SubcargoData> subcargos = new ArrayList<>();
        for (Cargo sc : subcargosPredecesor) {
            CargoDetailDTO.SubcargoData sd = new CargoDetailDTO.SubcargoData();
            sd.setNumero(sc.getCodigo());
            sd.setEstado(sc.getEstado());
            sd.setCategoria(sc.getCategoria() != null ? sc.getCategoria().getCategoriaNombre() : null);
            subcargos.add(sd);
        }

        List<Cargo> subcargosDivision = cargoRepository.findByCargoOrigenDivisionId(cargo.getId());
        List<CargoDetailDTO.SubcargoData> cargosSubdivididos = new ArrayList<>();
        for (Cargo sc : subcargosDivision) {
            CargoDetailDTO.SubcargoData sd = new CargoDetailDTO.SubcargoData();
            sd.setNumero(sc.getCodigo());
            sd.setEstado(sc.getEstado());
            sd.setCategoria(sc.getCategoria() != null ? sc.getCategoria().getCategoriaNombre() : null);
            cargosSubdivididos.add(sd);
            subcargos.add(sd);
        }
        dto.setSubcargosActivos(subcargos);
        dto.setCargosSubdivididos(cargosSubdivididos);

        dto.setDocumentacionAdjunta(new ArrayList<>());

        return dto;
    }

    @Transactional
    public CargoDetailDTO crearCargo(CargoCreateRequest request, String username) throws Exception {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String rolNombre = usuario.getRol().getNombre();
        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD")) {
            if (usuario.getId() != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    Integer userUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                    if (!userUnidadId.equals(request.getUnidadId())) {
                        throw new AccessDeniedException("No tiene permisos para registrar cargos en esta unidad");
                    }
                } else {
                    throw new AccessDeniedException("La Unidad del Administrador no fue encontrada");
                }
            } else {
                throw new AccessDeniedException("ID de usuario nulo");
            }
        }

        if (cargoRepository.existsByCodigo(request.getCodigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El número de cargo ya existe");
        }

        Origen origen = origenRepository.findById(Objects.requireNonNull(request.getOrigenId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Origen no encontrado"));

        Caracter caracter = caracterRepository.findById(Objects.requireNonNull(request.getCaracterId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carácter no encontrado"));

        Categoria categoria = categoriaRepository.findById(Objects.requireNonNull(request.getCategoriaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));

        Dedicacion dedicacion = dedicacionRepository.findById(Objects.requireNonNull(request.getDedicacionId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dedicación no encontrada"));

        Unidad unidad = unidadRepository.findById(Objects.requireNonNull(request.getUnidadId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad no encontrada"));

        Cargo cargo = new Cargo();
        cargo.setCodigo(request.getCodigo());
        cargo.setFechaCreacion(request.getFechaCreacion());
        cargo.setOrigen(origen);
        cargo.setCaracter(caracter);
        cargo.setCategoria(categoria);
        cargo.setDedicacion(dedicacion);
        cargo.setUnidad(unidad);
        cargo.setEstado("Libre");
        cargo.setHorasOcupadas(0);
        cargo.setDesignacion(null);

        if (request.getPredecesorCodigo() != null) {
            Cargo predecesor = cargoRepository.findByCodigo(request.getPredecesorCodigo())
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo predecesor no encontrado"));
            cargo.setCargoPredecesor(predecesor);
        }

        Cargo savedCargo = cargoRepository.save(Objects.requireNonNull(cargo));

        CargoDetailDTO dto = new CargoDetailDTO();
        dto.setNumero(savedCargo.getCodigo());
        dto.setCategoria(categoria.getCategoriaNombre());
        dto.setEstado(savedCargo.getEstado());
        dto.setFechaCreacion(savedCargo.getFechaCreacion());
        dto.setDedicacion(dedicacion.getDedicacionNombre());
        dto.setCaracter(caracter.getCaracterNombre());

        if (savedCargo.getOrigen() != null) {
            dto.setOrigenId(savedCargo.getOrigen().getOrigenId());
            dto.setOrigen(savedCargo.getOrigen().getOrigenNombre());
        }

        if (savedCargo.getUnidad() != null) {
            dto.setUnidadId(savedCargo.getUnidad().getUnidadId());
            dto.setUnidadNombre(savedCargo.getUnidad().getUnidadNombre());
            if (savedCargo.getUnidad().getTipoUnidad() != null) {
                dto.setTipoUnidadNombre(savedCargo.getUnidad().getTipoUnidad().getTipoUnidadNombre());
            }
        }
        dto.setActividadesVinculadas(new ArrayList<>());
        dto.setSubcargosActivos(new ArrayList<>());
        dto.setCargosSubdivididos(new ArrayList<>());
        dto.setDocumentacionAdjunta(new ArrayList<>());

        if (savedCargo.getCargoPredecesor() != null) {
            Cargo predecesor = savedCargo.getCargoPredecesor();
            CargoDetailDTO.CargoPredecesorData pd = new CargoDetailDTO.CargoPredecesorData();
            pd.setNumero(predecesor.getCodigo());
            pd.setEstado(predecesor.getEstado());
            pd.setCategoria(predecesor.getCategoria() != null ? predecesor.getCategoria().getCategoriaNombre() : null);
            dto.setCargoPredecesor(pd);
        }

        return dto;
    }

    @Transactional
    public CargoDetailDTO modificarCargo(Integer id, CargoUpdateRequest request, String username) throws Exception {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID de cargo nulo");
        }

        Cargo cargo = cargoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado"));

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String rolNombre = usuario.getRol().getNombre();
        if (rolNombre.equalsIgnoreCase("Admin de unidad") ||
                rolNombre.equalsIgnoreCase("Administrador de Unidad") ||
                rolNombre.equalsIgnoreCase("ADMIN_UNIDAD") ||
                rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD")) {
            if (usuario.getId() != null) {
                Optional<AdminUnidad> adminUnidadOpt = adminUnidadRepository.findById(usuario.getId().intValue());
                if (adminUnidadOpt.isPresent() && adminUnidadOpt.get().getUnidad() != null) {
                    Integer userUnidadId = adminUnidadOpt.get().getUnidad().getUnidadId();
                    if (cargo.getUnidad() == null || !userUnidadId.equals(cargo.getUnidad().getUnidadId())) {
                        throw new AccessDeniedException("No tiene permisos para modificar cargos en esta unidad");
                    }
                } else {
                    throw new AccessDeniedException("La Unidad del Administrador no fue encontrada");
                }
            } else {
                throw new AccessDeniedException("ID de usuario nulo");
            }
        }

        Origen origen = origenRepository.findById(Objects.requireNonNull(request.getOrigenId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Origen no encontrado"));

        Caracter caracter = caracterRepository.findById(Objects.requireNonNull(request.getCaracterId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carácter no encontrado"));

        Categoria categoria = categoriaRepository.findById(Objects.requireNonNull(request.getCategoriaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));

        Dedicacion dedicacion = dedicacionRepository.findById(Objects.requireNonNull(request.getDedicacionId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dedicación no encontrada"));

        if (cargo.getHorasOcupadas() != null && cargo.getHorasOcupadas() > 0) {
            if (dedicacion.getDedicacionHoras() != null && dedicacion.getDedicacionHoras() < cargo.getHorasOcupadas()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Las horas de la dedicación no pueden ser inferiores a las horas ocupadas");
            }
        }

        if (request.getPredecesorCodigo() != null) {
            if (cargo.getCodigo() != null && cargo.getCodigo().equals(request.getPredecesorCodigo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un cargo no puede ser predecesor de sí mismo");
            }
            Cargo predecesor = cargoRepository.findByCodigo(request.getPredecesorCodigo())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo predecesor no encontrado"));
            cargo.setCargoPredecesor(predecesor);
        }

        cargo.setFechaCreacion(request.getFechaCreacion());
        cargo.setOrigen(origen);
        cargo.setCaracter(caracter);
        cargo.setCategoria(categoria);
        cargo.setDedicacion(dedicacion);

        Cargo savedCargo = cargoRepository.save(cargo);

        return buildCargoDetailDTO(savedCargo, categoria, dedicacion, caracter);
    }

    private CargoDetailDTO buildCargoDetailDTO(Cargo savedCargo, Categoria categoria, Dedicacion dedicacion, Caracter caracter) {
        CargoDetailDTO dto = new CargoDetailDTO();
        dto.setNumero(savedCargo.getCodigo());
        dto.setCategoria(categoria != null ? categoria.getCategoriaNombre() : null);
        dto.setEstado(savedCargo.getEstado());
        dto.setFechaCreacion(savedCargo.getFechaCreacion());
        dto.setDedicacion(dedicacion != null ? dedicacion.getDedicacionNombre() : null);
        dto.setCaracter(caracter != null ? caracter.getCaracterNombre() : null);

        if (savedCargo.getOrigen() != null) {
            dto.setOrigenId(savedCargo.getOrigen().getOrigenId());
            dto.setOrigen(savedCargo.getOrigen().getOrigenNombre());
        }

        if (savedCargo.getUnidad() != null) {
            dto.setUnidadId(savedCargo.getUnidad().getUnidadId());
            dto.setUnidadNombre(savedCargo.getUnidad().getUnidadNombre());
            if (savedCargo.getUnidad().getTipoUnidad() != null) {
                dto.setTipoUnidadNombre(savedCargo.getUnidad().getTipoUnidad().getTipoUnidadNombre());
            }
        }
        dto.setActividadesVinculadas(new ArrayList<>());
        dto.setSubcargosActivos(new ArrayList<>());
        dto.setCargosSubdivididos(new ArrayList<>());
        dto.setDocumentacionAdjunta(new ArrayList<>());

        if (savedCargo.getCargoPredecesor() != null) {
            Cargo predecesor = savedCargo.getCargoPredecesor();
            CargoDetailDTO.CargoPredecesorData pd = new CargoDetailDTO.CargoPredecesorData();
            pd.setNumero(predecesor.getCodigo());
            pd.setEstado(predecesor.getEstado());
            pd.setCategoria(predecesor.getCategoria() != null ? predecesor.getCategoria().getCategoriaNombre() : null);
            dto.setCargoPredecesor(pd);
        }

        if (savedCargo.getCargoOrigenDivision() != null) {
            Cargo origenDivision = savedCargo.getCargoOrigenDivision();
            CargoDetailDTO.CargoOrigenDivisionData od = new CargoDetailDTO.CargoOrigenDivisionData();
            od.setNumero(origenDivision.getCodigo());
            od.setEstado(origenDivision.getEstado());
            od.setCategoria(
                    origenDivision.getCategoria() != null ? origenDivision.getCategoria().getCategoriaNombre() : null);
            dto.setCargoOrigenDivision(od);
        }

        if (savedCargo.getDesignacion() != null && savedCargo.getDesignacion().getUsuarioId() != null && docenteRepository != null) {
            Optional<Docente> docenteOpt = docenteRepository.findById(savedCargo.getDesignacion().getUsuarioId().intValue());
            if (docenteOpt.isPresent()) {
                Docente docente = docenteOpt.get();
                dto.setAsignadoANombre(docente.getApellido() + " " + docente.getNombre());
                dto.setAsignadoADni(docente.getDni());
            }
        }

        if (savedCargo.getDesignacion() != null) {
            dto.setNroResolucionDesignacion(savedCargo.getDesignacion().getDesignacionNumeroResolucion());
            dto.setDesignacionNroResolucion(savedCargo.getDesignacion().getDesignacionNumeroResolucion());
            dto.setDesignacionFechaInicio(savedCargo.getDesignacion().getDesignacionFechaInicio());
            dto.setDesignacionFechaFin(savedCargo.getDesignacion().getDesignacionFechaFin());
        }

        if (savedCargo.getId() != null && funcionRepository != null) {
            List<Funcion> funciones = funcionRepository.findByCargoIdIn(List.of(savedCargo.getId()));
            if (funciones != null) {
                List<CargoDetailDTO.ActividadData> actividades = new ArrayList<>();
                for (Funcion f : funciones) {
                    CargoDetailDTO.ActividadData ad = new CargoDetailDTO.ActividadData();
                    ad.setMateriaProyecto(f.getMateria() != null ? f.getMateria().getNombre() : f.getNombre());
                    ad.setOrigen(f.getCargo() != null && f.getCargo().getOrigen() != null
                            ? f.getCargo().getOrigen().getOrigenNombre()
                            : null);
                    ad.setHoras(f.getHoras());
                    ad.setEstado(f.getCargo() != null && f.getCargo().getDesignacion() != null
                            ? f.getCargo().getDesignacion().getDesignacionEstadoActual()
                            : null);
                    actividades.add(ad);
                }
                dto.setActividadesVinculadas(actividades);
            }
        }

        if (savedCargo.getId() != null && cargoRepository != null) {
            List<Cargo> subcargosPredecesor = cargoRepository.findByCargoPredecesorId(savedCargo.getId());
            if (subcargosPredecesor != null) {
                List<CargoDetailDTO.SubcargoData> subcargos = new ArrayList<>();
                for (Cargo sc : subcargosPredecesor) {
                    CargoDetailDTO.SubcargoData sd = new CargoDetailDTO.SubcargoData();
                    sd.setNumero(sc.getCodigo());
                    sd.setEstado(sc.getEstado());
                    sd.setCategoria(sc.getCategoria() != null ? sc.getCategoria().getCategoriaNombre() : null);
                    subcargos.add(sd);
                }
                dto.setSubcargosActivos(subcargos);
            }

            List<Cargo> subcargosDivision = cargoRepository.findByCargoOrigenDivisionId(savedCargo.getId());
            if (subcargosDivision != null) {
                List<CargoDetailDTO.SubcargoData> cargosSubdivididos = new ArrayList<>();
                for (Cargo sc : subcargosDivision) {
                    CargoDetailDTO.SubcargoData sd = new CargoDetailDTO.SubcargoData();
                    sd.setNumero(sc.getCodigo());
                    sd.setEstado(sc.getEstado());
                    sd.setCategoria(sc.getCategoria() != null ? sc.getCategoria().getCategoriaNombre() : null);
                    cargosSubdivididos.add(sd);
                }
                dto.setCargosSubdivididos(cargosSubdivididos);
            }
        }

        return dto;
    }
}
