package br.com.fsa.prospeccao.service;

import br.com.fsa.prospeccao.domain.EtapaFunil;
import br.com.fsa.prospeccao.repository.EmpresaRepository;
import br.com.fsa.prospeccao.repository.InteracaoRepository;
import br.com.fsa.prospeccao.web.dto.DashboardResponse;
import br.com.fsa.prospeccao.web.dto.DashboardResponse.EtapaResumo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final EmpresaRepository empresas;
    private final InteracaoRepository interacoes;

    public DashboardService(EmpresaRepository empresas, InteracaoRepository interacoes) {
        this.empresas = empresas;
        this.interacoes = interacoes;
    }

    public DashboardResponse resumo() {
        Map<EtapaFunil, EtapaResumo> porEtapa = new EnumMap<>(EtapaFunil.class);
        for (EtapaFunil etapa : EtapaFunil.values()) {
            porEtapa.put(etapa, new EtapaResumo(etapa, 0, BigDecimal.ZERO));
        }
        empresas.resumoPorEtapa().forEach(r ->
                porEtapa.put(r.getEtapa(), new EtapaResumo(r.getEtapa(), r.getQuantidade(), r.getValor())));

        long total = porEtapa.values().stream().mapToLong(EtapaResumo::quantidade).sum();
        long ganhos = porEtapa.get(EtapaFunil.GANHO).quantidade();
        long perdidos = porEtapa.get(EtapaFunil.PERDIDO).quantidade();
        double conversao = ganhos + perdidos == 0 ? 0 : (double) ganhos / (ganhos + perdidos);

        List<EtapaFunil> abertas = Arrays.stream(EtapaFunil.values()).filter(e -> !e.isFinalizada()).toList();
        Instant agora = Instant.now();
        return new DashboardResponse(
                total,
                List.copyOf(porEtapa.values()),
                empresas.somarValorPorEtapas(abertas),
                porEtapa.get(EtapaFunil.GANHO).valor(),
                Math.round(conversao * 10000) / 10000.0,
                interacoes.contarFollowUpsPendentes(),
                interacoes.contarFollowUpsAtrasados(agora),
                interacoes.countByDataHoraAfter(agora.minus(7, ChronoUnit.DAYS)));
    }
}
