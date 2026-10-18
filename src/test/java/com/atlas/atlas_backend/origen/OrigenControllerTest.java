package com.atlas.atlas_backend.origen;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Objects;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OrigenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrigenService origenService;

    @Test
    public void testGetAllOrigenes() throws Exception {
        Origen o1 = new Origen();
        o1.setOrigenId(1);
        o1.setOrigenNombre("Planta");

        Origen o2 = new Origen();
        o2.setOrigenId(2);
        o2.setOrigenNombre("Investigación");

        when(origenService.findAll()).thenReturn(Arrays.asList(o1, o2));

        mockMvc.perform(get("/api/origenes")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origenId").value(1))
                .andExpect(jsonPath("$[0].origenNombre").value("Planta"))
                .andExpect(jsonPath("$[1].origenId").value(2))
                .andExpect(jsonPath("$[1].origenNombre").value("Investigación"));
    }
}
