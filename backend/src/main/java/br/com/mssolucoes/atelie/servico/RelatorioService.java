package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.*;
import br.com.mssolucoes.atelie.dominio.Enums.*;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.*;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Números do período escolhido, comparados com o período anterior de mesmo tamanho. */
@Service
public class RelatorioService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String SEM_FICHA = "Peças únicas (sem ficha)";

    private final LancamentoRepository lancamentos;
    private final EncomendaRepository encomendas;
    private final EncomendaService encomendaService;
    private final VendaRepository vendas;
    private final ConsumoMaterialRepository consumos;

    public RelatorioService(LancamentoRepository lancamentos, EncomendaRepository encomendas,
                            EncomendaService encomendaService, VendaRepository vendas,
                            ConsumoMaterialRepository consumos) {
        this.lancamentos = lancamentos;
        this.encomendas = encomendas;
        this.encomendaService = encomendaService;
        this.vendas = vendas;
        this.consumos = consumos;
    }

    @Transactional(readOnly = true)
    public Relatorio gerar(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new RegraNegocioException("O período está invertido.");
        }
        long dias = ChronoUnit.DAYS.between(de, ate) + 1;
        if (dias > 731) {
            throw new RegraNegocioException("Escolha um período de até 2 anos.");
        }
        // Período anterior: o mesmo trecho do mês/ano anterior quando começa no dia 1
        // (01 a 23/10 compara com 01 a 23/09); senão, os mesmos tantos dias logo antes.
        LocalDate deAnt;
        LocalDate ateAnt;
        if (de.getDayOfYear() == 1 && de.getYear() == ate.getYear()) {
            deAnt = de.minusYears(1);
            ateAnt = ate.minusYears(1);
        } else if (de.getDayOfMonth() == 1) {
            long meses = ChronoUnit.MONTHS.between(YearMonth.from(de), YearMonth.from(ate)) + 1;
            deAnt = de.minusMonths(meses);
            ateAnt = ate.equals(YearMonth.from(ate).atEndOfMonth())
                    ? YearMonth.from(ate).minusMonths(meses).atEndOfMonth() : ate.minusMonths(meses);
        } else {
            ateAnt = de.minusDays(1);
            deAnt = ateAnt.minusDays(dias - 1);
        }

        List<Lancamento> lancs = lancamentos.findByDataBetweenOrderByDataDescIdDesc(de, ate);
        List<Lancamento> lancsAnt = lancamentos.findByDataBetween(deAnt, ateAnt);
        BigDecimal ent = FinanceiroService.soma(lancs, TipoLancamento.ENTRADA);
        BigDecimal sai = FinanceiroService.soma(lancs, TipoLancamento.SAIDA);
        BigDecimal entAnt = FinanceiroService.soma(lancsAnt, TipoLancamento.ENTRADA);
        BigDecimal saiAnt = FinanceiroService.soma(lancsAnt, TipoLancamento.SAIDA);

        // Entradas por frente
        Map<CategoriaLancamento, BigDecimal> porCat = new EnumMap<>(CategoriaLancamento.class);
        lancs.stream().filter(l -> l.getTipo() == TipoLancamento.ENTRADA)
                .forEach(l -> porCat.merge(l.getCategoria(), l.getValor(), BigDecimal::add));

        // Encomendas entregues, agrupadas pela ficha
        Map<String, Acumulado> porPeca = new LinkedHashMap<>();
        BigDecimal faturamentoEncomendas = BigDecimal.ZERO;
        List<Encomenda> entregues = encomendas.findByStatusAndEntregueEmBetween(StatusEncomenda.ENTREGUE, de, ate);
        Map<String, int[]> ranking = new HashMap<>();
        Map<String, BigDecimal> rankingValor = new HashMap<>();
        for (Encomenda e : entregues) {
            EncomendaResp r = encomendaService.paraResp(e);
            String nome = r.produtoNome() != null ? r.produtoNome() : SEM_FICHA;
            Acumulado a = porPeca.computeIfAbsent(nome, k -> new Acumulado());
            a.pecas += r.quantidade();
            a.faturamento = a.faturamento.add(r.valorTotal());
            a.materiais = a.materiais.add(r.custos().custoMateriais());
            a.ganho = a.ganho.add(r.custos().ganho());
            a.horas = a.horas.add(r.custos().horas());
            faturamentoEncomendas = faturamentoEncomendas.add(r.valorTotal());
            if (r.produtoNome() != null) {
                ranking.computeIfAbsent(nome, k -> new int[2])[0] += r.quantidade();
                rankingValor.merge(nome, r.valorTotal(), BigDecimal::add);
            }
        }
        List<LinhaPeca> linhasPeca = porPeca.entrySet().stream().map(en -> {
            Acumulado a = en.getValue();
            return new LinhaPeca(en.getKey(), a.pecas, a.faturamento, a.materiais, a.ganho, a.horas,
                    a.horas.signum() > 0 ? a.ganho.divide(a.horas, 2, RoundingMode.HALF_UP) : null);
        }).sorted(Comparator.comparing(LinhaPeca::faturamento).reversed()).toList();

        // Pronta-entrega
        Map<String, Acumulado> porItem = new LinkedHashMap<>();
        BigDecimal descontos = BigDecimal.ZERO;
        for (Venda v : vendas.doPeriodo(de, ate)) {
            if (v.getStatus() != StatusVenda.CONCLUIDA) {
                continue;
            }
            descontos = descontos.add(v.getDesconto());
            for (VendaItem i : v.getItens()) {
                BigDecimal q = BigDecimal.valueOf(i.getQuantidade());
                Acumulado a = porItem.computeIfAbsent(i.getDescricao(), k -> new Acumulado());
                a.pecas += i.getQuantidade();
                a.faturamento = a.faturamento.add(i.getPrecoUnitario().multiply(q));
                a.materiais = a.materiais.add(i.getCustoUnitario().multiply(q));
                ranking.computeIfAbsent(i.getDescricao(), k -> new int[2])[1] += i.getQuantidade();
                rankingValor.merge(i.getDescricao(), i.getPrecoUnitario().multiply(q), BigDecimal::add);
            }
        }
        List<LinhaVenda> linhasVenda = porItem.entrySet().stream().map(en -> new LinhaVenda(en.getKey(),
                        en.getValue().pecas, en.getValue().faturamento, en.getValue().materiais,
                        en.getValue().faturamento.subtract(en.getValue().materiais)))
                .sorted(Comparator.comparing(LinhaVenda::faturamento).reversed()).toList();

        List<LinhaRanking> maisVendidos = ranking.entrySet().stream()
                .map(en -> new LinhaRanking(en.getKey(), en.getValue()[0] + en.getValue()[1], en.getValue()[0],
                        en.getValue()[1], rankingValor.getOrDefault(en.getKey(), BigDecimal.ZERO)))
                .sorted(Comparator.comparingInt(LinhaRanking::pecas).reversed()
                        .thenComparing(Comparator.comparing(LinhaRanking::faturamento).reversed()))
                .limit(10).toList();

        // Materiais mais usados (encomendas e produção para o estoque)
        Map<Long, LinhaMaterialAcc> porMaterial = new LinkedHashMap<>();
        for (ConsumoMaterial c : consumos.findByDataBetween(de, ate)) {
            Material m = c.getMaterial();
            LinhaMaterialAcc a = porMaterial.computeIfAbsent(m.getId(), k -> new LinhaMaterialAcc(m.getNome(), m.getUnidade()));
            a.quantidade = a.quantidade.add(c.getQuantidade());
            a.custo = a.custo.add(c.getCustoTotal());
        }
        List<LinhaMaterial> linhasMaterial = porMaterial.values().stream()
                .map(a -> new LinhaMaterial(a.nome, a.unidade, a.quantidade, a.custo))
                .sorted(Comparator.comparing(LinhaMaterial::custo).reversed()).toList();

        BigDecimal ticket = entregues.isEmpty() ? BigDecimal.ZERO
                : faturamentoEncomendas.divide(BigDecimal.valueOf(entregues.size()), 2, RoundingMode.HALF_UP);

        return new Relatorio(de, ate, deAnt, ateAnt,
                comparar(ent, entAnt), comparar(sai, saiAnt), comparar(ent.subtract(sai), entAnt.subtract(saiAnt)),
                entregues.size(), ticket,
                evolucao(ate), FinanceiroService.percentuais(porCat, ent), linhasPeca, linhasVenda, descontos,
                maisVendidos, linhasMaterial, lancs.stream().map(FinanceiroService::paraResp).toList());
    }

    /** Entradas e saídas dos 12 meses que terminam no mês final do período. */
    private List<MesValores> evolucao(LocalDate ate) {
        YearMonth fim = YearMonth.from(ate);
        YearMonth inicio = fim.minusMonths(11);
        List<Lancamento> todos = lancamentos.findByDataBetween(inicio.atDay(1), fim.atEndOfMonth());
        List<MesValores> meses = new ArrayList<>();
        for (YearMonth ym = inicio; !ym.isAfter(fim); ym = ym.plusMonths(1)) {
            final YearMonth m = ym;
            List<Lancamento> doMes = todos.stream().filter(l -> YearMonth.from(l.getData()).equals(m)).toList();
            String rotulo = m.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "") + "/"
                    + String.valueOf(m.getYear()).substring(2);
            meses.add(new MesValores(rotulo, FinanceiroService.soma(doMes, TipoLancamento.ENTRADA),
                    FinanceiroService.soma(doMes, TipoLancamento.SAIDA)));
        }
        return meses;
    }

    private static Comparativo comparar(BigDecimal atual, BigDecimal anterior) {
        Integer variacao = anterior.signum() == 0 ? null
                : atual.subtract(anterior).multiply(BigDecimal.valueOf(100))
                .divide(anterior.abs(), 0, RoundingMode.HALF_UP).intValue();
        return new Comparativo(atual, anterior, variacao);
    }

    private static final class Acumulado {
        int pecas;
        BigDecimal faturamento = BigDecimal.ZERO;
        BigDecimal materiais = BigDecimal.ZERO;
        BigDecimal ganho = BigDecimal.ZERO;
        BigDecimal horas = BigDecimal.ZERO;
    }

    private static final class LinhaMaterialAcc {
        final String nome;
        final String unidade;
        BigDecimal quantidade = BigDecimal.ZERO;
        BigDecimal custo = BigDecimal.ZERO;

        LinhaMaterialAcc(String nome, String unidade) {
            this.nome = nome;
            this.unidade = unidade;
        }
    }
}
