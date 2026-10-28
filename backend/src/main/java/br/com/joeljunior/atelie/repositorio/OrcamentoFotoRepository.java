package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.OrcamentoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrcamentoFotoRepository extends JpaRepository<OrcamentoFoto, Long> {

    List<OrcamentoFoto> findByOrcamentoIdOrderByIdAsc(Long orcamentoId);
}
