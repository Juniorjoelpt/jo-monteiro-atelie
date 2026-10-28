package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Enums.StatusOrcamento;
import br.com.mssolucoes.atelie.dominio.Orcamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    @EntityGraph(attributePaths = "cliente")
    List<Orcamento> findAllByOrderByCriadoEmDesc();

    @EntityGraph(attributePaths = "cliente")
    List<Orcamento> findByStatusInOrderByCriadoEmDesc(Collection<StatusOrcamento> status);

    @EntityGraph(attributePaths = "cliente")
    List<Orcamento> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    long countByStatusIn(Collection<StatusOrcamento> status);
}
