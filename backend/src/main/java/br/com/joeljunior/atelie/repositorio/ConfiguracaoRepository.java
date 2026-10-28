package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, Long> {
}
