package br.com.fsa.prospeccao.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenStreetMapClientTest {

    private static final String URL = "https://overpass.exemplo/api/interpreter";

    /** Resposta no formato real da Overpass API (out center tags). */
    private static final String RESPOSTA = """
            {"version":0.6,"generator":"Overpass API","elements":[
              {"type":"node","id":101,"lat":-12.26,"lon":-38.96,"tags":{"amenity":"restaurant","name":"Sabor da Feira",
                "phone":"+55 75 3221-0000","addr:street":"Rua Direita","addr:housenumber":"10","addr:suburb":"Centro"}},
              {"type":"way","id":202,"center":{"lat":-12.25,"lon":-38.95},"tags":{"amenity":"restaurant",
                "name":"Restaurante Com Site","website":"https://comsite.com.br","contact:email":"oi@comsite.com.br"}},
              {"type":"node","id":303,"lat":-12.27,"lon":-38.97,"tags":{"amenity":"restaurant","name":"Cantina da Vó",
                "contact:instagram":"@cantinadavo","contact:mobile":"+55 75 99999-0000;+55 75 3222-0000"}},
              {"type":"node","id":404,"lat":-12.28,"lon":-38.98,"tags":{"amenity":"restaurant"}}
            ]}""";

    @Test
    void enviaConsultaEConverteElementos() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("amenity")))
                .andRespond(withSuccess(RESPOSTA, MediaType.APPLICATION_JSON));

        var lugares = new OpenStreetMapClient(builder, URL).buscar("restaurantes", "Feira de Santana", "BA", 200);

        server.verify();
        assertThat(lugares).hasSize(3); // o elemento sem nome é descartado
        var sabor = lugares.get(0);
        assertThat(sabor.idExterno()).isEqualTo("osm:node/101");
        assertThat(sabor.nome()).isEqualTo("Sabor da Feira");
        assertThat(sabor.telefone()).isEqualTo("+55 75 3221-0000");
        assertThat(sabor.endereco()).isEqualTo("Rua Direita, 10 - Centro");
        assertThat(sabor.cidade()).isEqualTo("Feira de Santana");
        assertThat(sabor.uf()).isEqualTo("BA");
        assertThat(sabor.site()).isNull();
        assertThat(sabor.mapaUrl()).isEqualTo("https://www.openstreetmap.org/node/101");
        assertThat(sabor.categoria()).isEqualTo("Restaurante");

        assertThat(lugares.get(1).site()).isEqualTo("https://comsite.com.br");
        assertThat(lugares.get(1).email()).isEqualTo("oi@comsite.com.br");
        assertThat(lugares.get(1).idExterno()).isEqualTo("osm:way/202");

        var cantina = lugares.get(2);
        assertThat(cantina.site()).isEqualTo("https://instagram.com/cantinadavo");
        assertThat(cantina.telefone()).isEqualTo("+55 75 99999-0000");
    }

    @Test
    void montaConsultaPorCategoriaOuPorNome() {
        String porCategoria = OpenStreetMapClient.montarConsulta("Cafés e padarias", "Feira de Santana", "ba", 100);
        assertThat(porCategoria)
                .contains("area[\"ISO3166-2\"=\"BR-BA\"]->.uf;")
                .contains("[\"name\"=\"Feira de Santana\"](area.uf);")
                .contains("nwr(area.cidade)[\"amenity\"=\"cafe\"][\"name\"];")
                .contains("nwr(area.cidade)[\"shop\"~\"^(bakery|pastry|confectionery)$\"][\"name\"];")
                .contains("out center tags 100;");

        String porNome = OpenStreetMapClient.montarConsulta("açaí \"top\" (.*)", "Salvador", null, 50);
        assertThat(porNome)
                .doesNotContain("->.uf")
                .contains("[\"name\"~\"açaí top \",i]");
    }

    @Test
    void categoriaIgnoraAcentoEMaiusculas() {
        assertThat(CategoriaOsm.encontrar("SALOES_BELEZA")).contains(CategoriaOsm.SALOES_BELEZA);
        assertThat(CategoriaOsm.encontrar("farmácias")).contains(CategoriaOsm.FARMACIAS);
        assertThat(CategoriaOsm.encontrar("OTICAS")).contains(CategoriaOsm.OTICAS);
        assertThat(CategoriaOsm.encontrar("pizzaria do joão")).isEmpty();
    }
}
