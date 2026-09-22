package br.com.fsa.prospeccao.service;

/** Normalização e validação de CNPJ (dígitos verificadores). */
public final class Cnpj {

    private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() {
    }

    /** Remove pontuação; devolve null se o valor estiver vazio. */
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    public static boolean valido(String digitos) {
        if (digitos == null || !digitos.matches("\\d{14}") || digitos.chars().distinct().count() == 1) {
            return false;
        }
        return digito(digitos, PESOS_1) == digitos.charAt(12) - '0'
                && digito(digitos, PESOS_2) == digitos.charAt(13) - '0';
    }

    private static int digito(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digitos.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
