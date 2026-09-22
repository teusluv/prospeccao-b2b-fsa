package br.com.fsa.prospeccao.service;

import br.com.fsa.prospeccao.domain.Contato;
import br.com.fsa.prospeccao.domain.Empresa;
import br.com.fsa.prospeccao.exception.RecursoNaoEncontradoException;
import br.com.fsa.prospeccao.repository.ContatoRepository;
import br.com.fsa.prospeccao.web.dto.ContatoDtos.ContatoRequest;
import br.com.fsa.prospeccao.web.dto.ContatoDtos.ContatoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContatoService {

    private final ContatoRepository contatos;
    private final EmpresaService empresaService;

    public ContatoService(ContatoRepository contatos, EmpresaService empresaService) {
        this.contatos = contatos;
        this.empresaService = empresaService;
    }

    @Transactional(readOnly = true)
    public List<ContatoResponse> listarDaEmpresa(Long empresaId) {
        empresaService.buscarEntidade(empresaId);
        return contatos.findByEmpresaIdOrderByPrincipalDescNomeAsc(empresaId).stream().map(ContatoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ContatoResponse buscar(Long id) {
        return ContatoResponse.de(buscarEntidade(id));
    }

    public ContatoResponse criar(Long empresaId, ContatoRequest req) {
        Empresa empresa = empresaService.buscarEntidade(empresaId);
        Contato c = new Contato();
        c.setEmpresa(empresa);
        aplicar(c, req);
        contatos.save(c);
        garantirUnicoPrincipal(c);
        return ContatoResponse.de(c);
    }

    public ContatoResponse atualizar(Long id, ContatoRequest req) {
        Contato c = buscarEntidade(id);
        aplicar(c, req);
        garantirUnicoPrincipal(c);
        return ContatoResponse.de(c);
    }

    public void excluir(Long id) {
        contatos.delete(buscarEntidade(id));
    }

    private Contato buscarEntidade(Long id) {
        return contatos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Contato", id));
    }

    private void garantirUnicoPrincipal(Contato c) {
        if (c.isPrincipal()) {
            contatos.flush();
            contatos.desmarcarPrincipais(c.getEmpresa().getId(), c.getId());
        }
    }

    private static void aplicar(Contato c, ContatoRequest req) {
        c.setNome(req.nome().trim());
        c.setCargo(req.cargo());
        c.setEmail(req.email());
        c.setTelefone(req.telefone());
        c.setLinkedin(req.linkedin());
        c.setPrincipal(req.principal());
    }
}
