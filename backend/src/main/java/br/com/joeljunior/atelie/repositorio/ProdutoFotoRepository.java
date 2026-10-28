package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.ProdutoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoFotoRepository extends JpaRepository<ProdutoFoto, Long> {
}
