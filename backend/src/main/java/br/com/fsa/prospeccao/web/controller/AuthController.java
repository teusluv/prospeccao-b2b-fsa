package br.com.fsa.prospeccao.web.controller;

import br.com.fsa.prospeccao.security.JwtService;
import br.com.fsa.prospeccao.security.UsuarioAutenticado;
import br.com.fsa.prospeccao.service.UsuarioService;
import br.com.fsa.prospeccao.web.dto.AuthDtos.LoginRequest;
import br.com.fsa.prospeccao.web.dto.AuthDtos.LoginResponse;
import br.com.fsa.prospeccao.web.dto.AuthDtos.TrocarSenhaRequest;
import br.com.fsa.prospeccao.web.dto.UsuarioDtos.UsuarioResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim(), req.senha()));
        UsuarioAutenticado usuario = (UsuarioAutenticado) auth.getPrincipal();
        var token = jwtService.gerar(usuario);
        return new LoginResponse(token.token(), "Bearer", token.expiraEm(),
                UsuarioResponse.de(usuarioService.buscar(usuario.id())));
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return UsuarioResponse.de(usuarioService.buscar(usuario.id()));
    }

    @PostMapping("/trocar-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@AuthenticationPrincipal UsuarioAutenticado usuario,
                            @Valid @RequestBody TrocarSenhaRequest req) {
        usuarioService.trocarSenha(usuario.id(), req.senhaAtual(), req.novaSenha());
    }
}
