package br.com.fsa.prospeccao;

import br.com.fsa.prospeccao.integracao.GooglePlacesClient;
import br.com.fsa.prospeccao.integracao.GooglePlacesClient.ComponenteEndereco;
import br.com.fsa.prospeccao.integracao.GooglePlacesClient.Lugar;
import br.com.fsa.prospeccao.integracao.GooglePlacesClient.Texto;
import br.com.fsa.prospeccao.integracao.LugarExterno;
import br.com.fsa.prospeccao.integracao.OpenStreetMapClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ProspeccaoGoogleMapsIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockitoBean
    GooglePlacesClient googlePlaces;

    @MockitoBean
    OpenStreetMapClient openStreetMap;

    private static Lugar lugar(String id, String nome, String site, String status) {
        var cidade = new ComponenteEndereco("Feira de Santana", "Feira de Santana", List.of("administrative_area_level_2"));
        var uf = new ComponenteEndereco("Bahia", "BA", List.of("administrative_area_level_1"));
        return new Lugar(id, new Texto(nome), "Rua A, 10 - Centro, Feira de Santana - BA", List.of(cidade, uf),
                "(75) 3221-0000", site, new Texto("Padaria"), status, "https://maps.google.com/?cid=" + id);
    }

    @Test
    void importaSomenteEmpresasSemSiteESemDuplicar() throws Exception {
        when(googlePlaces.buscar(eq("padarias em Feira de Santana - BA"), anyInt())).thenReturn(List.of(
                lugar("g1", "Padaria Sem Site", null, "OPERATIONAL"),
                lugar("g2", "Padaria do Insta", "https://instagram.com/padariadoinsta", "OPERATIONAL"),
                lugar("g3", "Padaria Com Site", "https://padariacomsite.com.br", "OPERATIONAL"),
                lugar("g4", "Padaria Fechada", null, "CLOSED_PERMANENTLY")));

        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fsa.com.br\",\"senha\":\"admin123\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + json.readTree(login).get("token").asText();
        String busca = "{\"termo\":\"padarias\",\"cidade\":\"Feira de Santana\",\"uf\":\"ba\"%s}";

        // simulação não grava nada
        mvc.perform(post("/api/prospeccao/google-maps").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(busca.formatted(",\"simular\":true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulacao").value(true))
                .andExpect(jsonPath("$.importadas").value(2));
        mvc.perform(get("/api/empresas?semSite=true").header("Authorization", token))
                .andExpect(jsonPath("$.totalElementos").value(0));

        mvc.perform(post("/api/prospeccao/google-maps").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(busca.formatted("")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encontradas").value(4))
                .andExpect(jsonPath("$.semSite").value(2))
                .andExpect(jsonPath("$.importadas").value(2))
                .andExpect(jsonPath("$.lugares[2].situacao").value("TEM_SITE"))
                .andExpect(jsonPath("$.lugares[3].situacao").value("FECHADA"));

        // segunda busca não duplica
        mvc.perform(post("/api/prospeccao/google-maps").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(busca.formatted("")))
                .andExpect(jsonPath("$.importadas").value(0))
                .andExpect(jsonPath("$.jaCadastradas").value(2));

        mvc.perform(get("/api/empresas?semSite=true").header("Authorization", token))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(get("/api/empresas?semSite=true&busca=insta").header("Authorization", token))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].razaoSocial").value("Padaria do Insta"))
                .andExpect(jsonPath("$.conteudo[0].origem").value("Google Maps"))
                .andExpect(jsonPath("$.conteudo[0].uf").value("BA"))
                .andExpect(jsonPath("$.conteudo[0].cidade").value("Feira de Santana"));
    }

    @Test
    void openStreetMapEhAFontePadraoESemChave() throws Exception {
        when(openStreetMap.buscar(eq("RESTAURANTES"), eq("Feira de Santana"), eq("BA"), anyInt())).thenReturn(List.of(
                new LugarExterno("osm:node/1", "Tempero Baiano", "restaurant", "Rua A, 1", "Feira de Santana", "BA",
                        "+55 75 3221-0000", "contato@tempero.com", null, "https://www.openstreetmap.org/node/1", false),
                new LugarExterno("osm:node/2", "Bistrô Com Site", "restaurant", null, "Feira de Santana", "BA",
                        null, null, "https://bistro.com.br", "https://www.openstreetmap.org/node/2", false)));
        when(googlePlaces.configurado()).thenReturn(false);

        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fsa.com.br\",\"senha\":\"admin123\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + json.readTree(login).get("token").asText();

        mvc.perform(get("/api/prospeccao/fontes").header("Authorization", token))
                .andExpect(jsonPath("$.googleMapsDisponivel").value(false))
                .andExpect(jsonPath("$.categoriasOpenStreetMap[0].chave").value("RESTAURANTES"));

        mvc.perform(post("/api/prospeccao/buscar").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"termo\":\"RESTAURANTES\",\"cidade\":\"Feira de Santana\",\"uf\":\"BA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("OPENSTREETMAP"))
                .andExpect(jsonPath("$.encontradas").value(2))
                .andExpect(jsonPath("$.importadas").value(1))
                .andExpect(jsonPath("$.lugares[0].idExterno").value("osm:node/1"))
                .andExpect(jsonPath("$.lugares[1].situacao").value("TEM_SITE"));

        mvc.perform(get("/api/empresas?busca=tempero").header("Authorization", token))
                .andExpect(jsonPath("$.conteudo[0].origem").value("OpenStreetMap"))
                .andExpect(jsonPath("$.conteudo[0].email").value("contato@tempero.com"))
                .andExpect(jsonPath("$.conteudo[0].telefone").value("+55 75 3221-0000"));
    }
}
