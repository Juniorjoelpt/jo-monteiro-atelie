package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findAllByOrderByOrdemAscIdDesc();

    List<PortfolioItem> findByPublicadoTrueOrderByOrdemAscIdDesc();
}
