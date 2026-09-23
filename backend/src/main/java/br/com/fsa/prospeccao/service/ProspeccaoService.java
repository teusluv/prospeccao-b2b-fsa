package br.com.fsa.prospeccao.service;

import br.com.fsa.prospeccao.domain.Empresa;
import br.com.fsa.prospeccao.integracao.CategoriaOsm;
import br.com.fsa.prospeccao.integracao.GooglePlacesClient;
import br.com.fsa.prospeccao.integracao.GooglePlacesClient.Lugar;
import br.com.fsa.prospeccao.integracao.LugarExterno;
import br.com.fsa.prospeccao.integracao.OpenStreetMapClient;
import br.com.fsa.prospeccao.repository.EmpresaRepository;
import br.com.fsa.prospeccao.repository.UsuarioRepository;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaGoogleMapsRequest;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaRequest;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaResponse;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.CategoriaResponse;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.Fonte;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.FontesResponse;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.LugarEncontrado;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.Situacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Descobre empresas (OpenStreetMap ou Google Maps) e cadastra como lead as que não têm site. */
@Service
public class ProspeccaoService {

    private static final Set<String> REDES_SOCIAIS = Set.of(
            "instagram.com", "facebook.com", "fb.com", "wa.me", "whatsapp.com", "api.whatsapp.com",
            "linktr.ee", "tiktok.com", "twitter.com", "x.com", "youtube.com", "linkedin.com",
            "ifood.com.br", "goo.gl", "g.page", "business.site", "sites.google.com");

    private final GooglePlacesClient googlePlaces;
    private final OpenStreetMapClient openStreetMap;
    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;

    public ProspeccaoService(GooglePlacesClient googlePlaces, OpenStreetMapClient openStreetMap,
                             EmpresaRepository empresas, UsuarioRepository usuarios) {
        this.googlePlaces = googlePlaces;
        this.openStreetMap = openStreetMap;
        this.empresas = empresas;
        this.usuarios = usuarios;
    }

    public FontesResponse fontes() {
        var categorias = Arrays.stream(CategoriaOsm.values())
                .map(c -> new CategoriaResponse(c.name(), c.getNome()))
                .toList();
        return new FontesResponse(googlePlaces.configurado(), categorias);
    }

    @Transactional
    public BuscaResponse buscarSemSite(BuscaRequest req, Long usuarioLogadoId) {
        Fonte fonte = req.fonte() == null ? Fonte.OPENSTREETMAP : req.fonte();
        boolean simular = Boolean.TRUE.equals(req.simular());
        boolean redeSocialSemSite = !Boolean.FALSE.equals(req.redeSocialContaComoSemSite());
        String uf = req.uf() == null || req.uf().isBlank() ? null : req.uf().trim().toUpperCase(Locale.ROOT);
        List<LugarExterno> encontrados = switch (fonte) {
            case GOOGLE_MAPS -> buscarNoGoogle(req, uf);
            case OPENSTREETMAP -> openStreetMap.buscar(req.termo(), req.cidade(), uf,
                    req.maxResultados() == null ? 200 : req.maxResultados());
        };

        List<LugarEncontrado> resultado = new ArrayList<>();
        int semSite = 0;
        int importadas = 0;
        int jaCadastradas = 0;
        for (LugarExterno lugar : encontrados) {
            Situacao situacao;
            Long empresaId = null;
            if (lugar.fechado()) {
                situacao = Situacao.FECHADA;
            } else if (temSite(lugar.site(), redeSocialSemSite)) {
                situacao = Situacao.TEM_SITE;
            } else {
                semSite++;
                if (lugar.idExterno() != null && empresas.existsByIdExterno(lugar.idExterno())) {
                    situacao = Situacao.JA_CADASTRADA;
                    jaCadastradas++;
                } else {
                    situacao = Situacao.IMPORTADA;
                    if (!simular) {
                        empresaId = empresas.save(paraEmpresa(lugar, req, fonte, uf, usuarioLogadoId)).getId();
                    }
                    importadas++;
                }
            }
            resultado.add(new LugarEncontrado(lugar.idExterno(), lugar.nome(), lugar.categoria(), lugar.endereco(),
                    lugar.telefone(), lugar.site(), lugar.mapaUrl(), situacao, empresaId));
        }
        return new BuscaResponse(fonte, resultado.size(), semSite, importadas, jaCadastradas, simular, resultado);
    }

