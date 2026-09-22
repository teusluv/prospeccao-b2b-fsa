package br.com.fsa.prospeccao.web.dto;

import br.com.fsa.prospeccao.domain.EtapaFunil;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalEmpresas,
        List<EtapaResumo> funil,
        BigDecimal valorEmAberto,
        BigDecimal valorGanho,
        double taxaConversao,
        long followUpsPendentes,
        long followUpsAtrasados,
        long interacoesUltimos7Dias) {

    public record EtapaResumo(EtapaFunil etapa, long quantidade, BigDecimal valor) {
    }
}
