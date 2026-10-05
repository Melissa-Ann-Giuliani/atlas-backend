package com.atlas.atlas_backend.cargo;

import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.DocenteRepository;
import com.atlas.atlas_backend.funcion.Funcion;
import com.atlas.atlas_backend.funcion.FuncionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public Page<CargoListDTO> getActiveCargos(String username, String searchTerm, Pageable pageable) throws Exception {
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

        return cargoRepository.findActiveCargos(unidadId, searchTerm, pageable);
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
        dto.setEstado(cargo.getDesignacion() != null ? cargo.getDesignacion().getDesignacionEstadoActual() : null);

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

        List<Funcion> funciones = funcionRepository.findByCargoIdIn(List.of(cargo.getId()));
        List<CargoDetailDTO.ActividadData> actividades = new ArrayList<>();
        for (Funcion f : funciones) {
            CargoDetailDTO.ActividadData ad = new CargoDetailDTO.ActividadData();
            ad.setMateriaProyecto(f.getMateria() != null ? f.getMateria().getNombre() : f.getNombre());
            ad.setOrigen(f.getCargo() != null && f.getCargo().getOrigen() != null ? f.getCargo().getOrigen().getOrigenNombre() : null);
            ad.setHoras(f.getHoras());
            ad.setEstado(f.getCargo() != null && f.getCargo().getDesignacion() != null ? f.getCargo().getDesignacion().getDesignacionEstadoActual() : null);
            actividades.add(ad);
        }
        dto.setActividadesVinculadas(actividades);

        if (cargo.getCargoPredecesor() != null) {
            Cargo predecesor = cargo.getCargoPredecesor();
            CargoDetailDTO.CargoPredecesorData pd = new CargoDetailDTO.CargoPredecesorData();
            pd.setNumero(predecesor.getCodigo());
            pd.setEstado(predecesor.getDesignacion() != null ? predecesor.getDesignacion().getDesignacionEstadoActual() : null);
            pd.setCategoria(predecesor.getCategoria() != null ? predecesor.getCategoria().getCategoriaNombre() : null);
            dto.setCargoPredecesor(pd);
        }

        List<Cargo> subcargosList = cargoRepository.findByCargoPredecesorId(cargo.getId());
        List<CargoDetailDTO.SubcargoData> subcargos = new ArrayList<>();
        for (Cargo sc : subcargosList) {
            CargoDetailDTO.SubcargoData sd = new CargoDetailDTO.SubcargoData();
            sd.setNumero(sc.getCodigo());
            sd.setEstado(sc.getDesignacion() != null ? sc.getDesignacion().getDesignacionEstadoActual() : null);
            sd.setCategoria(sc.getCategoria() != null ? sc.getCategoria().getCategoriaNombre() : null);
            subcargos.add(sd);
        }
        dto.setSubcargosActivos(subcargos);

        dto.setDocumentacionAdjunta(new ArrayList<>()); 

        return dto;
    }
}
