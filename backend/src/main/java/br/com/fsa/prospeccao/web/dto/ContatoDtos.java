package br.com.fsa.prospeccao.web.dto;

import br.com.fsa.prospeccao.domain.Contato;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ContatoDtos {

    private ContatoDtos() {
    }

    public record ContatoRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 100) String cargo,
            @Email @Size(max = 160) String email,
            @Size(max = 30) String telefone,
            @Size(max = 200) String linkedin,
            boolean principal) {
    }

    public record ContatoResponse(Long id, Long empresaId, String nome, String cargo, String email, String telefone,
                                  String linkedin, boolean principal, Instant criadoEm) {

        public static ContatoResponse de(Contato c) {
            return new ContatoResponse(c.getId(), c.getEmpresa().getId(), c.getNome(), c.getCargo(), c.getEmail(),
                    c.getTelefone(), c.getLinkedin(), c.isPrincipal(), c.getCriadoEm());
        }
    }
}
