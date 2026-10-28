package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.EncomendaFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EncomendaFotoRepository extends JpaRepository<EncomendaFoto, Long> {

    List<EncomendaFoto> findByEncomendaIdOrderByIdAsc(Long encomendaId);

    long countByEncomendaId(Long encomendaId);
}
