package com.atlas.atlas_backend.unidad;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UnidadService {

    private final UnidadRepository unidadRepository;

    public UnidadService(UnidadRepository unidadRepository) {
        this.unidadRepository = unidadRepository;
    }

    public List<UnidadResponse> getUnidades(Integer tipoUnidadId, String tipo) {
        List<Unidad> unidades;
        if (tipoUnidadId != null) {
            unidades = unidadRepository.findByTipoUnidad_Id(tipoUnidadId);
        } else if (tipo != null && !tipo.trim().isEmpty()) {
            unidades = unidadRepository.findByTipoUnidad_NombreIgnoreCase(tipo.trim());
        } else {
            unidades = unidadRepository.findAll();
        }

        return unidades.stream()
                .map(UnidadResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
