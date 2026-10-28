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
}
