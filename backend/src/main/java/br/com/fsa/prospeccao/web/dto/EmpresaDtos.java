package br.com.fsa.prospeccao.web.dto;

import br.com.fsa.prospeccao.domain.Empresa;
import br.com.fsa.prospeccao.domain.EtapaFunil;
import br.com.fsa.prospeccao.domain.PorteEmpresa;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class EmpresaDtos {

    private EmpresaDtos() {
    }

    public record EmpresaRequest(
            @NotBlank @Size(max = 200) String razaoSocial,
            @Size(max = 200) String nomeFantasia,
            @Size(max = 18) String cnpj,
            @Size(max = 100) String segmento,
            PorteEmpresa porte,
            @Size(max = 100) String cidade,
            @Pattern(regexp = "^[A-Za-z]{2}$", message = "deve ser a sigla do estado (ex.: BA)") String uf,
            @Size(max = 200) String site,
            @Size(max = 30) String telefone,
            @Email @Size(max = 160) String email,
            @Size(max = 100) String origem,
            EtapaFunil etapa,
            @DecimalMin("0.00") BigDecimal valorEstimado,
            @Size(max = 4000) String observacoes,
            Long responsavelId) {
    }

    public record MudarEtapaRequest(@NotNull EtapaFunil etapa, @Size(max = 500) String motivoPerda) {
    }

    public record EmpresaResponse(
            Long id,
            String razaoSocial,
            String nomeFantasia,
            String cnpj,
            String segmento,
            PorteEmpresa porte,
            String cidade,
            String uf,
            String site,
            String telefone,
            String email,
            String origem,
            EtapaFunil etapa,
            BigDecimal valorEstimado,
            String motivoPerda,
            String observacoes,
            UsuarioDtos.UsuarioResumo responsavel,
            Instant criadoEm,
            Instant atualizadoEm) {

        public static EmpresaResponse de(Empresa e) {
            return new EmpresaResponse(e.getId(), e.getRazaoSocial(), e.getNomeFantasia(), e.getCnpj(),
                    e.getSegmento(), e.getPorte(), e.getCidade(), e.getUf(), e.getSite(), e.getTelefone(),
                    e.getEmail(), e.getOrigem(), e.getEtapa(), e.getValorEstimado(), e.getMotivoPerda(),
                    e.getObservacoes(), UsuarioDtos.UsuarioResumo.de(e.getResponsavel()), e.getCriadoEm(),
                    e.getAtualizadoEm());
        }
    }

    public record ImportacaoResponse(int importadas, int ignoradas, List<String> erros) {
    }
}
