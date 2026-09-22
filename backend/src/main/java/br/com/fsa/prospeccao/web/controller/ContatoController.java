package br.com.fsa.prospeccao.web.controller;

import br.com.fsa.prospeccao.service.ContatoService;
import br.com.fsa.prospeccao.web.dto.ContatoDtos.ContatoRequest;
import br.com.fsa.prospeccao.web.dto.ContatoDtos.ContatoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Contatos")
@RestController
@RequestMapping("/api")
public class ContatoController {

    private final ContatoService service;

    public ContatoController(ContatoService service) {
        this.service = service;
    }

    @GetMapping("/empresas/{empresaId}/contatos")
    public List<ContatoResponse> listar(@PathVariable Long empresaId) {
        return service.listarDaEmpresa(empresaId);
    }

    @PostMapping("/empresas/{empresaId}/contatos")
    public ResponseEntity<ContatoResponse> criar(@PathVariable Long empresaId, @Valid @RequestBody ContatoRequest req) {
        var c = service.criar(empresaId, req);
        return ResponseEntity.created(URI.create("/api/contatos/" + c.id())).body(c);
    }

    @GetMapping("/contatos/{id}")
    public ContatoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/contatos/{id}")
    public ContatoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContatoRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/contatos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
