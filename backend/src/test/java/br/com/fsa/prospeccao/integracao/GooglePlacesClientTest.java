package br.com.fsa.prospeccao.integracao;

import br.com.fsa.prospeccao.config.AppProperties;
import br.com.fsa.prospeccao.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GooglePlacesClientTest {

    private static AppProperties props(String chave) {
        return new AppProperties(null, null, null,
                new AppProperties.GooglePlaces(chave, "https://places.googleapis.com"));
    }

    @Test
    void segueAPaginacaoELeOsCampos() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://places.googleapis.com/v1/places:searchText"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "chave"))
                .andExpect(content().json("{\"textQuery\":\"padarias em Feira de Santana\",\"pageSize\":20}"))
                .andRespond(withSuccess("""
                        {"places":[{"id":"p1","displayName":{"text":"Padaria Sol"},
                          "addressComponents":[{"longText":"Feira de Santana","shortText":"Feira de Santana",
                            "types":["administrative_area_level_2","political"]}],
                          "nationalPhoneNumber":"(75) 3221-0000"}],
                         "nextPageToken":"pag2"}""", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://places.googleapis.com/v1/places:searchText"))
                .andExpect(content().json("{\"pageToken\":\"pag2\"}"))
                .andRespond(withSuccess("{\"places\":[{\"id\":\"p2\",\"websiteUri\":\"https://x.com.br\"}]}",
                        MediaType.APPLICATION_JSON));

        var lugares = new GooglePlacesClient(builder, props("chave")).buscar("padarias em Feira de Santana", 60);

        server.verify();
        assertThat(lugares).hasSize(2);
        assertThat(lugares.get(0).nome()).isEqualTo("Padaria Sol");
        assertThat(lugares.get(0).componente("administrative_area_level_2").longText()).isEqualTo("Feira de Santana");
        assertThat(lugares.get(1).websiteUri()).isEqualTo("https://x.com.br");
    }

    @Test
    void semChaveConfiguradaDaErroClaro() {
        assertThatThrownBy(() -> new GooglePlacesClient(RestClient.builder(), props("")).buscar("x", 10))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("GOOGLE_PLACES_API_KEY");
    }
}
