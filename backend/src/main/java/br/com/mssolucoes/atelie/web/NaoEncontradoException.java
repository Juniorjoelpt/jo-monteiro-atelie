package br.com.mssolucoes.atelie.web;

public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String oQue) {
        super(oQue + " não encontrado(a).");
    }
}
