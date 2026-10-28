package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.OrcamentoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrcamentoFotoRepository extends JpaRepository<OrcamentoFoto, Long> {

    List<OrcamentoFoto> findByOrcamentoIdOrderByIdAsc(Long orcamentoId);
}
