package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.OrcamentoItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrcamentoItemRepository extends JpaRepository<OrcamentoItem, Long> {

    @EntityGraph(attributePaths = "produto")
    List<OrcamentoItem> findByOrcamentoIdOrderByIdAsc(Long orcamentoId);

    void deleteByOrcamentoId(Long orcamentoId);

    boolean existsByProdutoId(Long produtoId);
}