    @Transactional
    public BuscaResponse buscarSemSiteGoogle(
            BuscaGoogleMapsRequest req, Long usuarioLogadoId) {
        return buscarSemSite(req.paraBusca(), usuarioLogadoId);
    }

    private List<LugarExterno> buscarNoGoogle(BuscaRequest req, String uf) {
        String consulta = req.termo().trim() + " em " + req.cidade().trim() + (uf == null ? "" : " - " + uf);
        int max = req.maxResultados() == null ? GooglePlacesClient.MAX_RESULTADOS
                : Math.min(req.maxResultados(), GooglePlacesClient.MAX_RESULTADOS);
        return googlePlaces.buscar(consulta, max).stream().map(l -> doGoogle(l, req, uf)).toList();
    }

    private static LugarExterno doGoogle(Lugar l, BuscaRequest req, String ufPadrao) {
        var cidade = l.componente("administrative_area_level_2");
        var estado = l.componente("administrative_area_level_1");
        String uf = estado != null && estado.shortText() != null && estado.shortText().length() == 2
                ? estado.shortText().toUpperCase(Locale.ROOT) : ufPadrao;
        return new LugarExterno(
                l.id() == null ? null : "google:" + l.id(),
                l.nome(),
                l.categoria(),
                l.formattedAddress(),
                cidade != null ? cidade.longText() : req.cidade().trim(),
                uf,
                l.nationalPhoneNumber(),
                null,
                l.websiteUri(),
                l.googleMapsUri(),
                "CLOSED_PERMANENTLY".equals(l.businessStatus()));
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

    private Empresa paraEmpresa(LugarExterno lugar, BuscaRequest req, Fonte fonte, String ufPadrao,
                                Long usuarioLogadoId) {
        Empresa e = new Empresa();
        e.setRazaoSocial(cortar(lugar.nome() != null ? lugar.nome() : "Sem nome", 200));
        e.setNomeFantasia(cortar(lugar.nome(), 200));
        e.setSegmento(cortar(lugar.categoria() != null ? lugar.categoria() : req.termo().trim(), 100));
        e.setCidade(cortar(lugar.cidade() != null ? lugar.cidade() : req.cidade().trim(), 100));
        e.setUf(lugar.uf() != null ? lugar.uf() : ufPadrao);
        e.setTelefone(cortar(lugar.telefone(), 30));
        e.setEmail(lugar.email() != null && lugar.email().contains("@") ? cortar(lugar.email(), 160) : null);
        e.setOrigem(fonte == Fonte.GOOGLE_MAPS ? "Google Maps" : "OpenStreetMap");
        e.setIdExterno(lugar.idExterno());
        StringBuilder obs = new StringBuilder();
        if (lugar.endereco() != null) {
            obs.append("Endereço: ").append(lugar.endereco());
        }
        if (lugar.site() != null) {
            obs.append(obs.isEmpty() ? "" : "\n").append("Rede social: ").append(lugar.site());
        }
        if (lugar.mapaUrl() != null) {
            obs.append(obs.isEmpty() ? "" : "\n").append("Mapa: ").append(lugar.mapaUrl());
        }
        e.setObservacoes(obs.isEmpty() ? null : cortar(obs.toString(), 4000));
        e.setResponsavel(usuarios.getReferenceById(usuarioLogadoId));
        return e;
    }

    private static String cortar(String valor, int max) {
        return valor == null || valor.length() <= max ? valor : valor.substring(0, max);
    }
}
