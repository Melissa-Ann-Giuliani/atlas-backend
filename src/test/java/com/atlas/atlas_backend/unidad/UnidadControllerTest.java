package com.atlas.atlas_backend.unidad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Objects;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UnidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UnidadService unidadService;

    @Test
    public void testGetUnidades_ByTipoUnidadId() throws Exception {
        UnidadResponse u1 = new UnidadResponse(1, "Creación. Art. Coral", 1, "Centro");
        when(unidadService.getUnidades(1, null)).thenReturn(Collections.singletonList(u1));

        mockMvc.perform(get("/api/unidades?tipoUnidadId=1")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unidadId").value(1))
                .andExpect(jsonPath("$[0].unidadNombre").value("Creación. Art. Coral"))
                .andExpect(jsonPath("$[0].tipoUnidadNombre").value("Centro"));
    }

    @Test
    public void testGetUnidades_ByTipoNombre() throws Exception {
        UnidadResponse u2 = new UnidadResponse(2, "Historia", 2, "Departamento");
        when(unidadService.getUnidades(null, "Departamento")).thenReturn(Collections.singletonList(u2));

        mockMvc.perform(get("/api/unidades?tipo=Departamento")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unidadId").value(2))
                .andExpect(jsonPath("$[0].unidadNombre").value("Historia"))
                .andExpect(jsonPath("$[0].tipoUnidadNombre").value("Departamento"));
    }
}
