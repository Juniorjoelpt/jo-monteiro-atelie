package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Enums.TipoLancamento;
import br.com.mssolucoes.atelie.dominio.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    List<Lancamento> findByDataBetweenOrderByDataDescIdDesc(LocalDate de, LocalDate ate);

    List<Lancamento> findByDataBetween(LocalDate de, LocalDate ate);

    List<Lancamento> findByEncomendaIdOrderByDataAsc(Long encomendaId);

    List<Lancamento> findByTipoAndDataBetween(TipoLancamento tipo, LocalDate de, LocalDate ate);

    void deleteByVendaId(Long vendaId);

    void deleteByCompraId(Long compraId);

    boolean existsByCompraId(Long compraId);

    boolean existsByContaFixaId(Long contaFixaId);

    List<Lancamento> findByContaFixaIsNotNullAndCompetenciaIn(java.util.Collection<LocalDate> competencias);

    java.util.Optional<Lancamento> findByContaFixaIdAndCompetencia(Long contaFixaId, LocalDate competencia);

    @Query("select coalesce(sum(l.valor), 0) from Lancamento l where l.encomenda.id = :encomendaId and l.tipo = :tipo")
    BigDecimal somaPorEncomenda(@Param("encomendaId") Long encomendaId, @Param("tipo") TipoLancamento tipo);

    default BigDecimal totalPagoEncomenda(Long encomendaId) {
        return somaPorEncomenda(encomendaId, TipoLancamento.ENTRADA);
    }
}
