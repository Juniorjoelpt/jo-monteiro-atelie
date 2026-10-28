package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.Encomenda;
import br.com.joeljunior.atelie.dominio.Enums.StatusArte;
import br.com.joeljunior.atelie.dominio.Enums.StatusEncomenda;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface EncomendaRepository extends JpaRepository<Encomenda, Long> {

    @EntityGraph(attributePaths = "cliente")
    List<Encomenda> findAllByOrderByCriadoEmDesc();

    @EntityGraph(attributePaths = "cliente")
    List<Encomenda> findByStatusInOrderByPrazoEntregaAsc(Collection<StatusEncomenda> status);

    @EntityGraph(attributePaths = "cliente")
    List<Encomenda> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    long countByStatusIn(Collection<StatusEncomenda> status);

    long countByStatusInAndStatusArteIn(Collection<StatusEncomenda> status, Collection<StatusArte> arte);

    java.util.Optional<Encomenda> findByCodigoArte(String codigoArte);

    /** [produtoId, peças] das encomendas não canceladas criadas desde a data. */
    @org.springframework.data.jpa.repository.Query("""
            select e.produto.id, sum(e.quantidade) from Encomenda e
            where e.produto is not null and e.status <> br.com.joeljunior.atelie.dominio.Enums.StatusEncomenda.CANCELADA
              and e.criadoEm >= :desde
            group by e.produto.id""")
    List<Object[]> pecasPorProduto(@org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde);

    @EntityGraph(attributePaths = "cliente")
    List<Encomenda> findByStatusInAndStatusArte(Collection<StatusEncomenda> status, StatusArte arte);

    long countByStatusInAndPrazoEntregaBetween(Collection<StatusEncomenda> status, LocalDate de, LocalDate ate);

    boolean existsByOrcamentoId(Long orcamentoId);

    java.util.Optional<Encomenda> findFirstByOrcamentoId(Long orcamentoId);

    boolean existsByProdutoId(Long produtoId);

    @EntityGraph(attributePaths = "cliente")
    List<Encomenda> findByPrazoEntregaBetweenOrderByPrazoEntregaAsc(LocalDate de, LocalDate ate);

    @EntityGraph(attributePaths = {"cliente", "produto"})
    List<Encomenda> findByStatusAndEntregueEmBetween(StatusEncomenda status, LocalDate de, LocalDate ate);

    @EntityGraph(attributePaths = "cliente")
    List<Encomenda> findByStatusIn(Collection<StatusEncomenda> status);
}
