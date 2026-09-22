package br.com.fsa.prospeccao.web.dto;

import br.com.fsa.prospeccao.domain.Interacao;
import br.com.fsa.prospeccao.domain.TipoInteracao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class InteracaoDtos {

    private InteracaoDtos() {
    }

    public record InteracaoRequest(
            @NotNull TipoInteracao tipo,
            @NotBlank @Size(max = 4000) String descricao,
            Instant dataHora,
            Long contatoId,
            @Size(max = 500) String proximoPasso,
            Instant dataFollowUp) {
    }

    public record InteracaoResponse(
            Long id,
            Long empresaId,
            String empresaNome,
            Long contatoId,
            String contatoNome,
            UsuarioDtos.UsuarioResumo usuario,
            TipoInteracao tipo,
            String descricao,
            Instant dataHora,
            String proximoPasso,
            Instant dataFollowUp,
            boolean followUpConcluido,
            Instant criadoEm) {

        public static InteracaoResponse de(Interacao i) {
            var empresa = i.getEmpresa();
            var contato = i.getContato();
            String nomeEmpresa = empresa.getNomeFantasia() != null ? empresa.getNomeFantasia() : empresa.getRazaoSocial();
            return new InteracaoResponse(i.getId(), empresa.getId(), nomeEmpresa,
                    contato == null ? null : contato.getId(), contato == null ? null : contato.getNome(),
                    UsuarioDtos.UsuarioResumo.de(i.getUsuario()), i.getTipo(), i.getDescricao(), i.getDataHora(),
                    i.getProximoPasso(), i.getDataFollowUp(), i.isFollowUpConcluido(), i.getCriadoEm());
        }
    }
}
