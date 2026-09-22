package br.com.fsa.prospeccao.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
    }

    public record LoginResponse(String token, String tipo, Instant expiraEm, UsuarioDtos.UsuarioResponse usuario) {
    }

    public record TrocarSenhaRequest(@NotBlank String senhaAtual, @NotBlank @Size(min = 6, max = 72) String novaSenha) {
    }
}
