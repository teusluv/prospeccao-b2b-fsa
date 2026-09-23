package br.com.fsa.prospeccao.integracao;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Ramos de negócio e as etiquetas (tags) correspondentes no OpenStreetMap.
 * Referência das tags: https://wiki.openstreetmap.org/wiki/Map_features
 */
public enum CategoriaOsm {
    RESTAURANTES("Restaurantes", "[\"amenity\"=\"restaurant\"]"),
    LANCHONETES("Lanchonetes e fast food", "[\"amenity\"=\"fast_food\"]"),
    BARES("Bares", "[\"amenity\"~\"^(bar|pub|biergarten)$\"]"),
    CAFES_PADARIAS("Cafés e padarias", "[\"amenity\"=\"cafe\"]", "[\"shop\"~\"^(bakery|pastry|confectionery)$\"]"),
    SALOES_BELEZA("Salões de beleza e barbearias", "[\"shop\"~\"^(hairdresser|beauty|cosmetics)$\"]"),
    OFICINAS("Oficinas e autopeças", "[\"shop\"~\"^(car_repair|tyres|car_parts|motorcycle)$\"]", "[\"craft\"=\"car_repair\"]"),
    DENTISTAS("Dentistas", "[\"amenity\"=\"dentist\"]", "[\"healthcare\"=\"dentist\"]"),
    CLINICAS("Clínicas e consultórios", "[\"amenity\"~\"^(clinic|doctors)$\"]", "[\"healthcare\"~\"^(clinic|doctor|physiotherapist|psychotherapist)$\"]"),
    ACADEMIAS("Academias", "[\"leisure\"~\"^(fitness_centre|sports_centre)$\"]"),
    PET("Pet shops e veterinários", "[\"shop\"=\"pet\"]", "[\"amenity\"=\"veterinary\"]"),
    ROUPAS("Lojas de roupas e calçados", "[\"shop\"~\"^(clothes|shoes|boutique|fashion_accessories)$\"]"),
    CONSTRUCAO("Materiais de construção", "[\"shop\"~\"^(hardware|doityourself|paint|trade|building_materials)$\"]"),
    MERCADOS("Mercados e mercearias", "[\"shop\"~\"^(supermarket|convenience|greengrocer|butcher|deli)$\"]"),
    FARMACIAS("Farmácias", "[\"amenity\"=\"pharmacy\"]", "[\"shop\"=\"chemist\"]"),
    HOTEIS("Hotéis e pousadas", "[\"tourism\"~\"^(hotel|guest_house|hostel|motel)$\"]"),
    IMOBILIARIAS("Imobiliárias", "[\"office\"=\"estate_agent\"]"),
    CONTABILIDADE("Escritórios de contabilidade", "[\"office\"~\"^(accountant|tax_advisor)$\"]"),
    ADVOCACIA("Escritórios de advocacia", "[\"office\"~\"^(lawyer|notary)$\"]"),
    AUTOESCOLAS("Autoescolas", "[\"amenity\"=\"driving_school\"]"),
    ESCOLAS("Escolas e cursos", "[\"amenity\"~\"^(school|language_school|music_school|kindergarten|prep_school)$\"]"),
    OTICAS("Óticas", "[\"shop\"=\"optician\"]"),
    LOJAS("Qualquer loja", "[\"shop\"]");

    private final String nome;
    private final List<String> filtros;

    CategoriaOsm(String nome, String... filtros) {
        this.nome = nome;
        this.filtros = List.of(filtros);
    }

    public String getNome() {
        return nome;
    }

    /** Filtros Overpass (cada um vira uma consulta unida às outras). */
    public List<String> getFiltros() {
        return filtros;
    }

    /** Aceita a chave ("RESTAURANTES") ou o nome ("restaurantes"), sem diferenciar acentos. */
    public static Optional<CategoriaOsm> encontrar(String termo) {
        if (termo == null) {
            return Optional.empty();
        }
        String t = normalizar(termo);
        for (CategoriaOsm c : values()) {
            if (normalizar(c.name()).equals(t) || normalizar(c.nome).equals(t)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replace('_', ' ').trim();
    }
}
