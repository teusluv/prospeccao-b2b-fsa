package br.com.fsa.prospeccao.web.controller;

import br.com.fsa.prospeccao.service.UsuarioService;
import br.com.fsa.prospeccao.web.dto.UsuarioDtos.UsuarioAtualizarRequest;
import br.com.fsa.prospeccao.web.dto.UsuarioDtos.UsuarioCriarRequest;
import br.com.fsa.prospeccao.web.dto.UsuarioDtos.UsuarioResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Gestão de usuários (apenas ADMIN — ver SecurityConfig). */
@Tag(name = "Usuários")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar().stream().map(UsuarioResponse::de).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return UsuarioResponse.de(service.buscar(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioCriarRequest req) {
        var u = service.criar(req);
        return ResponseEntity.created(URI.create("/api/usuarios/" + u.getId())).body(UsuarioResponse.de(u));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioAtualizarRequest req) {
        return UsuarioResponse.de(service.atualizar(id, req));
    }
}
