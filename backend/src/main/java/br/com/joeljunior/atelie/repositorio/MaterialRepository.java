package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findAllByOrderByNomeAsc();
}
