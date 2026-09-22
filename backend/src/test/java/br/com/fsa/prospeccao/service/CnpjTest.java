package br.com.fsa.prospeccao.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjTest {

    @Test
    void normalizaEValida() {
        assertThat(Cnpj.normalizar("11.222.333/0001-81")).isEqualTo("11222333000181");
        assertThat(Cnpj.valido("11222333000181")).isTrue();
        assertThat(Cnpj.valido("11222333000182")).isFalse();
        assertThat(Cnpj.valido("11111111111111")).isFalse();
        assertThat(Cnpj.valido("123")).isFalse();
        assertThat(Cnpj.normalizar("  ")).isNull();
    }
}
