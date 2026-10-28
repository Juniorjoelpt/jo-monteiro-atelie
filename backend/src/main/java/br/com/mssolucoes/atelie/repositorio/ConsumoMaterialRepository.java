package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.ConsumoMaterial;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ConsumoMaterialRepository extends JpaRepository<ConsumoMaterial, Long> {

    @EntityGraph(attributePaths = "material")
    List<ConsumoMaterial> findByEncomendaIdOrderByIdAsc(Long encomendaId);

    @Query("select coalesce(sum(c.custoTotal), 0) from ConsumoMaterial c where c.encomenda.id = :encomendaId")
    BigDecimal custoDaEncomenda(@Param("encomendaId") Long encomendaId);

    boolean existsByMaterialId(Long materialId);

    @EntityGraph(attributePaths = "material")
    List<ConsumoMaterial> findByDataBetween(java.time.LocalDate de, java.time.LocalDate ate);

    boolean existsByEncomendaId(Long encomendaId);

    boolean existsByProdutoId(Long produtoId);
}
