package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, Long> {
}
