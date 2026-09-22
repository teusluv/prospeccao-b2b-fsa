package br.com.fsa.prospeccao.web.controller;

import br.com.fsa.prospeccao.security.UsuarioAutenticado;
import br.com.fsa.prospeccao.service.InteracaoService;
import br.com.fsa.prospeccao.web.dto.InteracaoDtos.InteracaoRequest;
import br.com.fsa.prospeccao.web.dto.InteracaoDtos.InteracaoResponse;
import br.com.fsa.prospeccao.web.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Tag(name = "Interações e follow-ups")
@RestController
@RequestMapping("/api")
public class InteracaoController {

    private final InteracaoService service;

    public InteracaoController(InteracaoService service) {
        this.service = service;
    }

    @GetMapping("/empresas/{empresaId}/interacoes")
    public PaginaResponse<InteracaoResponse> listar(@PathVariable Long empresaId,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return PaginaResponse.de(service.listarDaEmpresa(empresaId, pageable));
    }

    @PostMapping("/empresas/{empresaId}/interacoes")
    public ResponseEntity<InteracaoResponse> registrar(@PathVariable Long empresaId,
                                                       @Valid @RequestBody InteracaoRequest req,
                                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        var i = service.registrar(empresaId, req, usuario);
        return ResponseEntity.created(URI.create("/api/interacoes/" + i.id())).body(i);
    }

    @Operation(summary = "Follow-ups pendentes até a data informada (padrão: agora). meus=true filtra pelo usuário logado")
    @GetMapping("/follow-ups")
    public List<InteracaoResponse> followUps(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(defaultValue = "false") boolean meus,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Instant limite = ate != null ? ate : Instant.now();
        return service.followUps(limite, meus ? usuario.id() : null);
    }

    @PatchMapping("/interacoes/{id}/concluir-follow-up")
    public InteracaoResponse concluirFollowUp(@PathVariable Long id) {
        return service.concluirFollowUp(id);
    }

    @DeleteMapping("/interacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        service.excluir(id, usuario);
    }
}
