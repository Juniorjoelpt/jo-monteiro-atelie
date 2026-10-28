package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.Enums.AreaHistorico;
import br.com.joeljunior.atelie.dominio.Historico;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoricoRepository extends JpaRepository<Historico, Long> {

    List<Historico> findByEncomendaIdOrderByCriadoEmDescIdDesc(Long encomendaId);

    @Query("""
            select h from Historico h left join fetch h.encomenda e
            where h.criadoEm >= :de and h.criadoEm < :ate
              and (:area is null or h.area = :area)
              and (:usuario is null or h.usuarioId = :usuario)
              and (:soCliente = false or h.usuarioId is null)
            order by h.criadoEm desc, h.id desc""")
    List<Historico> buscar(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate,
                           @Param("area") AreaHistorico area, @Param("usuario") Long usuario,
                           @Param("soCliente") boolean soCliente, Pageable pagina);
}
