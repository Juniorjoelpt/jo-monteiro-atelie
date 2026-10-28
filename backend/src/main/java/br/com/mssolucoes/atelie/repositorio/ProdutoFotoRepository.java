package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.ProdutoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoFotoRepository extends JpaRepository<ProdutoFoto, Long> {
}
