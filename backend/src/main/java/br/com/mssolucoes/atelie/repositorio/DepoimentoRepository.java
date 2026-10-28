package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Depoimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepoimentoRepository extends JpaRepository<Depoimento, Long> {

    List<Depoimento> findAllByOrderByOrdemAscIdDesc();

    List<Depoimento> findByPublicadoTrueOrderByOrdemAscIdDesc();
}
