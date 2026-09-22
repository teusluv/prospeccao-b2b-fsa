package br.com.fsa.prospeccao.exception;

/** Erro de validação de regra de negócio, devolvido ao cliente como 422. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
