package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    List<Fornecedor> findAllByOrderByAtivoDescNomeAsc();
}
