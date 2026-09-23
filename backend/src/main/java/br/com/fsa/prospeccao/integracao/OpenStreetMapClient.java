package br.com.fsa.prospeccao.integracao;

import br.com.fsa.prospeccao.config.AppProperties;
import br.com.fsa.prospeccao.exception.RegraNegocioException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Busca empresas no OpenStreetMap pela Overpass API (gratuita, sem chave).
 * Documentação: https://wiki.openstreetmap.org/wiki/Overpass_API
 */
@Component
public class OpenStreetMapClient {

    public static final int MAX_RESULTADOS = 500;

    private final RestClient http;
    private final String url;

    @Autowired
    public OpenStreetMapClient(RestClient.Builder builder, AppProperties props) {
        this(builder.requestFactory(fabricaComTimeouts()), props.osm().overpassUrl());
    }

    /** Usado nos testes, com um builder ligado a um servidor simulado. */
    OpenStreetMapClient(RestClient.Builder builder, String url) {
        this.http = builder.defaultHeader("User-Agent", "ProspectRadar/0.1 (prospeccao-b2b)").build();
        this.url = url;
    }

    /** A Overpass pode demorar em cidades grandes. */
    private static SimpleClientHttpRequestFactory fabricaComTimeouts() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(15_000);
        fabrica.setReadTimeout(120_000);
        return fabrica;
    }

    /**
     * @param termo chave/nome de uma {@link CategoriaOsm} ou, se não for uma, parte do nome da empresa
     */
    public List<LugarExterno> buscar(String termo, String cidade, String uf, int maxResultados) {
        int limite = Math.min(Math.max(maxResultados, 1), MAX_RESULTADOS);
        String consulta = montarConsulta(termo, cidade, uf, limite);
        var form = new LinkedMultiValueMap<String, String>();
        form.add("data", consulta);
        RespostaOverpass resposta;
        try {
            resposta = http.post().uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(RespostaOverpass.class);
        } catch (RestClientException e) {
            throw new RegraNegocioException("Falha ao consultar o OpenStreetMap (tente de novo em instantes): "
                    + e.getMessage());
        }
        if (resposta == null || resposta.elements() == null) {
            return List.of();
        }
        List<LugarExterno> lugares = new ArrayList<>();
        for (Elemento el : resposta.elements()) {
            if (el.tags() != null && el.tags().get("name") != null) {
                lugares.add(converter(el, cidade, uf));
            }
        }
        return lugares.size() > limite ? lugares.subList(0, limite) : lugares;
    }

    static String montarConsulta(String termo, String cidade, String uf, int limite) {
        StringBuilder q = new StringBuilder("[out:json][timeout:90];\n");
        String area = "";
        if (uf != null && !uf.isBlank()) {
            q.append("area[\"ISO3166-2\"=\"BR-").append(uf.trim().toUpperCase()).append("\"]->.uf;\n");
            area = "(area.uf)";
        }
        q.append("rel[\"boundary\"=\"administrative\"][\"admin_level\"=\"8\"][\"name\"=\"")
                .append(escapar(cidade.trim())).append("\"]").append(area).append(";\n")
                .append("map_to_area->.cidade;\n(\n");
        var categoria = CategoriaOsm.encontrar(termo);
        if (categoria.isPresent()) {
            for (String filtro : categoria.get().getFiltros()) {
                q.append("  nwr(area.cidade)").append(filtro).append("[\"name\"];\n");
            }
        } else {
            // busca livre pelo nome; remove caracteres especiais de regex e aspas
            String nome = termo.trim().replaceAll("[\\\\^$.|?*+()\\[\\]{}\"]", "");
            q.append("  nwr(area.cidade)[\"name\"~\"").append(nome)
                    .append("\",i][~\"^(shop|amenity|office|craft|leisure|tourism|healthcare)$\"~\".\"];\n");
        }
        q.append(");\nout center tags ").append(limite).append(";\n");
        return q.toString();
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static LugarExterno converter(Elemento el, String cidadePadrao, String ufPadrao) {
        Map<String, String> t = el.tags();
        String site = primeiro(t, "website", "contact:website", "url");
        if (site == null) {
            site = redeSocial(t);
        }
        String rua = primeiro(t, "addr:street");
        String endereco = null;
        if (rua != null) {
            endereco = rua + (t.get("addr:housenumber") != null ? ", " + t.get("addr:housenumber") : "")
                    + (t.get("addr:suburb") != null ? " - " + t.get("addr:suburb") : "");
        }
        return new LugarExterno(
                "osm:" + el.type() + "/" + el.id(),
                t.get("name"),
                categoria(t),
                endereco,
                primeiro(t, "addr:city") != null ? t.get("addr:city") : cidadePadrao,
                ufPadrao,
                primeiro(t, "phone", "contact:phone", "contact:mobile", "mobile", "contact:whatsapp"),
                primeiro(t, "email", "contact:email"),
                site,
                "https://www.openstreetmap.org/" + el.type() + "/" + el.id(),
                t.containsKey("disused:shop") || t.containsKey("disused:amenity"));
    }

    /** Perfil em rede social, normalizado para URL (o OSM às vezes guarda só o @). */
    private static String redeSocial(Map<String, String> t) {
        String insta = primeiro(t, "contact:instagram", "instagram");
        if (insta != null) {
            return insta.startsWith("http") ? insta : "https://instagram.com/" + insta.replace("@", "");
        }
        String face = primeiro(t, "contact:facebook", "facebook");
        if (face != null) {
            return face.startsWith("http") ? face : "https://facebook.com/" + face;
        }
        return null;
    }

    private static final Map<String, String> TRADUCOES = Map.ofEntries(
            Map.entry("restaurant", "Restaurante"), Map.entry("fast_food", "Lanchonete"), Map.entry("bar", "Bar"),
            Map.entry("pub", "Bar"), Map.entry("cafe", "Café"), Map.entry("bakery", "Padaria"),
            Map.entry("pastry", "Confeitaria"), Map.entry("confectionery", "Doceria"), Map.entry("ice_cream", "Sorveteria"),
            Map.entry("hairdresser", "Salão / barbearia"), Map.entry("beauty", "Salão de beleza"),
            Map.entry("cosmetics", "Cosméticos"), Map.entry("car_repair", "Oficina mecânica"), Map.entry("tyres", "Borracharia"),
            Map.entry("car_parts", "Autopeças"), Map.entry("motorcycle", "Motos"), Map.entry("dentist", "Dentista"),
            Map.entry("clinic", "Clínica"), Map.entry("doctors", "Consultório médico"), Map.entry("doctor", "Consultório médico"),
            Map.entry("physiotherapist", "Fisioterapia"), Map.entry("psychotherapist", "Psicologia"),
            Map.entry("fitness_centre", "Academia"), Map.entry("sports_centre", "Centro esportivo"), Map.entry("pet", "Pet shop"),
            Map.entry("veterinary", "Veterinário"), Map.entry("clothes", "Loja de roupas"), Map.entry("shoes", "Calçados"),
            Map.entry("boutique", "Boutique"), Map.entry("hardware", "Ferragens"), Map.entry("doityourself", "Materiais de construção"),
            Map.entry("paint", "Tintas"), Map.entry("trade", "Materiais de construção"), Map.entry("supermarket", "Supermercado"),
            Map.entry("convenience", "Mercearia"), Map.entry("greengrocer", "Hortifrúti"), Map.entry("butcher", "Açougue"),
            Map.entry("pharmacy", "Farmácia"), Map.entry("chemist", "Drogaria"), Map.entry("hotel", "Hotel"),
            Map.entry("guest_house", "Pousada"), Map.entry("hostel", "Hostel"), Map.entry("motel", "Motel"),
            Map.entry("estate_agent", "Imobiliária"), Map.entry("accountant", "Contabilidade"), Map.entry("tax_advisor", "Contabilidade"),
            Map.entry("lawyer", "Advocacia"), Map.entry("notary", "Cartório"), Map.entry("driving_school", "Autoescola"),
            Map.entry("school", "Escola"), Map.entry("language_school", "Escola de idiomas"), Map.entry("music_school", "Escola de música"),
            Map.entry("kindergarten", "Creche"), Map.entry("optician", "Ótica"), Map.entry("mobile_phone", "Celulares"),
            Map.entry("electronics", "Eletrônicos"), Map.entry("furniture", "Móveis"), Map.entry("jewelry", "Joalheria"),
            Map.entry("florist", "Floricultura"), Map.entry("books", "Livraria"), Map.entry("stationery", "Papelaria"),
            Map.entry("variety_store", "Variedades"), Map.entry("department_store", "Loja de departamentos"),
            Map.entry("laundry", "Lavanderia"), Map.entry("car_wash", "Lava-jato"), Map.entry("fuel", "Posto de combustível"));

    private static String categoria(Map<String, String> t) {
        for (String chave : List.of("shop", "amenity", "office", "craft", "leisure", "tourism", "healthcare")) {
            String valor = t.get(chave);
            if (valor != null) {
                return TRADUCOES.getOrDefault(valor, valor.replace('_', ' '));
            }
        }
        return null;
    }

    private static String primeiro(Map<String, String> t, String... chaves) {
        for (String c : chaves) {
            String v = t.get(c);
            if (v != null && !v.isBlank()) {
                return v.split(";")[0].trim();
            }
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaOverpass(List<Elemento> elements) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Elemento(String type, long id, Map<String, String> tags) {
    }
}
