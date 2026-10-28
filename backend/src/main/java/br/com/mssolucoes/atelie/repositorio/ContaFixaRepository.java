package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.ContaFixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContaFixaRepository extends JpaRepository<ContaFixa, Long> {

    List<ContaFixa> findAllByOrderByDiaVencimentoAscNomeAsc();

    List<ContaFixa> findByAtivaTrueOrderByDiaVencimentoAscNomeAsc();
}
