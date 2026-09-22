package br.com.fsa.prospeccao.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProspeccaoServiceTest {

    @Test
    void redeSocialNaoContaComoSite() {
        assertThat(ProspeccaoService.temSite(null, true)).isFalse();
        assertThat(ProspeccaoService.temSite("", true)).isFalse();
        assertThat(ProspeccaoService.temSite("https://www.instagram.com/padaria", true)).isFalse();
        assertThat(ProspeccaoService.temSite("http://m.facebook.com/loja", true)).isFalse();
        assertThat(ProspeccaoService.temSite("https://wa.me/5575999999999", true)).isFalse();
        assertThat(ProspeccaoService.temSite("https://www.padariasol.com.br", true)).isTrue();
        assertThat(ProspeccaoService.temSite("https://www.instagram.com/padaria", false)).isTrue();
    }
}
