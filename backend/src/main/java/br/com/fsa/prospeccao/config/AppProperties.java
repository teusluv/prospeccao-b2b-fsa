package br.com.fsa.prospeccao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Admin admin) {

    public record Jwt(String secret, long expiracaoMinutos) {
    }

    public record Cors(List<String> origensPermitidas) {
    }

    public record Admin(String nome, String email, String senha) {
    }
}
