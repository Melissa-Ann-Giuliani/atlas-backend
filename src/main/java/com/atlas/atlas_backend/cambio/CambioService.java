package com.atlas.atlas_backend.cambio;

import com.atlas.atlas_backend.usuario.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class CambioService {
    private final CambioRepository repository;

    public CambioService(CambioRepository repository) {
        this.repository = repository;
    }

    public void registrarCambio(String tabla, String campo, Object valorAnterior, Object valorNuevo, Usuario usuarioResponsable) {
        String strAnterior = valorAnterior != null ? valorAnterior.toString().trim() : null;
        String strNuevo = valorNuevo != null ? valorNuevo.toString().trim() : null;

        if (!Objects.equals(strAnterior, strNuevo)) {
            Cambio cambio = new Cambio();
            cambio.setTablaAfectada(tabla);
            cambio.setCampoAfectado(campo);
            cambio.setValorAnterior(strAnterior != null ? truncate(strAnterior, 100) : "null");
            cambio.setValorNuevo(strNuevo != null ? truncate(strNuevo, 100) : "null");
            cambio.setFechaHora(LocalDate.now());
            cambio.setUsuario(usuarioResponsable);
            repository.save(cambio);
        }
    }

    private String truncate(String val, int maxLength) {
        if (val == null) return null;
        return val.length() <= maxLength ? val : val.substring(0, maxLength);
    }
}

