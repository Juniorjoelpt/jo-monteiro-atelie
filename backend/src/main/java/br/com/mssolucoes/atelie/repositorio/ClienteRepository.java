package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findFirstByWhatsapp(String whatsapp);

    List<Cliente> findByAniversarioIsNotNull();

    @Query("""
            select c from Cliente c
            where (:busca is null or lower(c.nome) like lower(concat('%', :busca, '%'))
                   or c.whatsapp like concat('%', :busca, '%'))
            order by c.nome
            """)
    List<Cliente> buscar(@Param("busca") String busca);
}
