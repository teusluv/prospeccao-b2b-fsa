package br.com.fsa.prospeccao.repository;

import br.com.fsa.prospeccao.domain.Interacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface InteracaoRepository extends JpaRepository<Interacao, Long> {

    @EntityGraph(attributePaths = {"contato", "usuario", "empresa"})
    Page<Interacao> findByEmpresaIdOrderByDataHoraDesc(Long empresaId, Pageable pageable);

    @EntityGraph(attributePaths = {"contato", "usuario", "empresa"})
    List<Interacao> findByFollowUpConcluidoFalseAndDataFollowUpLessThanEqualOrderByDataFollowUpAsc(Instant ate);

    @EntityGraph(attributePaths = {"contato", "usuario", "empresa"})
    List<Interacao> findByFollowUpConcluidoFalseAndDataFollowUpLessThanEqualAndUsuarioIdOrderByDataFollowUpAsc(
            Instant ate, Long usuarioId);

    @Query("select count(i) from Interacao i where i.followUpConcluido = false and i.dataFollowUp is not null "
            + "and i.dataFollowUp < :agora")
    long contarFollowUpsAtrasados(Instant agora);

    @Query("select count(i) from Interacao i where i.followUpConcluido = false and i.dataFollowUp is not null")
    long contarFollowUpsPendentes();

    long countByDataHoraAfter(Instant desde);
}
