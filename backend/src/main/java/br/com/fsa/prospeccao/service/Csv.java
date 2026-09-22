package br.com.fsa.prospeccao.service;

import br.com.fsa.prospeccao.domain.PorteEmpresa;
import br.com.fsa.prospeccao.web.dto.EmpresaDtos.EmpresaRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Leitura simples de CSV (suporta campos entre aspas) usada na importação de empresas. */
final class Csv {

    private Csv() {
    }

    static List<String> dividir(String linha, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                if (entreAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    entreAspas = !entreAspas;
                }
            } else if (c == separador && !entreAspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }

    static EmpresaRequest paraEmpresa(List<String> cabecalho, List<String> valores) {
        return new EmpresaRequest(
                valor(cabecalho, valores, "razaosocial"),
                valor(cabecalho, valores, "nomefantasia"),
                valor(cabecalho, valores, "cnpj"),
                valor(cabecalho, valores, "segmento"),
                porte(valor(cabecalho, valores, "porte")),
                valor(cabecalho, valores, "cidade"),
                valor(cabecalho, valores, "uf"),
                valor(cabecalho, valores, "site"),
                valor(cabecalho, valores, "telefone"),
                valor(cabecalho, valores, "email"),
                valor(cabecalho, valores, "origem"),
                null,
                decimal(valor(cabecalho, valores, "valorestimado")),
                null,
                null);
    }

    private static String valor(List<String> cabecalho, List<String> valores, String coluna) {
        int idx = cabecalho.indexOf(coluna);
        if (idx < 0 || idx >= valores.size()) {
            return null;
        }
        String v = valores.get(idx).trim();
        return v.isEmpty() ? null : v;
    }

    private static PorteEmpresa porte(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return PorteEmpresa.valueOf(valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("porte inválido: " + valor);
        }
    }

    private static BigDecimal decimal(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.contains(",") ? valor.replace(".", "").replace(",", ".") : valor;
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valorEstimado inválido: " + valor);
        }
    }
}
