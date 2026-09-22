package br.com.fsa.prospeccao.service;

import br.com.fsa.prospeccao.domain.Empresa;
import br.com.fsa.prospeccao.integracao.GooglePlacesClient;
import br.com.fsa.prospeccao.integracao.GooglePlacesClient.Lugar;
import br.com.fsa.prospeccao.repository.EmpresaRepository;
import br.com.fsa.prospeccao.repository.UsuarioRepository;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaGoogleMapsRequest;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaGoogleMapsResponse;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.LugarEncontrado;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.Situacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Descobre empresas no Google Maps e cadastra como lead as que não têm site. */
@Service
public class ProspeccaoService {

    static final String ORIGEM_GOOGLE_MAPS = "Google Maps";

    private static final Set<String> REDES_SOCIAIS = Set.of(
            "instagram.com", "facebook.com", "fb.com", "wa.me", "whatsapp.com", "api.whatsapp.com",
            "linktr.ee", "tiktok.com", "twitter.com", "x.com", "youtube.com", "linkedin.com",
            "ifood.com.br", "goo.gl", "g.page", "business.site", "sites.google.com");

    private final GooglePlacesClient googlePlaces;
    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;

    public ProspeccaoService(GooglePlacesClient googlePlaces, EmpresaRepository empresas, UsuarioRepository usuarios) {
        this.googlePlaces = googlePlaces;
        this.empresas = empresas;
        this.usuarios = usuarios;
    }

    @Transactional
    public BuscaGoogleMapsResponse buscarSemSiteNoGoogleMaps(BuscaGoogleMapsRequest req, Long usuarioLogadoId) {
        boolean simular = Boolean.TRUE.equals(req.simular());
        boolean redeSocialSemSite = !Boolean.FALSE.equals(req.redeSocialContaComoSemSite());
        String uf = req.uf() == null ? null : req.uf().trim().toUpperCase(Locale.ROOT);
        String consulta = req.termo().trim() + " em " + req.cidade().trim() + (uf == null ? "" : " - " + uf);
        int max = req.maxResultados() == null ? GooglePlacesClient.MAX_RESULTADOS : req.maxResultados();

        List<LugarEncontrado> resultado = new ArrayList<>();
        int semSite = 0;
        int importadas = 0;
        int jaCadastradas = 0;
        for (Lugar lugar : googlePlaces.buscar(consulta, max)) {
            Situacao situacao;
            Long empresaId = null;
            if ("CLOSED_PERMANENTLY".equals(lugar.businessStatus())) {
                situacao = Situacao.FECHADA;
            } else if (temSite(lugar.websiteUri(), redeSocialSemSite)) {
                situacao = Situacao.TEM_SITE;
            } else {
                semSite++;
                if (lugar.id() != null && empresas.existsByGooglePlaceId(lugar.id())) {
                    situacao = Situacao.JA_CADASTRADA;
                    jaCadastradas++;
                } else {
                    situacao = Situacao.IMPORTADA;
                    if (!simular) {
                        empresaId = empresas.save(paraEmpresa(lugar, req, uf, usuarioLogadoId)).getId();
                    }
                    importadas++;
                }
            }
            resultado.add(new LugarEncontrado(lugar.id(), lugar.nome(), lugar.categoria(), lugar.formattedAddress(),
                    lugar.nationalPhoneNumber(), lugar.websiteUri(), lugar.googleMapsUri(), situacao, empresaId));
        }
        return new BuscaGoogleMapsResponse(resultado.size(), semSite, importadas, jaCadastradas, simular, resultado);
    }

    /** Página própria conta como site; perfil em rede social só conta se o usuário pedir. */
    static boolean temSite(String url, boolean redeSocialContaComoSemSite) {
        if (url == null || url.isBlank()) {
            return false;
        }
        if (!redeSocialContaComoSemSite) {
            return true;
        }
        String host = url.toLowerCase(Locale.ROOT).replaceFirst("^[a-z]+://", "").replaceFirst("^www\\.", "");
        int barra = host.indexOf('/');
        if (barra >= 0) {
            host = host.substring(0, barra);
        }
        for (String rede : REDES_SOCIAIS) {
            if (host.equals(rede) || host.endsWith("." + rede)) {
                return false;
            }
        }
        return true;
    }

    private Empresa paraEmpresa(Lugar lugar, BuscaGoogleMapsRequest req, String ufPadrao, Long usuarioLogadoId) {
        var cidade = lugar.componente("administrative_area_level_2");
        var estado = lugar.componente("administrative_area_level_1");
        Empresa e = new Empresa();
        e.setRazaoSocial(cortar(lugar.nome() != null ? lugar.nome() : "Sem nome", 200));
        e.setNomeFantasia(cortar(lugar.nome(), 200));
        e.setSegmento(cortar(lugar.categoria() != null ? lugar.categoria() : req.termo().trim(), 100));
        e.setCidade(cortar(cidade != null ? cidade.longText() : req.cidade().trim(), 100));
        String uf = estado != null && estado.shortText() != null && estado.shortText().length() == 2
                ? estado.shortText().toUpperCase(Locale.ROOT) : ufPadrao;
        e.setUf(uf);
        e.setTelefone(cortar(lugar.nationalPhoneNumber(), 30));
        e.setOrigem(ORIGEM_GOOGLE_MAPS);
        e.setGooglePlaceId(lugar.id());
        StringBuilder obs = new StringBuilder();
        if (lugar.formattedAddress() != null) {
            obs.append("Endereço: ").append(lugar.formattedAddress());
        }
        if (lugar.websiteUri() != null) {
            obs.append(obs.isEmpty() ? "" : "\n").append("Rede social: ").append(lugar.websiteUri());
        }
        if (lugar.googleMapsUri() != null) {
            obs.append(obs.isEmpty() ? "" : "\n").append("Google Maps: ").append(lugar.googleMapsUri());
        }
        e.setObservacoes(obs.isEmpty() ? null : cortar(obs.toString(), 4000));
        e.setResponsavel(usuarios.getReferenceById(usuarioLogadoId));
        return e;
    }

    private static String cortar(String valor, int max) {
        return valor == null || valor.length() <= max ? valor : valor.substring(0, max);
    }
}
