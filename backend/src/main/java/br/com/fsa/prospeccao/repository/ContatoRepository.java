package br.com.fsa.prospeccao.repository;

import br.com.fsa.prospeccao.domain.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContatoRepository extends JpaRepository<Contato, Long> {

    List<Contato> findByEmpresaIdOrderByPrincipalDescNomeAsc(Long empresaId);

    Optional<Contato> findByIdAndEmpresaId(Long id, Long empresaId);

    @Modifying
    @Query("update Contato c set c.principal = false where c.empresa.id = :empresaId and c.id <> :exceto")
    void desmarcarPrincipais(Long empresaId, Long exceto);
}
