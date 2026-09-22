package br.com.fsa.prospeccao.web.controller;

import br.com.fsa.prospeccao.domain.EtapaFunil;
import br.com.fsa.prospeccao.exception.RegraNegocioException;
import br.com.fsa.prospeccao.security.UsuarioAutenticado;
import br.com.fsa.prospeccao.service.EmpresaService;
import br.com.fsa.prospeccao.web.dto.EmpresaDtos.EmpresaRequest;
import br.com.fsa.prospeccao.web.dto.EmpresaDtos.EmpresaResponse;
import br.com.fsa.prospeccao.web.dto.EmpresaDtos.ImportacaoResponse;
import br.com.fsa.prospeccao.web.dto.EmpresaDtos.MudarEtapaRequest;
import br.com.fsa.prospeccao.web.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Empresas (leads)")
@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @Operation(summary = "Lista empresas com filtros e paginação",
            description = "Ex.: /api/empresas?busca=acme&etapa=QUALIFICADO&page=0&size=20&sort=razaoSocial,asc")
    @GetMapping
    public PaginaResponse<EmpresaResponse> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) EtapaFunil etapa,
            @RequestParam(required = false) String segmento,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) Long responsavelId,
            @PageableDefault(size = 20, sort = "atualizadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        var filtro = new EmpresaService.Filtro(busca, etapa, segmento, cidade, uf, responsavelId);
        return PaginaResponse.de(service.listar(filtro, pageable));
    }

    @GetMapping("/{id}")
    public EmpresaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<EmpresaResponse> criar(@Valid @RequestBody EmpresaRequest req,
                                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        var e = service.criar(req, usuario.id());
        return ResponseEntity.created(URI.create("/api/empresas/" + e.id())).body(e);
    }

    @PutMapping("/{id}")
    public EmpresaResponse atualizar(@PathVariable Long id, @Valid @RequestBody EmpresaRequest req) {
        return service.atualizar(id, req);
    }

    @Operation(summary = "Move a empresa para outra etapa do funil (motivoPerda obrigatório para PERDIDO)")
    @PatchMapping("/{id}/etapa")
    public EmpresaResponse mudarEtapa(@PathVariable Long id, @Valid @RequestBody MudarEtapaRequest req) {
        return service.mudarEtapa(id, req.etapa(), req.motivoPerda());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @Operation(summary = "Importa empresas de um arquivo CSV (UTF-8, separador ';' ou ',', coluna razaoSocial obrigatória)")
    @PostMapping(path = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportacaoResponse importar(@RequestPart("arquivo") MultipartFile arquivo,
                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        List<String> linhas;
        try (var reader = new BufferedReader(new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            linhas = reader.lines().toList();
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível ler o arquivo enviado");
        }
        var r = service.importarCsv(linhas, usuario.id());
        return new ImportacaoResponse(r.importadas(), r.ignoradas(), r.erros());
    }
}
