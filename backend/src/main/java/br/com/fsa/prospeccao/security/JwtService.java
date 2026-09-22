package br.com.fsa.prospeccao.security;

import br.com.fsa.prospeccao.config.AppProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(AppProperties props) {
        byte[] segredo = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (segredo.length < 32) {
            throw new IllegalStateException("app.jwt.secret precisa ter pelo menos 32 bytes");
        }
        this.chave = Keys.hmacShaKeyFor(segredo);
        this.expiracaoMinutos = props.jwt().expiracaoMinutos();
    }

    public TokenGerado gerar(UsuarioAutenticado usuario) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(expiracaoMinutos, ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .subject(usuario.email())
                .claim("uid", usuario.id())
                .claim("perfil", usuario.perfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expira))
                .signWith(chave)
                .compact();
        return new TokenGerado(token, expira);
    }

    /** Devolve o e-mail (subject) se o token for válido e não estiver expirado. */
    public Optional<String> validar(String token) {
        try {
            return Optional.ofNullable(
                    Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload().getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record TokenGerado(String token, Instant expiraEm) {
    }
}
