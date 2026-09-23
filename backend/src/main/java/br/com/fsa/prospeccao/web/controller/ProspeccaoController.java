package br.com.fsa.prospeccao.web.controller;

import br.com.fsa.prospeccao.security.UsuarioAutenticado;
import br.com.fsa.prospeccao.service.ProspeccaoService;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaGoogleMapsRequest;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaRequest;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaResponse;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.FontesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Prospecção automática")
@RestController
@RequestMapping("/api/prospeccao")
public class ProspeccaoController {

    private final ProspeccaoService service;

    public ProspeccaoController(ProspeccaoService service) {
        this.service = service;
    }

    @Operation(summary = "Fontes de busca disponíveis e categorias do OpenStreetMap")
    @GetMapping("/fontes")
    public FontesResponse fontes() {
        return service.fontes();
    }

    @Operation(summary = "Busca empresas e cadastra como lead as que não têm site",
            description = "fonte = OPENSTREETMAP (padrão, gratuito) ou GOOGLE_MAPS (requer GOOGLE_PLACES_API_KEY). "
                    + "No OpenStreetMap, termo pode ser a chave de uma categoria (ver /fontes) ou parte do nome. "
                    + "Ex.: {\"termo\":\"RESTAURANTES\",\"cidade\":\"Feira de Santana\",\"uf\":\"BA\",\"simular\":true}. "
                    + "Empresas que só têm rede social contam como sem site, a menos que redeSocialContaComoSemSite=false. "
                    + "Empresas já importadas não são duplicadas.")
    @PostMapping("/buscar")
    public BuscaResponse buscar(@Valid @RequestBody BuscaRequest req,
                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.buscarSemSite(req, usuario.id());
    }

    @Operation(summary = "Atalho para /buscar com fonte GOOGLE_MAPS")
    @PostMapping("/google-maps")
    public BuscaResponse googleMaps(@Valid @RequestBody BuscaGoogleMapsRequest req,
                                    @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.buscarSemSiteGoogle(req, usuario.id());
    }
}
