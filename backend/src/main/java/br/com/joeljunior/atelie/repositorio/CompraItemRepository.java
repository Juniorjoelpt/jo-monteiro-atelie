package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.CompraItem;
import br.com.joeljunior.atelie.dominio.Enums.StatusCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompraItemRepository extends JpaRepository<CompraItem, Long> {

    boolean existsByMaterialId(Long materialId);

    /** Compras do material, da mais recente para a mais antiga. */
    @Query("select i from CompraItem i join fetch i.compra c left join fetch c.fornecedor "
            + "where i.material.id = :materialId and c.status = :status order by c.data desc, c.id desc, i.id desc")
    List<CompraItem> historico(@Param("materialId") Long materialId, @Param("status") StatusCompra status);
}
