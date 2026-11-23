package com.atlas.atlas_backend.cambio;

import com.atlas.atlas_backend.unidad.Unidad;
import com.atlas.atlas_backend.usuario.AdminUnidad;
import com.atlas.atlas_backend.usuario.AdminUnidadRepository;
import com.atlas.atlas_backend.usuario.Usuario;
import com.atlas.atlas_backend.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CambioService {

    private static final int MAX_VALOR_LENGTH = 255;

    private static final Map<String, String> TABLA_A_CATEGORIA = Map.ofEntries(
            Map.entry("docentes", "Docente"),
            Map.entry("designaciones", "Designación"),
            Map.entry("cargos", "Cargo"),
            Map.entry("funciones", "Función"),
            Map.entry("licencias", "Licencia"),
            Map.entry("renuncias", "Renuncia"),
            Map.entry("subrogancias", "Subrogancia"),
            Map.entry("unidades", "Unidad"),
            Map.entry("materias", "Materia"),
            Map.entry("catedras", "Cátedra"),
            Map.entry("usuarios", "Usuario"),
            Map.entry("admin_globales", "Usuario"),
            Map.entry("admin_de_unidad", "Usuario"),
            Map.entry("auditores", "Usuario"));

    private final CambioRepository repository;
    private final UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private AdminUnidadRepository adminUnidadRepository;

    @Autowired
    public CambioService(CambioRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    public CambioService(CambioRepository repository) {
        this(repository, null);
    }

    @Transactional
    public void registrarCambio(String tabla, String campo, Object valorAnterior, Object valorNuevo,
            Usuario usuarioResponsable) {
        registrarCambio(tabla, campo, valorAnterior, valorNuevo, TipoOperacion.MODIFICACION,
                usuarioResponsable, null, null);
    }

    @Transactional
    public void registrarCambio(String tabla, String campo, Object valorAnterior, Object valorNuevo,
            TipoOperacion tipoOperacion, Usuario usuarioResponsable, Usuario usuarioAfectado, Unidad unidad) {
        Usuario responsable = usuarioResponsable != null ? usuarioResponsable : resolveAuthenticatedUsuario();
        if (responsable == null) {
            return;
        }

        TipoOperacion op = tipoOperacion != null ? tipoOperacion : TipoOperacion.MODIFICACION;
        String strAnterior = valorAnterior != null ? valorAnterior.toString().trim() : null;
        String strNuevo = valorNuevo != null ? valorNuevo.toString().trim() : null;

        if (op == TipoOperacion.MODIFICACION && Objects.equals(strAnterior, strNuevo)) {
            return;
        }

        Cambio cambio = new Cambio();
        cambio.setTablaAfectada(truncate(tabla, 50));
        cambio.setCampoAfectado(truncate(campo, 50));
        cambio.setValorAnterior(truncate(strAnterior, MAX_VALOR_LENGTH));
        cambio.setValorNuevo(truncate(strNuevo, MAX_VALOR_LENGTH));
        cambio.setFechaHora(LocalDateTime.now());
        cambio.setTipoOperacion(op);
        cambio.setUsuario(responsable);
        cambio.setUsuarioAfectado(usuarioAfectado);
        cambio.setUnidad(unidad);
        repository.save(cambio);
    }

    @Transactional(readOnly = true)
    public Page<CambioResponseDTO> consultarHistorial(
            String usuarioAfectado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            TipoOperacion tipoOperacion,
            Integer usuarioId,
            String categoria,
            Integer unidadId,
            @NonNull Pageable pageable) {

        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }

        Usuario authenticated = resolveAuthenticatedUsuario();
        Integer effectiveUnidadId = unidadId;
        boolean requireUsuarioAfectado = false;

        if (authenticated instanceof AdminUnidad adminUnidad) {
            effectiveUnidadId = adminUnidad.getUnidad() != null ? adminUnidad.getUnidad().getUnidadId() : -1;
            requireUsuarioAfectado = true;
        } else if (isAdminUnidadRole(authenticated)) {
            requireUsuarioAfectado = true;
            effectiveUnidadId = resolveAdminUnidadId(authenticated);
        }

        List<String> tablasFiltro = resolveTablasPorCategoria(categoria);
        if (categoria != null && !categoria.isBlank() && tablasFiltro.isEmpty()) {
            return Page.empty(pageable);
        }

        Sort defaultSort = Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id"));
        Sort effectiveSort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Objects.requireNonNull(effectiveSort));

        Specification<Cambio> spec = CambioSpecification.withFilters(
                usuarioAfectado,
                fechaDesde,
                fechaHasta,
                tipoOperacion,
                usuarioId,
                tablasFiltro,
                effectiveUnidadId,
                requireUsuarioAfectado);

        return repository.findAll(spec, sortedPageable).map(this::toResponseDTO);
    }

    public Usuario resolveAuthenticatedUsuario() {
        if (usuarioRepository == null) {
            return null;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return usuarioRepository.findByUsername(auth.getName()).orElse(null);
    }

    public static String mapCategoria(String tablaAfectada) {
        if (tablaAfectada == null) {
            return "Sistema";
        }
        return TABLA_A_CATEGORIA.getOrDefault(tablaAfectada.toLowerCase(), tablaAfectada);
    }

    private boolean isAdminUnidadRole(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            return false;
        }
        String rolNombre = usuario.getRol().getNombre();
        return rolNombre.equalsIgnoreCase("Admin de unidad")
                || rolNombre.equalsIgnoreCase("Administrador de Unidad")
                || rolNombre.equalsIgnoreCase("ADMIN_UNIDAD")
                || rolNombre.equalsIgnoreCase("ADMIN_DE_UNIDAD")
                || rolNombre.equalsIgnoreCase("ADMINISTRADOR_DE_UNIDAD");
    }

    private Integer resolveAdminUnidadId(Usuario usuario) {
        if (adminUnidadRepository != null && usuario != null && usuario.getId() != null) {
            return adminUnidadRepository.findById(Objects.requireNonNull(usuario.getId()))
                    .filter(au -> au.getUnidad() != null)
                    .map(au -> au.getUnidad().getUnidadId())
                    .orElse(-1);
        }
        return -1;
    }

    private List<String> resolveTablasPorCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return List.of();
        }
        String normalized = stripAccents(categoria.trim().toLowerCase());
        return TABLA_A_CATEGORIA.entrySet().stream()
                .filter(e -> stripAccents(e.getValue().toLowerCase()).equals(normalized)
                        || stripAccents(e.getKey()).equals(normalized))
                .map(e -> e.getKey())
                .collect(Collectors.toList());
    }

    private static String stripAccents(String input) {
        if (input == null) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private CambioResponseDTO toResponseDTO(Cambio cambio) {
        CambioResponseDTO.UsuarioResumenDTO responsableDto = null;
        if (cambio.getUsuario() != null) {
            Usuario u = cambio.getUsuario();
            responsableDto = CambioResponseDTO.UsuarioResumenDTO.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .build();
        }

        Integer afectadoId = null;
        String afectadoNombre = null;
        if (cambio.getUsuarioAfectado() != null) {
            Usuario ua = cambio.getUsuarioAfectado();
            afectadoId = ua.getId();
            afectadoNombre = formatNombreCompleto(ua.getApellido(), ua.getNombre());
        }

        return CambioResponseDTO.builder()
                .id(cambio.getId())
                .fechaHora(cambio.getFechaHora())
                .usuarioResponsable(responsableDto)
                .categoria(mapCategoria(cambio.getTablaAfectada()))
                .campoAfectado(cambio.getCampoAfectado())
                .valorAnterior(cambio.getValorAnterior())
                .valorNuevo(cambio.getValorNuevo())
                .tipoOperacion(cambio.getTipoOperacion() != null ? cambio.getTipoOperacion().name() : null)
                .usuarioAfectadoId(afectadoId)
                .usuarioAfectadoNombreCompleto(afectadoNombre)
                .unidadId(cambio.getUnidad() != null ? cambio.getUnidad().getUnidadId() : null)
                .build();
    }

    private static String formatNombreCompleto(String apellido, String nombre) {
        String a = apellido != null ? apellido.trim() : "";
        String n = nombre != null ? nombre.trim() : "";
        if (!a.isEmpty() && !n.isEmpty()) {
            return a + ", " + n;
        }
        if (!a.isEmpty()) {
            return a;
        }
        return !n.isEmpty() ? n : null;
    }

    private String truncate(String val, int maxLength) {
        if (val == null) {
            return null;
        }
        return val.length() <= maxLength ? val : val.substring(0, maxLength);
    }
}
