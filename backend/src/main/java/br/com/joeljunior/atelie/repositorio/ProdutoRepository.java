package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @Query("select distinct p from Produto p left join fetch p.materiais pm left join fetch pm.material order by p.nome")
    List<Produto> todosComMateriais();

    boolean existsByMateriaisMaterialId(Long materialId);
}
