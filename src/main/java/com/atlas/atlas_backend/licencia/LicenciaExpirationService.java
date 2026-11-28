package com.atlas.atlas_backend.licencia;

import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.cargo.CargoRepository;
import com.atlas.atlas_backend.designacion.Designacion;
import com.atlas.atlas_backend.designacion.DesignacionRepository;
import com.atlas.atlas_backend.usuario.Docente;
import com.atlas.atlas_backend.usuario.DocenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LicenciaExpirationService {

    private static final Logger log = LoggerFactory.getLogger(LicenciaExpirationService.class);

    @Autowired
    private DesignacionRepository designacionRepository;

    @Autowired
    private LicenciaRepository licenciaRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private CargoRepository cargoRepository;

    @Value("${atlas.licencias.sync-on-startup:true}")
    private boolean syncOnStartup;

    /**
     * Unified definition of an active Licencia on the given reference date:
     * - Associated designacion is currently in estado 'Licencia' (case-insensitive)
     * - fechaInicio <= today
     * - COALESCE(fechaFin, fechaFinEstim) is null OR today <= COALESCE(fechaFin, fechaFinEstim)
     */
    public static boolean isLicenciaActiva(Licencia licencia, LocalDate today) {
        if (licencia == null || today == null) {
            return false;
        }
        Designacion des = licencia.getDesignacion();
        if (des == null || des.getDesignacionEstadoActual() == null
                || !des.getDesignacionEstadoActual().equalsIgnoreCase("Licencia")) {
            return false;
        }
        if (licencia.getFechaInicio() == null || today.isBefore(licencia.getFechaInicio())) {
            return false;
        }
        LocalDate effectiveEnd = resolveEffectiveEndDate(licencia);
        return effectiveEnd == null || !today.isAfter(effectiveEnd);
    }

    public static boolean isLicenciaVencida(Licencia licencia, LocalDate today) {
        if (licencia == null || today == null) {
            return false;
        }
        LocalDate effectiveEnd = resolveEffectiveEndDate(licencia);
        return effectiveEnd != null && effectiveEnd.isBefore(today);
    }

    public static LocalDate resolveEffectiveEndDate(Licencia licencia) {
        if (licencia == null) {
            return null;
        }
        return licencia.getFechaFin() != null ? licencia.getFechaFin() : licencia.getFechaFinEstim();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!syncOnStartup) {
            return;
        }
        int updated = sincronizarLicenciasVencidas();
        if (updated > 0) {
            log.info("Startup licencia sync transitioned {} expired designacion(es)/docente(s) back to Activo", updated);
        }
    }

    @Scheduled(cron = "${atlas.licencias.expiration-cron:0 5 0 * * *}")
    @Transactional
    public int sincronizarLicenciasVencidas() {
        return sincronizarLicenciasVencidas(LocalDate.now());
    }

    @Transactional
    public int sincronizarLicenciasVencidas(LocalDate today) {
        int transitionedCount = 0;

        List<Designacion> designacionesEnLicencia = designacionRepository
                .findByDesignacionEstadoActualIgnoreCase("Licencia");
        List<Integer> desigIds = designacionesEnLicencia.stream()
                .filter(d -> d != null && d.getDesignacionId() != null)
                .map(d -> d.getDesignacionId())
                .toList();

        Map<Integer, List<Licencia>> licenciasPorDesig = new HashMap<>();
        if (!desigIds.isEmpty()) {
            for (Licencia l : licenciaRepository.findByDesignacionDesignacionIdIn(desigIds)) {
                if (l.getDesignacion() != null && l.getDesignacion().getDesignacionId() != null) {
                    licenciasPorDesig
                            .computeIfAbsent(l.getDesignacion().getDesignacionId(), k -> new java.util.ArrayList<>())
                            .add(l);
                }
            }
        }

        Set<Integer> affectedDocenteIds = new HashSet<>();
        List<Integer> expiredDesigIds = new java.util.ArrayList<>();

        for (Designacion des : designacionesEnLicencia) {
            List<Licencia> licencias = licenciasPorDesig.getOrDefault(des.getDesignacionId(), List.of());
            boolean hasActive = licencias.stream().anyMatch(l -> isLicenciaActiva(l, today));
            boolean hasExpired = licencias.stream().anyMatch(l -> isLicenciaVencida(l, today));

            if (!hasActive && hasExpired) {
                des.setDesignacionEstadoActual("Activo");
                designacionRepository.save(des);
                expiredDesigIds.add(des.getDesignacionId());
                if (des.getUsuarioId() != null) {
                    affectedDocenteIds.add(des.getUsuarioId());
                }
                transitionedCount++;
            }
        }

        if (!expiredDesigIds.isEmpty()) {
            List<Cargo> cargos = cargoRepository.findByDesignacionDesignacionIdIn(expiredDesigIds);
            for (Cargo cargo : cargos) {
                if (cargo.getEstado() != null && cargo.getEstado().equalsIgnoreCase("Licencia")) {
                    cargo.setEstado("Asignado");
                    cargoRepository.save(cargo);
                }
            }
        }

        // Reconcile all docentes currently marked 'Licencia' (including those whose designacion was already updated)
        List<Docente> docentesEnLicencia = docenteRepository.findByEstadoIgnoreCase("Licencia");
        Set<Integer> docentesToCheck = docentesEnLicencia.stream()
                .filter(d -> d != null && d.getId() != null)
                .map(d -> d.getId())
                .collect(Collectors.toCollection(HashSet::new));
        docentesToCheck.addAll(affectedDocenteIds);

        for (Integer docenteId : docentesToCheck) {
            if (docenteId == null) {
                continue;
            }
            Docente docente = docenteRepository.findById(docenteId).orElse(null);
            if (docente == null || docente.getEstado() == null
                    || !docente.getEstado().equalsIgnoreCase("Licencia")) {
                continue;
            }

            List<Designacion> docDesigs = designacionRepository.findByUsuarioId(docenteId);
            List<Integer> docDesigIds = docDesigs.stream()
                    .filter(d -> d != null && d.getDesignacionId() != null)
                    .map(d -> d.getDesignacionId())
                    .toList();

            List<Licencia> allDocLicencias = docDesigIds.isEmpty()
                    ? List.of()
                    : licenciaRepository.findByDesignacionDesignacionIdIn(docDesigIds);

            boolean hasAnyActiveLicencia = allDocLicencias.stream().anyMatch(l -> isLicenciaActiva(l, today));
            boolean hasAnyExpiredLicencia = allDocLicencias.stream().anyMatch(l -> isLicenciaVencida(l, today));

            if (!hasAnyActiveLicencia && hasAnyExpiredLicencia) {
                docente.setEstado("Activo");
                docenteRepository.save(docente);
                transitionedCount++;
            }
        }

        return transitionedCount;
    }
}
