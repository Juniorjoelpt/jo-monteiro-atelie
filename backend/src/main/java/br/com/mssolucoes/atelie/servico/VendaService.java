package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Enums.*;
import br.com.mssolucoes.atelie.dominio.Lancamento;
import br.com.mssolucoes.atelie.dominio.Produto;
import br.com.mssolucoes.atelie.dominio.Venda;
import br.com.mssolucoes.atelie.dominio.VendaItem;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.LancamentoRepository;
import br.com.mssolucoes.atelie.repositorio.ProdutoRepository;
import br.com.mssolucoes.atelie.repositorio.VendaRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Venda de peças prontas: tira do estoque e lança a entrada no financeiro. */
@Service
public class VendaService {

    private final VendaRepository repo;
    private final ProdutoRepository produtos;
    private final LancamentoRepository lancamentos;
    private final ClienteService clientes;
    private final PrecificacaoService precificacao;

    public VendaService(VendaRepository repo, ProdutoRepository produtos, LancamentoRepository lancamentos,
                        ClienteService clientes, PrecificacaoService precificacao) {
        this.repo = repo;
        this.produtos = produtos;
        this.lancamentos = lancamentos;
        this.clientes = clientes;
        this.precificacao = precificacao;
    }

    @Transactional
    public VendaResp registrar(VendaReq r) {
        var par = precificacao.parametros();
        Venda v = new Venda();
        v.setData(r.data() == null ? LocalDate.now() : r.data());
        if (v.getData().isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data da venda não pode ser no futuro.");
        }
        v.setCliente(r.clienteId() == null ? null : clientes.buscar(r.clienteId()));
        v.setCanal(r.canal() == null ? CanalVenda.ATELIE : r.canal());
        v.setFormaPagamento(r.formaPagamento() == null ? FormaPagamento.PIX : r.formaPagamento());
        v.setObservacao(Texto.limpar(r.observacao()));

        // Soma as quantidades por peça antes de conferir o estoque
        Map<Long, Integer> porProduto = new HashMap<>();
        r.itens().stream().filter(i -> i.produtoId() != null)
                .forEach(i -> porProduto.merge(i.produtoId(), i.quantidade(), Integer::sum));
        Map<Long, Produto> carregados = new HashMap<>();
        for (var e : porProduto.entrySet()) {
            Produto p = produtos.findById(e.getKey()).orElseThrow(() -> new NaoEncontradoException("Peça"));
            if (!p.isProntaEntrega() || !p.isAtivo()) {
                throw new RegraNegocioException(p.getNome() + " não está à venda como pronta-entrega.");
            }
            if (p.getEstoque() < e.getValue()) {
                throw new RegraNegocioException(p.getEstoque() == 0
                        ? p.getNome() + " está sem peças prontas. Registre a produção em Produtos."
                        : "Só há " + p.getEstoque() + " " + p.getNome() + " pronta(s).");
            }
            carregados.put(p.getId(), p);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        int pecas = 0;
        for (VendaItemReq i : r.itens()) {
            VendaItem item = new VendaItem();
            item.setVenda(v);
            item.setQuantidade(i.quantidade());
            if (i.produtoId() != null) {
                Produto p = carregados.get(i.produtoId());
                var custo = precificacao.calcular(p, par);
                item.setProduto(p);
                item.setDescricao(p.getNome());
                item.setPrecoUnitario(i.precoUnitario() != null ? i.precoUnitario() : custo.precoAtual());
                item.setCustoUnitario(custo.custoMateriais().add(custo.outrosCustos()));
            } else {
                String desc = Texto.limpar(i.descricao());
                if (desc == null || i.precoUnitario() == null) {
                    throw new RegraNegocioException("Item avulso precisa de descrição e preço.");
                }
                item.setDescricao(desc);
                item.setPrecoUnitario(i.precoUnitario());
                item.setCustoUnitario(BigDecimal.ZERO);
            }
            subtotal = subtotal.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
            pecas += item.getQuantidade();
            v.getItens().add(item);
        }
        BigDecimal desconto = r.desconto() == null ? BigDecimal.ZERO : r.desconto();
        if (desconto.compareTo(subtotal) > 0) {
            throw new RegraNegocioException("O desconto é maior que o valor da venda.");
        }
        v.setDesconto(desconto);
        v.setTotal(subtotal.subtract(desconto));

        porProduto.forEach((id, qtd) -> {
            Produto p = carregados.get(id);
            p.setEstoque(p.getEstoque() - qtd);
        });
        repo.save(v);

        if (v.getTotal().signum() > 0) {
            Lancamento l = new Lancamento();
            l.setTipo(TipoLancamento.ENTRADA);
            l.setCategoria(CategoriaLancamento.VENDA);
            l.setDescricao("Venda pronta-entrega — " + pecas + (pecas == 1 ? " peça" : " peças")
                    + " (" + nomeCanal(v.getCanal()) + ")");
            l.setValor(v.getTotal());
            l.setData(v.getData());
            l.setFormaPagamento(v.getFormaPagamento());
            l.setVenda(v);
            lancamentos.save(l);
        }
        return paraResp(v);
    }

    /** Cancela a venda: as peças voltam para o estoque e a entrada sai do financeiro. */
    @Transactional
    public VendaResp cancelar(Long id) {
        Venda v = repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Venda"));
        if (v.getStatus() == StatusVenda.CANCELADA) {
            throw new RegraNegocioException("Esta venda já está cancelada.");
        }
        for (VendaItem i : v.getItens()) {
            if (i.getProduto() != null) {
                i.getProduto().setEstoque(i.getProduto().getEstoque() + i.getQuantidade());
            }
        }
        lancamentos.deleteByVendaId(id);
        v.setStatus(StatusVenda.CANCELADA);
        return paraResp(repo.save(v));
    }

    @Transactional(readOnly = true)
    public ResumoVendas resumo(int ano, int mes) {
        YearMonth ym = YearMonth.of(ano, mes);
        List<Venda> lista = repo.doPeriodo(ym.atDay(1), ym.atEndOfMonth());
        BigDecimal total = BigDecimal.ZERO, custo = BigDecimal.ZERO;
        int pecas = 0, qtd = 0;
        Map<CanalVenda, BigDecimal> porCanal = new EnumMap<>(CanalVenda.class);
        for (Venda v : lista) {
            if (v.getStatus() != StatusVenda.CONCLUIDA) {
                continue;
            }
            qtd++;
            total = total.add(v.getTotal());
            custo = custo.add(custo(v));
            pecas += v.getItens().stream().mapToInt(VendaItem::getQuantidade).sum();
            porCanal.merge(v.getCanal(), v.getTotal(), BigDecimal::add);
        }
        final BigDecimal t = total;
        List<ValorPorNome> canais = porCanal.entrySet().stream()
                .sorted(Map.Entry.<CanalVenda, BigDecimal>comparingByValue().reversed())
                .map(e -> new ValorPorNome(nomeCanal(e.getKey()), e.getValue(), t.signum() == 0 ? 0
                        : e.getValue().multiply(BigDecimal.valueOf(100)).divide(t, 0, RoundingMode.HALF_UP).intValue()))
                .toList();
        return new ResumoVendas(ano, mes, total, custo, total.subtract(custo), pecas, qtd, canais,
                lista.stream().map(this::paraResp).toList());
    }

    private static BigDecimal custo(Venda v) {
        return v.getItens().stream()
                .map(i -> i.getCustoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static String nomeCanal(CanalVenda c) {
        return switch (c) {
            case ATELIE -> "Ateliê";
            case FEIRA -> "Feira";
            case INSTAGRAM -> "Instagram";
            case WHATSAPP -> "WhatsApp";
            case OUTRO -> "Outro";
        };
    }

    private VendaResp paraResp(Venda v) {
        return new VendaResp(v.getId(), v.getData(),
                v.getCliente() == null ? null : v.getCliente().getId(),
                v.getCliente() == null ? null : v.getCliente().getNome(),
                v.getCanal(), v.getFormaPagamento(), v.getDesconto(), v.getTotal(), custo(v), v.getObservacao(),
                v.getStatus(),
                v.getItens().stream().map(i -> new VendaItemResp(
                        i.getProduto() == null ? null : i.getProduto().getId(), i.getDescricao(), i.getQuantidade(),
                        i.getPrecoUnitario(), i.getCustoUnitario())).toList());
    }
}
