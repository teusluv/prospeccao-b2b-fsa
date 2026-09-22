package br.com.fsa.prospeccao.web.controller;

import br.com.fsa.prospeccao.security.UsuarioAutenticado;
import br.com.fsa.prospeccao.service.ProspeccaoService;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaGoogleMapsRequest;
import br.com.fsa.prospeccao.web.dto.ProspeccaoDtos.BuscaGoogleMapsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Operation(summary = "Busca empresas no Google Maps e cadastra como lead as que não têm site",
            description = "Ex.: {\"termo\":\"restaurantes\",\"cidade\":\"Feira de Santana\",\"uf\":\"BA\"}. "
                    + "Com simular=true só lista o que seria importado. Por padrão, empresas que só têm "
                    + "Instagram/Facebook/WhatsApp contam como sem site (redeSocialContaComoSemSite=false desliga). "
                    + "Empresas já importadas antes não são duplicadas.")
    @PostMapping("/google-maps")
    public BuscaGoogleMapsResponse googleMaps(@Valid @RequestBody BuscaGoogleMapsRequest req,
                                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.buscarSemSiteNoGoogleMaps(req, usuario.id());
    }
}
