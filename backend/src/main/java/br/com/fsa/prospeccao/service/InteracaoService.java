package br.com.fsa.prospeccao.service;

import br.com.fsa.prospeccao.domain.Contato;
import br.com.fsa.prospeccao.domain.Empresa;
import br.com.fsa.prospeccao.domain.EtapaFunil;
import br.com.fsa.prospeccao.domain.Interacao;
import br.com.fsa.prospeccao.exception.RecursoNaoEncontradoException;
import br.com.fsa.prospeccao.exception.RegraNegocioException;
import br.com.fsa.prospeccao.repository.ContatoRepository;
import br.com.fsa.prospeccao.repository.InteracaoRepository;
import br.com.fsa.prospeccao.repository.UsuarioRepository;
import br.com.fsa.prospeccao.security.UsuarioAutenticado;
import br.com.fsa.prospeccao.web.dto.InteracaoDtos.InteracaoRequest;
import br.com.fsa.prospeccao.web.dto.InteracaoDtos.InteracaoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class InteracaoService {

    private final InteracaoRepository interacoes;
    private final ContatoRepository contatos;
    private final UsuarioRepository usuarios;
    private final EmpresaService empresaService;

    public InteracaoService(InteracaoRepository interacoes, ContatoRepository contatos, UsuarioRepository usuarios,
                            EmpresaService empresaService) {
        this.interacoes = interacoes;
        this.contatos = contatos;
        this.usuarios = usuarios;
        this.empresaService = empresaService;
    }

    @Transactional(readOnly = true)
    public Page<InteracaoResponse> listarDaEmpresa(Long empresaId, Pageable pageable) {
        empresaService.buscarEntidade(empresaId);
        return interacoes.findByEmpresaIdOrderByDataHoraDesc(empresaId, pageable).map(InteracaoResponse::de);
    }

    @Transactional(readOnly = true)
    public List<InteracaoResponse> followUps(Instant ate, Long usuarioId) {
        List<Interacao> lista = usuarioId == null
                ? interacoes.findByFollowUpConcluidoFalseAndDataFollowUpLessThanEqualOrderByDataFollowUpAsc(ate)
                : interacoes.findByFollowUpConcluidoFalseAndDataFollowUpLessThanEqualAndUsuarioIdOrderByDataFollowUpAsc(
                        ate, usuarioId);
        return lista.stream().map(InteracaoResponse::de).toList();
    }

    /**
     * Registra uma interação. Um lead ainda NOVO passa automaticamente para CONTATADO
     * no primeiro contato registrado.
     */
    public InteracaoResponse registrar(Long empresaId, InteracaoRequest req, UsuarioAutenticado usuario) {
        Empresa empresa = empresaService.buscarEntidade(empresaId);
        Interacao i = new Interacao();
        i.setEmpresa(empresa);
        i.setUsuario(usuarios.getReferenceById(usuario.id()));
        i.setTipo(req.tipo());
        i.setDescricao(req.descricao().trim());
        i.setDataHora(req.dataHora());
        i.setProximoPasso(req.proximoPasso());
        i.setDataFollowUp(req.dataFollowUp());
        if (req.contatoId() != null) {
            Contato contato = contatos.findByIdAndEmpresaId(req.contatoId(), empresaId)
                    .orElseThrow(() -> new RegraNegocioException("Contato " + req.contatoId() + " não pertence a esta empresa"));
            i.setContato(contato);
        }
        if (empresa.getEtapa() == EtapaFunil.NOVO) {
            empresa.setEtapa(EtapaFunil.CONTATADO);
        }
        return InteracaoResponse.de(interacoes.save(i));
    }

    public InteracaoResponse concluirFollowUp(Long id) {
        Interacao i = buscar(id);
        if (i.getDataFollowUp() == null) {
            throw new RegraNegocioException("Esta interação não tem follow-up agendado");
        }
        i.setFollowUpConcluido(true);
        return InteracaoResponse.de(i);
    }

    public void excluir(Long id, UsuarioAutenticado usuario) {
        Interacao i = buscar(id);
        if (!usuario.isAdmin() && !i.getUsuario().getId().equals(usuario.id())) {
            throw new AccessDeniedException("Só o autor ou um administrador pode excluir a interação");
        }
        interacoes.delete(i);
    }

    private Interacao buscar(Long id) {
        return interacoes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Interação", id));
    }
}
