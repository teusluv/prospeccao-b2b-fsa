package br.com.fsa.prospeccao.repository;

import br.com.fsa.prospeccao.domain.Empresa;
import br.com.fsa.prospeccao.domain.EtapaFunil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface EmpresaRepository extends JpaRepository<Empresa, Long>, JpaSpecificationExecutor<Empresa> {

    @Override
    @EntityGraph(attributePaths = "responsavel")
    Page<Empresa> findAll(Specification<Empresa> spec, Pageable pageable);

    boolean existsByCnpj(String cnpj);

    boolean existsByGooglePlaceId(String googlePlaceId);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    @Query("select e.etapa as etapa, count(e) as quantidade, coalesce(sum(e.valorEstimado), 0) as valor "
            + "from Empresa e group by e.etapa")
    List<ResumoEtapa> resumoPorEtapa();

    @Query("select coalesce(sum(e.valorEstimado), 0) from Empresa e where e.etapa in :etapas")
    BigDecimal somarValorPorEtapas(Collection<EtapaFunil> etapas);

    interface ResumoEtapa {
        EtapaFunil getEtapa();
        long getQuantidade();
        BigDecimal getValor();
    }
}
