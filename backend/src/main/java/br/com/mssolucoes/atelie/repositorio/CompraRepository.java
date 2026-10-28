package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Compra;
import br.com.mssolucoes.atelie.dominio.Enums.StatusCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findByDataBetweenOrderByDataDescIdDesc(LocalDate de, LocalDate ate);

    List<Compra> findByStatus(StatusCompra status);

    List<Compra> findByFornecedorIdOrderByDataDescIdDesc(Long fornecedorId);

    boolean existsByFornecedorId(Long fornecedorId);
}
