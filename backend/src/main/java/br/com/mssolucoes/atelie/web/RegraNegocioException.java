package br.com.mssolucoes.atelie.web;

/** Erro de regra de negócio, devolvido ao usuário com status 400 e a mensagem. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
