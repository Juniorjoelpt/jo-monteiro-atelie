package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.ArteVersao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArteVersaoRepository extends JpaRepository<ArteVersao, Long> {

    List<ArteVersao> findByEncomendaIdOrderByNumeroDesc(Long encomendaId);

    Optional<ArteVersao> findFirstByEncomendaIdOrderByNumeroDesc(Long encomendaId);

    long countByEncomendaId(Long encomendaId);
}
