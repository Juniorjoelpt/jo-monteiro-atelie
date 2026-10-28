package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    List<Fornecedor> findAllByOrderByAtivoDescNomeAsc();
}
