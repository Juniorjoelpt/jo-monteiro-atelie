package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    @Query("""
            select distinct v from Venda v left join fetch v.itens left join fetch v.cliente
            where v.data between :de and :ate order by v.data desc, v.id desc
            """)
    List<Venda> doPeriodo(@Param("de") LocalDate de, @Param("ate") LocalDate ate);

    @Query("select count(i) > 0 from VendaItem i where i.produto.id = :produtoId")
    boolean produtoJaVendido(@Param("produtoId") Long produtoId);

    /** [produtoId, peças] vendidas na pronta-entrega desde a data (vendas não canceladas). */
    @Query("""
            select i.produto.id, sum(i.quantidade) from VendaItem i
            where i.produto is not null and i.venda.status = br.com.mssolucoes.atelie.dominio.Enums.StatusVenda.CONCLUIDA
              and i.venda.data >= :desde
            group by i.produto.id""")
    List<Object[]> pecasPorProduto(@Param("desde") LocalDate desde);
}
