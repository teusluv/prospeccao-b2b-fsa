package br.com.fsa.prospeccao.security;

import br.com.fsa.prospeccao.domain.Perfil;
import br.com.fsa.prospeccao.domain.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Principal do Spring Security, montado a partir de um {@link Usuario}. */
public record UsuarioAutenticado(Long id, String nome, String email, String senhaHash, Perfil perfil, boolean ativo)
        implements UserDetails {

    public static UsuarioAutenticado de(Usuario u) {
        return new UsuarioAutenticado(u.getId(), u.getNome(), u.getEmail(), u.getSenhaHash(), u.getPerfil(), u.isAtivo());
    }

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
