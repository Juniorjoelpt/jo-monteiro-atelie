package br.com.joeljunior.atelie.servico;

import br.com.joeljunior.atelie.dominio.Enums.*;
import br.com.joeljunior.atelie.dominio.Lancamento;
import br.com.joeljunior.atelie.dto.Dtos.*;
import br.com.joeljunior.atelie.repositorio.EncomendaRepository;
import br.com.joeljunior.atelie.repositorio.LancamentoRepository;
import br.com.joeljunior.atelie.web.NaoEncontradoException;
import br.com.joeljunior.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class FinanceiroService {

    private final LancamentoRepository repo;
    private final EncomendaRepository encomendas;
    private final EncomendaService encomendaService;
    private final HistoricoService historico;

    public FinanceiroService(LancamentoRepository repo, EncomendaRepository encomendas,
                             EncomendaService encomendaService, HistoricoService historico) {
        this.historico = historico;
        this.repo = repo;
        this.encomendas = encomendas;
        this.encomendaService = encomendaService;
    }

    @Transactional(readOnly = true)
    public ResumoFinanceiro resumo(int ano, int mes) {
        YearMonth ym = YearMonth.of(ano, mes);
        List<Lancamento> lista = repo.findByDataBetweenOrderByDataDescIdDesc(ym.atDay(1), ym.atEndOfMonth());
        BigDecimal entradas = soma(lista, TipoLancamento.ENTRADA);
        BigDecimal saidas = soma(lista, TipoLancamento.SAIDA);

        List<EncomendaResp> aReceber = contasAReceber();
        BigDecimal totalAReceber = aReceber.stream().map(EncomendaResp::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<CategoriaLancamento, BigDecimal> porCategoria = new EnumMap<>(CategoriaLancamento.class);
        lista.stream().filter(l -> l.getTipo() == TipoLancamento.ENTRADA)
                .forEach(l -> porCategoria.merge(l.getCategoria(), l.getValor(), BigDecimal::add));

        return new ResumoFinanceiro(ano, mes, entradas, saidas, entradas.subtract(saidas), totalAReceber,
                percentuais(porCategoria, entradas), lista.stream().map(FinanceiroService::paraResp).toList(), aReceber);
    }

    /** Encomendas não canceladas com saldo em aberto (inclui entregues ainda não quitadas). */
    @Transactional(readOnly = true)
    public List<EncomendaResp> contasAReceber() {
        return encomendas.findByStatusInOrderByPrazoEntregaAsc(EnumSet.of(StatusEncomenda.AGUARDANDO_SINAL,
                        StatusEncomenda.EM_PRODUCAO, StatusEncomenda.PRONTA, StatusEncomenda.ENTREGUE))
                .stream().map(encomendaService::paraResp)
                .filter(e -> e.saldo().signum() > 0)
                .toList();
    }

    @Transactional
    public LancamentoResp criar(LancamentoReq r) {
        Lancamento l = new Lancamento();
        preencher(l, r);
        repo.save(l);
        historico.registrar(AreaHistorico.FINANCEIRO, null, "LANCOU", "Lançou " + descrever(l) + ".", true);
        return paraResp(l);
    }

    @Transactional
    public LancamentoResp atualizar(Long id, LancamentoReq r) {
        Lancamento l = buscar(id);
        bloquearAutomatico(l);
        String antes = descrever(l);
        preencher(l, r);
        repo.save(l);
        String depois = descrever(l);
        if (!antes.equals(depois)) {
            historico.registrar(AreaHistorico.FINANCEIRO, null, "EDITOU", "Editou o lançamento: " + antes + " → " + depois + ".", true);
        }
        return paraResp(l);
    }

    @Transactional
    public void excluir(Long id) {
        Lancamento l = buscar(id);
        bloquearAutomatico(l);
        historico.registrar(AreaHistorico.FINANCEIRO, l.getEncomenda(), "EXCLUIU", "Excluiu o lançamento " + descrever(l) + ".", true);
        repo.delete(l);
    }

    private static void bloquearAutomatico(Lancamento l) {
        if (l.getVenda() != null) {
            throw new RegraNegocioException("Este lançamento vem de uma venda. Cancele a venda em Vendas.");
        }
        if (l.getCompra() != null) {
            throw new RegraNegocioException("Este lançamento vem de uma compra. Cancele a compra em Compras.");
        }
        if (l.getContaFixa() != null) {
            throw new RegraNegocioException("Este lançamento é o pagamento de uma conta fixa. Desfaça em Contas fixas.");
        }
    }

    private Lancamento buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Lançamento"));
    }

    private void preencher(Lancamento l, LancamentoReq r) {
        l.setTipo(r.tipo());
        l.setCategoria(r.categoria());
        l.setDescricao(r.descricao().trim());
        l.setValor(r.valor());
        l.setData(r.data());
        l.setFormaPagamento(r.formaPagamento() == null ? FormaPagamento.PIX : r.formaPagamento());
    }

    static BigDecimal soma(List<Lancamento> lista, TipoLancamento tipo) {
        return lista.stream().filter(l -> l.getTipo() == tipo)
                .map(Lancamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static List<ValorPorNome> percentuais(Map<CategoriaLancamento, BigDecimal> mapa, BigDecimal total) {
        List<ValorPorNome> out = new ArrayList<>();
        mapa.entrySet().stream()
                .sorted(Map.Entry.<CategoriaLancamento, BigDecimal>comparingByValue().reversed())
                .forEach(e -> {
                    int pct = total.signum() == 0 ? 0 : e.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(total, 0, RoundingMode.HALF_UP).intValue();
                    out.add(new ValorPorNome(nome(e.getKey()), e.getValue(), pct));
                });
        return out;
    }

    public static String nome(CategoriaLancamento c) {
        return switch (c) {
            case PAPELARIA -> "Papelaria";
            case PORCELANA -> "Porcelana (canecas)";
            case PRESENTE -> "Presentes";
            case VENDA -> "Pronta-entrega";
            case MATERIAL -> "Materiais";
            case CONTAS_FIXAS -> "Contas fixas";
            case OUTROS -> "Outros";
        };
    }

    public static LancamentoResp paraResp(Lancamento l) {
        return new LancamentoResp(l.getId(), l.getTipo(), l.getCategoria(), l.getDescricao(), l.getValor(),
                l.getData(), l.getFormaPagamento(),
                l.getEncomenda() == null ? null : l.getEncomenda().getId(),
                l.getVenda() == null ? null : l.getVenda().getId(),
                l.getCompra() == null ? null : l.getCompra().getId(),
                l.getContaFixa() == null ? null : l.getContaFixa().getId(),
                l.getVenda() != null || l.getCompra() != null || l.getContaFixa() != null);
    }

    static LocalDate hoje() {
        return LocalDate.now();
    }

    private static String descrever(Lancamento l) {
        return (l.getTipo() == TipoLancamento.ENTRADA ? "entrada" : "saída") + " de " + HistoricoService.pdfMoeda(l.getValor())
                + " (“" + l.getDescricao() + "”, " + "%02d/%02d/%d".formatted(l.getData().getDayOfMonth(),
                l.getData().getMonthValue(), l.getData().getYear()) + ")";
    }
}
