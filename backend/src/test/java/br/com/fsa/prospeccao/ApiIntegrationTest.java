package br.com.fsa.prospeccao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String tokenAdmin;

    @BeforeEach
    void login() throws Exception {
        tokenAdmin = token("admin@fsa.com.br", "admin123");
    }

    @Test
    void rotasProtegidasExigemToken() throws Exception {
        mvc.perform(get("/api/empresas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fsa.com.br\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vendedorNaoGerenciaUsuariosNemExcluiEmpresa() throws Exception {
        mvc.perform(auth(post("/api/usuarios"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@fsa.com.br\",\"senha\":\"123456\",\"perfil\":\"VENDEDOR\"}"))
                .andExpect(status().isCreated());
        String tokenVendedor = token("ana@fsa.com.br", "123456");

        mvc.perform(auth(get("/api/usuarios"), tokenVendedor)).andExpect(status().isForbidden());
        long id = criarEmpresa(tokenVendedor, "{\"razaoSocial\":\"Loja da Ana\"}");
        mvc.perform(auth(delete("/api/empresas/" + id), tokenVendedor)).andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/empresas/" + id), tokenAdmin)).andExpect(status().isNoContent());
    }

    @Test
    void fluxoCompletoDeProspeccao() throws Exception {
        long empresaId = criarEmpresa(tokenAdmin, """
                {"razaoSocial":"Acme Comércio LTDA","nomeFantasia":"Acme","cnpj":"11.222.333/0001-81",
                 "segmento":"Varejo","cidade":"Feira de Santana","uf":"ba","valorEstimado":15000}""");

        // CNPJ duplicado e inválido
        mvc.perform(auth(post("/api/empresas"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Outra\",\"cnpj\":\"11222333000181\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(auth(post("/api/empresas"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Outra\",\"cnpj\":\"11222333000182\"}"))
                .andExpect(status().isUnprocessableEntity());
        // validação de campos
        mvc.perform(auth(post("/api/empresas"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.razaoSocial").exists());

        // busca e filtros
        mvc.perform(auth(get("/api/empresas?busca=acme&uf=BA"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].uf").value("BA"))
                .andExpect(jsonPath("$.conteudo[0].etapa").value("NOVO"))
                .andExpect(jsonPath("$.conteudo[0].responsavel.nome").value("Administrador"));

        // contato + interação com follow-up vencido
        String contato = mvc.perform(auth(post("/api/empresas/" + empresaId + "/contatos"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"João Compras\",\"cargo\":\"Comprador\",\"principal\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long contatoId = json.readTree(contato).get("id").asLong();

        mvc.perform(auth(post("/api/empresas/" + empresaId + "/interacoes"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"LIGACAO","descricao":"Apresentação inicial","contatoId":%d,
                                 "proximoPasso":"Enviar proposta","dataFollowUp":"2020-01-01T12:00:00Z"}"""
                                .formatted(contatoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contatoNome").value("João Compras"));

        // primeira interação move NOVO -> CONTATADO
        mvc.perform(auth(get("/api/empresas/" + empresaId), tokenAdmin))
                .andExpect(jsonPath("$.etapa").value("CONTATADO"));

        String followUps = mvc.perform(auth(get("/api/follow-ups?meus=true"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        long interacaoId = json.readTree(followUps).get(0).get("id").asLong();

        mvc.perform(auth(get("/api/dashboard"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmpresas").value(1))
                .andExpect(jsonPath("$.followUpsAtrasados").value(1))
                .andExpect(jsonPath("$.valorEmAberto").value(15000.0));

        mvc.perform(auth(patch("/api/interacoes/" + interacaoId + "/concluir-follow-up"), tokenAdmin))
                .andExpect(jsonPath("$.followUpConcluido").value(true));

        // PERDIDO exige motivo; GANHO conta na conversão
        mvc.perform(auth(patch("/api/empresas/" + empresaId + "/etapa"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"etapa\":\"PERDIDO\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(auth(patch("/api/empresas/" + empresaId + "/etapa"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"etapa\":\"GANHO\"}"))
                .andExpect(jsonPath("$.etapa").value("GANHO"));

        mvc.perform(auth(get("/api/dashboard"), tokenAdmin))
                .andExpect(jsonPath("$.taxaConversao").value(1.0))
                .andExpect(jsonPath("$.valorGanho").value(15000.0))
                .andExpect(jsonPath("$.followUpsPendentes").value(0));
    }

    @Test
    void importaCsv() throws Exception {
        String csv = """
                razaoSocial;cnpj;cidade;uf;porte;valorEstimado
                Padaria Central;11.444.777/0001-61;Feira de Santana;BA;MICRO;1.500,50
                "Mercado Bom Preço; Filial";;Salvador;BA;;
                Sem CNPJ Válido;123;Salvador;BA;;
                Duplicada;11444777000161;Salvador;BA;;
                """;
        var arquivo = new MockMultipartFile("arquivo", "leads.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        mvc.perform(auth(multipart("/api/empresas/importar").file(arquivo), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importadas").value(2))
                .andExpect(jsonPath("$.ignoradas").value(2))
                .andExpect(jsonPath("$.erros.length()").value(2));

        mvc.perform(auth(get("/api/empresas?busca=filial"), tokenAdmin))
                .andExpect(jsonPath("$.conteudo[0].razaoSocial").value("Mercado Bom Preço; Filial"));
        mvc.perform(auth(get("/api/empresas?busca=padaria"), tokenAdmin))
                .andExpect(jsonPath("$.conteudo[0].valorEstimado").value(1500.50));
    }

    private long criarEmpresa(String token, String body) throws Exception {
        String resp = mvc.perform(auth(post("/api/empresas"), token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(resp).get("id").asLong();
    }

    private String token(String email, String senha) throws Exception {
        String resp = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(resp);
        assertThat(node.get("tipo").asText()).isEqualTo("Bearer");
        return node.get("token").asText();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token);
    }
}
