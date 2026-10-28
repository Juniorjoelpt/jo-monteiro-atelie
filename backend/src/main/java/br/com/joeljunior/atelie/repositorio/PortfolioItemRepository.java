package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findAllByOrderByOrdemAscIdDesc();

    List<PortfolioItem> findByPublicadoTrueOrderByOrdemAscIdDesc();
}
