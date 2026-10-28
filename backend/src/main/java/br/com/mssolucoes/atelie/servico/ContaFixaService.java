package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Configuracao;
import br.com.mssolucoes.atelie.dominio.ContaFixa;
import br.com.mssolucoes.atelie.dominio.Enums.CategoriaLancamento;
import br.com.mssolucoes.atelie.dominio.Enums.FormaPagamento;
import br.com.mssolucoes.atelie.dominio.Enums.TipoLancamento;
import br.com.mssolucoes.atelie.dominio.Lancamento;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.ConfiguracaoRepository;
import br.com.mssolucoes.atelie.repositorio.ContaFixaRepository;
import br.com.mssolucoes.atelie.repositorio.LancamentoRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contas que se repetem todo mês. Pagar uma conta lança a despesa no financeiro, ligada ao mês
 * (não dá para pagar duas vezes o mesmo mês). A soma das contas pode alimentar a precificação.
 */
@Service
public class ContaFixaService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    /** Avisa no painel quando faltam até estes dias para vencer. */
    public static final int DIAS_AVISO = 5;

    private final ContaFixaRepository repo;
    private final LancamentoRepository lancamentos;
    private final ConfiguracaoRepository configuracoes;
    private final ConfiguracaoService configuracao;

    public ContaFixaService(ContaFixaRepository repo, LancamentoRepository lancamentos,
                            ConfiguracaoRepository configuracoes, ConfiguracaoService configuracao) {
        this.repo = repo;
        this.lancamentos = lancamentos;
        this.configuracoes = configuracoes;
        this.configuracao = configuracao;
    }

    @Transactional(readOnly = true)
    public ContasDoMes doMes(int ano, int mes) {
        YearMonth ym = YearMonth.of(ano, mes);
        LocalDate hoje = LocalDate.now();
        Map<Long, Lancamento> pagos = pagamentos(List.of(ym));

        List<ContaFixa> todas = repo.findAllByOrderByDiaVencimentoAscNomeAsc();
        List<ContaDoMes> doMes = new ArrayList<>();
        List<ContaFixaResp> inativas = new ArrayList<>();
        for (ContaFixa c : todas) {
            Lancamento pago = pagos.get(c.getId());
            // Conta desativada continua aparecendo nos meses em que foi paga
            if (!c.isAtiva() && pago == null) {
                inativas.add(paraResp(c));
                continue;
            }
            doMes.add(linha(c, ym, pago, hoje));
        }

        BigDecimal pago = doMes.stream().filter(l -> l.valorPago() != null).map(ContaDoMes::valorPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal falta = doMes.stream().filter(l -> l.valorPago() == null).map(l -> l.conta().valor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int vencidas = (int) doMes.stream().filter(l -> l.situacao().equals("VENCIDA")).count();
        return new ContasDoMes(ano, mes, pago.add(falta), pago, falta, vencidas, totalPrecificacao(todas),
                configuracao.atual().isCustosFixosAutomatico(), doMes, inativas);
    }

    /** Contas do mês passado e deste mês ainda não pagas que já venceram ou vencem logo. */
    @Transactional(readOnly = true)
    public List<ContaAVencer> aVencer() {
        LocalDate hoje = LocalDate.now();
        YearMonth atual = YearMonth.from(hoje);
        List<YearMonth> meses = List.of(atual.minusMonths(1), atual);
        Map<String, Lancamento> pagos = lancamentos.findByContaFixaIsNotNullAndCompetenciaIn(
                        meses.stream().map(m -> m.atDay(1)).toList()).stream()
                .collect(Collectors.toMap(l -> l.getContaFixa().getId() + ":" + l.getCompetencia(), Function.identity(), (a, b) -> a));
        List<ContaAVencer> lista = new ArrayList<>();
        for (ContaFixa c : repo.findByAtivaTrueOrderByDiaVencimentoAscNomeAsc()) {
            for (YearMonth ym : meses) {
                // Conta criada depois do mês passado não cobra o mês passado
                if (ym.isBefore(YearMonth.from(c.getCriadoEm())) || pagos.containsKey(c.getId() + ":" + ym.atDay(1))) {
                    continue;
                }
                LocalDate venc = vencimento(c, ym);
                if (!venc.isAfter(hoje.plusDays(DIAS_AVISO))) {
                    lista.add(new ContaAVencer(c.getId(), c.getNome(), c.getValor(), venc, venc.isBefore(hoje)));
                }
            }
        }
        lista.sort(Comparator.comparing(ContaAVencer::vencimento));
        return lista;
    }

    @Transactional
    public ContaFixaResp criar(ContaFixaReq r) {
        ContaFixa c = new ContaFixa();
        preencher(c, r);
        repo.save(c);
        sincronizarPrecificacao();
        return paraResp(c);
    }

    @Transactional
    public ContaFixaResp atualizar(Long id, ContaFixaReq r) {
        ContaFixa c = buscar(id);
        preencher(c, r);
        repo.save(c);
        sincronizarPrecificacao();
        return paraResp(c);
    }

    @Transactional
    public void excluir(Long id) {
        ContaFixa c = buscar(id);
        if (lancamentos.existsByContaFixaId(id)) {
            throw new RegraNegocioException("Esta conta já tem pagamentos no financeiro. Desative em vez de excluir.");
        }
        repo.delete(c);
        repo.flush();
        sincronizarPrecificacao();
    }

    @Transactional
    public ContaDoMes pagar(Long id, PagarContaReq r) {
        ContaFixa c = buscar(id);
        YearMonth ym = YearMonth.of(r.ano(), r.mes());
        if (lancamentos.findByContaFixaIdAndCompetencia(id, ym.atDay(1)).isPresent()) {
            throw new RegraNegocioException(c.getNome() + " de " + nomeMes(ym) + " já está paga.");
        }
        Lancamento l = new Lancamento();
        l.setTipo(TipoLancamento.SAIDA);
        l.setCategoria(CategoriaLancamento.CONTAS_FIXAS);
        l.setDescricao(c.getNome() + " — " + nomeMes(ym));
        l.setValor(r.valor());
        l.setData(r.data() == null ? LocalDate.now() : r.data());
        l.setFormaPagamento(r.formaPagamento() == null ? FormaPagamento.PIX : r.formaPagamento());
        l.setContaFixa(c);
        l.setCompetencia(ym.atDay(1));
        lancamentos.save(l);
        return linha(c, ym, l, LocalDate.now());
    }

    @Transactional
    public void desfazerPagamento(Long id, int ano, int mes) {
        ContaFixa c = buscar(id);
        YearMonth ym = YearMonth.of(ano, mes);
        Lancamento l = lancamentos.findByContaFixaIdAndCompetencia(id, ym.atDay(1))
                .orElseThrow(() -> new RegraNegocioException(c.getNome() + " de " + nomeMes(ym) + " não está paga."));
        lancamentos.delete(l);
    }

    /** Soma das contas ativas que entram na precificação. */
    @Transactional(readOnly = true)
    public BigDecimal totalPrecificacao() {
        return totalPrecificacao(repo.findAllByOrderByDiaVencimentoAscNomeAsc());
    }

    /** Com a opção automática ligada, os custos fixos da precificação acompanham as contas. */
    @Transactional
    public void sincronizarPrecificacao() {
        configuracoes.findById(Configuracao.ID_UNICO).filter(Configuracao::isCustosFixosAutomatico).ifPresent(cfg -> {
            cfg.setCustosFixosMensais(totalPrecificacao());
            cfg.setAtualizadoEm(LocalDateTime.now());
            configuracoes.save(cfg);
        });
    }

    // ---------- Apoio ----------

    private static BigDecimal totalPrecificacao(List<ContaFixa> contas) {
        return contas.stream().filter(c -> c.isAtiva() && c.isEntraNaPrecificacao())
                .map(ContaFixa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, Lancamento> pagamentos(List<YearMonth> meses) {
        return lancamentos.findByContaFixaIsNotNullAndCompetenciaIn(meses.stream().map(m -> m.atDay(1)).toList())
                .stream().collect(Collectors.toMap(l -> l.getContaFixa().getId(), Function.identity(), (a, b) -> a));
    }

    private static ContaDoMes linha(ContaFixa c, YearMonth ym, Lancamento pago, LocalDate hoje) {
        LocalDate venc = vencimento(c, ym);
        String situacao;
        if (pago != null) {
            situacao = "PAGA";
        } else if (venc.isBefore(hoje)) {
            situacao = "VENCIDA";
        } else if (venc.equals(hoje)) {
            situacao = "HOJE";
        } else if (!venc.isAfter(hoje.plusDays(3))) {
            situacao = "PROXIMA";
        } else {
            situacao = "A_VENCER";
        }
        return new ContaDoMes(paraResp(c), venc, situacao, pago == null ? null : pago.getId(),
                pago == null ? null : pago.getValor(), pago == null ? null : pago.getData(),
                pago == null ? null : pago.getFormaPagamento());
    }

    /** Dia 31 em fevereiro vence no último dia do mês. */
    public static LocalDate vencimento(ContaFixa c, YearMonth ym) {
        return ym.atDay(Math.min(c.getDiaVencimento(), ym.lengthOfMonth()));
    }

    static String nomeMes(YearMonth ym) {
        String m = ym.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
        return m + "/" + ym.getYear();
    }

    private ContaFixa buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Conta"));
    }

    private static void preencher(ContaFixa c, ContaFixaReq r) {
        c.setNome(r.nome().trim());
        c.setValor(r.valor());
        c.setDiaVencimento(r.diaVencimento());
        c.setEntraNaPrecificacao(r.entraNaPrecificacao() == null || r.entraNaPrecificacao());
        c.setAtiva(r.ativa() == null || r.ativa());
        c.setObservacoes(Texto.limpar(r.observacoes()));
    }

    public static ContaFixaResp paraResp(ContaFixa c) {
        return new ContaFixaResp(c.getId(), c.getNome(), c.getValor(), c.getDiaVencimento(),
                c.isEntraNaPrecificacao(), c.isAtiva(), c.getObservacoes());
    }
}
