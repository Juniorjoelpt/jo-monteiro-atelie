package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.Compromisso;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CompromissoRepository extends JpaRepository<Compromisso, Long> {

    @EntityGraph(attributePaths = {"cliente", "encomenda"})
    List<Compromisso> findByInicioBetweenOrderByInicioAsc(LocalDateTime de, LocalDateTime ate);

    boolean existsByEncomendaId(Long encomendaId);
}
