package br.com.fsa.prospeccao.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Lê o header "Authorization: Bearer <token>" e autentica a requisição. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UsuarioDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.validar(header.substring(7)).ifPresent(email -> autenticar(email, request));
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String email, HttpServletRequest request) {
        try {
            UsuarioAutenticado usuario = userDetailsService.loadUserByUsername(email);
            if (!usuario.isEnabled()) {
                return;
            }
            var auth = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException ignorado) {
            // usuário removido depois da emissão do token: segue sem autenticação
        }
    }
}
