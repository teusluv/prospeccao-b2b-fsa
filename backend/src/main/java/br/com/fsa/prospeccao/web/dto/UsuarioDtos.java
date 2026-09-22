package br.com.fsa.prospeccao.web.dto;

import br.com.fsa.prospeccao.domain.Perfil;
import br.com.fsa.prospeccao.domain.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    public record UsuarioCriarRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 6, max = 72) String senha,
            @NotNull Perfil perfil) {
    }

    public record UsuarioAtualizarRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull Perfil perfil,
            boolean ativo,
            @Size(min = 6, max = 72) String novaSenha) {
    }

    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo, Instant criadoEm) {

        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(), u.getCriadoEm());
        }
    }

    /** Versão resumida usada dentro de outras respostas. */
    public record UsuarioResumo(Long id, String nome) {

        public static UsuarioResumo de(Usuario u) {
            return u == null ? null : new UsuarioResumo(u.getId(), u.getNome());
        }
    }
}
