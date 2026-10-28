package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.ContaFixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContaFixaRepository extends JpaRepository<ContaFixa, Long> {

    List<ContaFixa> findAllByOrderByDiaVencimentoAscNomeAsc();

    List<ContaFixa> findByAtivaTrueOrderByDiaVencimentoAscNomeAsc();
}
