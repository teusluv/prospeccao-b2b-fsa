package br.com.fsa.prospeccao.config;

import br.com.fsa.prospeccao.domain.Perfil;
import br.com.fsa.prospeccao.domain.Usuario;
import br.com.fsa.prospeccao.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria o primeiro administrador quando o banco ainda não tem nenhum usuário. */
@Component
public class AdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public AdminInicialRunner(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, AppProperties props) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNome(props.admin().nome());
        admin.setEmail(props.admin().email().toLowerCase());
        admin.setSenhaHash(passwordEncoder.encode(props.admin().senha()));
        admin.setPerfil(Perfil.ADMIN);
        usuarios.save(admin);
        log.warn("Usuário administrador inicial criado: {} (troque a senha padrão)", admin.getEmail());
    }
}
