package br.com.mssolucoes.atelie.seguranca;

import br.com.mssolucoes.atelie.dominio.Enums.Perfil;

public record UsuarioLogado(Long id, String nome, String email, Perfil perfil) {

    public boolean ajudante() {
        return perfil == Perfil.AJUDANTE;
    }
}
