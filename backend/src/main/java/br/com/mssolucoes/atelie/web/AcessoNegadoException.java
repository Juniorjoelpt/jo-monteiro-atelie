package br.com.mssolucoes.atelie.web;

/** Ação que o perfil da usuária não permite (vira HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
