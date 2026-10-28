package br.com.joeljunior.atelie.web;

public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String oQue) {
        super(oQue + " não encontrado(a).");
    }
}
