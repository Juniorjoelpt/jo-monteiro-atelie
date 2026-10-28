package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.EncomendaFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EncomendaFotoRepository extends JpaRepository<EncomendaFoto, Long> {

    List<EncomendaFoto> findByEncomendaIdOrderByIdAsc(Long encomendaId);

    long countByEncomendaId(Long encomendaId);
}
