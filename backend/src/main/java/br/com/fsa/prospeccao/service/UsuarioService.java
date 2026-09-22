package br.com.fsa.prospeccao.service;

import br.com.fsa.prospeccao.domain.Usuario;
import br.com.fsa.prospeccao.exception.RecursoNaoEncontradoException;
import br.com.fsa.prospeccao.exception.RegraNegocioException;
import br.com.fsa.prospeccao.repository.UsuarioRepository;
import br.com.fsa.prospeccao.web.dto.UsuarioDtos.UsuarioAtualizarRequest;
import br.com.fsa.prospeccao.web.dto.UsuarioDtos.UsuarioCriarRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findAll(Sort.by("nome"));
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    public Usuario criar(UsuarioCriarRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe um usuário com o e-mail " + email);
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(passwordEncoder.encode(req.senha()));
        u.setPerfil(req.perfil());
        return usuarios.save(u);
    }

    public Usuario atualizar(Long id, UsuarioAtualizarRequest req) {
        Usuario u = buscar(id);
        u.setNome(req.nome().trim());
        u.setPerfil(req.perfil());
        u.setAtivo(req.ativo());
        if (req.novaSenha() != null && !req.novaSenha().isBlank()) {
            u.setSenhaHash(passwordEncoder.encode(req.novaSenha()));
        }
        return u;
    }

    public void trocarSenha(Long id, String senhaAtual, String novaSenha) {
        Usuario u = buscar(id);
        if (!passwordEncoder.matches(senhaAtual, u.getSenhaHash())) {
            throw new RegraNegocioException("Senha atual incorreta");
        }
        u.setSenhaHash(passwordEncoder.encode(novaSenha));
    }
}
