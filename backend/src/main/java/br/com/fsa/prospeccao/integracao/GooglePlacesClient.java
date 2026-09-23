package br.com.fsa.prospeccao.integracao;

import br.com.fsa.prospeccao.config.AppProperties;
import br.com.fsa.prospeccao.exception.RegraNegocioException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente da Google Places API (New) — Text Search.
 * Documentação: https://developers.google.com/maps/documentation/places/web-service/text-search
 */
@Component
public class GooglePlacesClient {

    /** A API devolve no máximo 20 por página e 60 por busca. */
    public static final int MAX_RESULTADOS = 60;

    private static final String CAMPOS = String.join(",",
            "places.id", "places.displayName", "places.formattedAddress", "places.addressComponents",
            "places.nationalPhoneNumber", "places.websiteUri", "places.primaryTypeDisplayName",
            "places.businessStatus", "places.googleMapsUri", "nextPageToken");

    private final RestClient http;
    private final String apiKey;

    public GooglePlacesClient(RestClient.Builder builder, AppProperties props) {
        this.http = builder.baseUrl(props.googlePlaces().baseUrl()).build();
        this.apiKey = props.googlePlaces().apiKey();
    }

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<Lugar> buscar(String consulta, int maxResultados) {
        if (!configurado()) {
            throw new RegraNegocioException(
                    "Busca no Google Maps indisponível: configure a variável GOOGLE_PLACES_API_KEY (ou use o OpenStreetMap, que é gratuito)");
        }
        int limite = Math.min(Math.max(maxResultados, 1), MAX_RESULTADOS);
        List<Lugar> lugares = new ArrayList<>();
        String pageToken = null;
        do {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("textQuery", consulta);
            corpo.put("languageCode", "pt-BR");
            corpo.put("regionCode", "BR");
            corpo.put("pageSize", Math.min(20, limite - lugares.size()));
            if (pageToken != null) {
                corpo.put("pageToken", pageToken);
            }
            RespostaBusca resposta;
            try {
                resposta = http.post()
                        .uri("/v1/places:searchText")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Goog-Api-Key", apiKey)
                        .header("X-Goog-FieldMask", CAMPOS)
                        .body(corpo)
                        .retrieve()
                        .body(RespostaBusca.class);
            } catch (RestClientException e) {
                throw new RegraNegocioException("Falha ao consultar o Google Maps: " + e.getMessage());
            }
            if (resposta == null || resposta.places() == null) {
                break;
            }
            lugares.addAll(resposta.places());
            pageToken = resposta.nextPageToken();
        } while (pageToken != null && !pageToken.isBlank() && lugares.size() < limite);
        return lugares.size() > limite ? lugares.subList(0, limite) : lugares;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaBusca(List<Lugar> places, String nextPageToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Lugar(String id, Texto displayName, String formattedAddress, List<ComponenteEndereco> addressComponents,
                        String nationalPhoneNumber, String websiteUri, Texto primaryTypeDisplayName,
                        String businessStatus, String googleMapsUri) {

        public String nome() {
            return displayName == null ? null : displayName.text();
        }

        public String categoria() {
            return primaryTypeDisplayName == null ? null : primaryTypeDisplayName.text();
        }

        /** Componente do endereço pelo tipo (ex.: administrative_area_level_2 = cidade). */
        public ComponenteEndereco componente(String tipo) {
            if (addressComponents == null) {
                return null;
            }
            return addressComponents.stream()
                    .filter(c -> c.types() != null && c.types().contains(tipo))
                    .findFirst().orElse(null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Texto(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ComponenteEndereco(String longText, String shortText, List<String> types) {
    }
}
