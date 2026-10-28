package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.*;
import br.com.mssolucoes.atelie.dominio.Enums.*;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.*;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Fornecedores e compras de material. Cada compra soma no estoque, atualiza o custo do material
 * (as fichas técnicas acompanham) e pode virar despesa no financeiro. O frete é dividido entre os
 * itens pelo valor de cada um, para o custo por unidade ser o real.
 */
@Service
public class CompraService {

    private static final String SEM_FORNECEDOR = "Sem fornecedor";

    private final CompraRepository compras;
    private final CompraItemRepository itens;
    private final FornecedorRepository fornecedores;
    private final MaterialRepository materiais;
    private final LancamentoRepository lancamentos;

    public CompraService(CompraRepository compras, CompraItemRepository itens, FornecedorRepository fornecedores,
                         MaterialRepository materiais, LancamentoRepository lancamentos) {
        this.compras = compras;
        this.itens = itens;
        this.fornecedores = fornecedores;
        this.materiais = materiais;
        this.lancamentos = lancamentos;
    }

    // ---------- Fornecedores ----------

    @Transactional(readOnly = true)
    public List<FornecedorResp> listarFornecedores() {
        Map<Long, List<Compra>> porFornecedor = new HashMap<>();
        for (Compra c : compras.findByStatus(StatusCompra.CONCLUIDA)) {
            if (c.getFornecedor() != null) {
                porFornecedor.computeIfAbsent(c.getFornecedor().getId(), k -> new ArrayList<>()).add(c);
            }
        }
        return fornecedores.findAllByOrderByAtivoDescNomeAsc().stream()
                .map(f -> paraResp(f, porFornecedor.getOrDefault(f.getId(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public List<CompraResp> comprasDoFornecedor(Long fornecedorId) {
        buscarFornecedor(fornecedorId);
        return compras.findByFornecedorIdOrderByDataDescIdDesc(fornecedorId).stream().map(this::paraResp).toList();
    }

    @Transactional
    public FornecedorResp criarFornecedor(FornecedorReq r) {
        Fornecedor f = new Fornecedor();
        preencher(f, r);
        return paraResp(fornecedores.save(f), List.of());
    }

    @Transactional
    public FornecedorResp atualizarFornecedor(Long id, FornecedorReq r) {
        Fornecedor f = buscarFornecedor(id);
        preencher(f, r);
        fornecedores.save(f);
        return paraResp(f, compras.findByFornecedorIdOrderByDataDescIdDesc(id).stream()
                .filter(c -> c.getStatus() == StatusCompra.CONCLUIDA).toList());
    }

    @Transactional
    public void excluirFornecedor(Long id) {
        Fornecedor f = buscarFornecedor(id);
        if (compras.existsByFornecedorId(id)) {
            throw new RegraNegocioException("Este fornecedor tem compras no histórico. Desative em vez de excluir.");
        }
        fornecedores.delete(f);
    }

    private void preencher(Fornecedor f, FornecedorReq r) {
        f.setNome(r.nome().trim());
        f.setWhatsapp(r.whatsapp() == null || r.whatsapp().isBlank() ? null : Texto.whatsapp(r.whatsapp()));
        f.setContato(Texto.limpar(r.contato()));
        f.setCidade(Texto.limpar(r.cidade()));
        f.setObservacoes(Texto.limpar(r.observacoes()));
        f.setAtivo(r.ativo() == null || r.ativo());
    }

    private Fornecedor buscarFornecedor(Long id) {
        return fornecedores.findById(id).orElseThrow(() -> new NaoEncontradoException("Fornecedor"));
    }

    // ---------- Compras ----------

    @Transactional(readOnly = true)
    public ResumoCompras doMes(int ano, int mes) {
        YearMonth ym = YearMonth.of(ano, mes);
        List<Compra> lista = compras.findByDataBetweenOrderByDataDescIdDesc(ym.atDay(1), ym.atEndOfMonth());
        List<Compra> validas = lista.stream().filter(c -> c.getStatus() == StatusCompra.CONCLUIDA).toList();
        BigDecimal total = validas.stream().map(Compra::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> porFornecedor = new LinkedHashMap<>();
        validas.forEach(c -> porFornecedor.merge(c.getFornecedor() != null ? c.getFornecedor().getNome() : SEM_FORNECEDOR,
                c.getTotal(), BigDecimal::add));
        List<ValorPorNome> fatias = porFornecedor.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new ValorPorNome(e.getKey(), e.getValue(), total.signum() == 0 ? 0
                        : e.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue()))
                .toList();
        return new ResumoCompras(ano, mes, total, validas.size(), fatias, lista.stream().map(this::paraResp).toList());
    }

    @Transactional(readOnly = true)
    public CompraResp detalhar(Long id) {
        return paraResp(buscar(id));
    }

    @Transactional
    public CompraResp registrar(CompraReq r) {
        Compra c = new Compra();
        c.setFornecedor(r.fornecedorId() == null ? null : buscarFornecedor(r.fornecedorId()));
        c.setData(r.data());
        c.setFrete(r.frete() == null ? BigDecimal.ZERO : r.frete().setScale(2, RoundingMode.HALF_UP));
        c.setFormaPagamento(r.formaPagamento() == null ? FormaPagamento.PIX : r.formaPagamento());
        c.setObservacoes(Texto.limpar(r.observacoes()));

        Set<Long> vistos = new HashSet<>();
        BigDecimal subtotal = r.itens().stream().map(CompraItemReq::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal freteRestante = c.getFrete();
        for (int i = 0; i < r.itens().size(); i++) {
            CompraItemReq ir = r.itens().get(i);
            Material m = materiais.findById(ir.materialId()).orElseThrow(() -> new NaoEncontradoException("Material"));
            if (!vistos.add(m.getId())) {
                throw new RegraNegocioException(m.getNome() + " aparece duas vezes na compra. Junte numa linha só.");
            }
            // O último item fica com o que sobrou do frete, para a soma fechar no centavo
            BigDecimal parteFrete = i == r.itens().size() - 1 ? freteRestante
                    : c.getFrete().multiply(ir.valor()).divide(subtotal, 2, RoundingMode.HALF_UP);
            freteRestante = freteRestante.subtract(parteFrete);

            CompraItem item = new CompraItem();
            item.setCompra(c);
            item.setMaterial(m);
            item.setQuantidade(ir.quantidade());
            item.setValor(ir.valor().setScale(2, RoundingMode.HALF_UP));
            item.setCustoUnitario(ir.valor().add(parteFrete).divide(ir.quantidade(), 4, RoundingMode.HALF_UP));
            c.getItens().add(item);

            m.setQuantidade(m.getQuantidade().add(ir.quantidade()));
            m.setCustoUnitario(item.getCustoUnitario());
            materiais.save(m);
        }
        c.setTotal(subtotal.add(c.getFrete()).setScale(2, RoundingMode.HALF_UP));
        compras.save(c);

        if (r.registrarDespesa() == null || r.registrarDespesa()) {
            Lancamento l = new Lancamento();
            l.setTipo(TipoLancamento.SAIDA);
            l.setCategoria(CategoriaLancamento.MATERIAL);
            l.setDescricao(descricaoDespesa(c));
            l.setValor(c.getTotal());
            l.setData(c.getData());
            l.setFormaPagamento(c.getFormaPagamento());
            l.setCompra(c);
            lancamentos.save(l);
        }
        return paraResp(c);
    }

    /**
     * Desfaz a compra: tira os materiais do estoque, apaga a despesa e volta o custo de cada material
     * para o da compra anterior (se houver).
     */
    @Transactional
    public CompraResp cancelar(Long id) {
        Compra c = buscar(id);
        if (c.getStatus() == StatusCompra.CANCELADA) {
            throw new RegraNegocioException("Esta compra já foi cancelada.");
        }
        c.setStatus(StatusCompra.CANCELADA);
        compras.saveAndFlush(c);
        for (CompraItem item : c.getItens()) {
            Material m = item.getMaterial();
            m.setQuantidade(m.getQuantidade().subtract(item.getQuantidade()));
            List<CompraItem> anteriores = itens.historico(m.getId(), StatusCompra.CONCLUIDA);
            if (!anteriores.isEmpty()) {
                m.setCustoUnitario(anteriores.get(0).getCustoUnitario());
            }
            materiais.save(m);
        }
        lancamentos.deleteByCompraId(c.getId());
        return paraResp(c);
    }

    @Transactional(readOnly = true)
    public HistoricoPrecos historico(Long materialId) {
        Material m = materiais.findById(materialId).orElseThrow(() -> new NaoEncontradoException("Material"));
        List<CompraItem> lista = itens.historico(materialId, StatusCompra.CONCLUIDA);
        List<PrecoPago> pagos = lista.stream().map(i -> new PrecoPago(i.getCompra().getId(), i.getCompra().getData(),
                nomeFornecedor(i.getCompra()), i.getQuantidade(), i.getCustoUnitario())).toList();

        // O primeiro de cada fornecedor na lista (mais recente) é o último preço pago lá
        Map<String, CompraItem> ultimo = new LinkedHashMap<>();
        lista.forEach(i -> ultimo.putIfAbsent(nomeFornecedor(i.getCompra()), i));
        BigDecimal menor = ultimo.values().stream().map(CompraItem::getCustoUnitario).min(BigDecimal::compareTo).orElse(null);
        List<PrecoPorFornecedor> porFornecedor = ultimo.values().stream()
                .sorted(Comparator.comparing(CompraItem::getCustoUnitario))
                .map(i -> new PrecoPorFornecedor(i.getCompra().getFornecedor() == null ? null : i.getCompra().getFornecedor().getId(),
                        nomeFornecedor(i.getCompra()), i.getCustoUnitario(), i.getCompra().getData(),
                        ultimo.size() > 1 && i.getCustoUnitario().compareTo(menor) == 0))
                .toList();
        return new HistoricoPrecos(m.getId(), m.getNome(), m.getUnidade(), m.getCustoUnitario(), pagos, porFornecedor);
    }

    // ---------- Apoio ----------

    private Compra buscar(Long id) {
        return compras.findById(id).orElseThrow(() -> new NaoEncontradoException("Compra"));
    }

    private static String nomeFornecedor(Compra c) {
        return c.getFornecedor() != null ? c.getFornecedor().getNome() : SEM_FORNECEDOR;
    }

    private static String descricaoDespesa(Compra c) {
        List<String> nomes = c.getItens().stream().map(i -> i.getMaterial().getNome()).toList();
        String lista = nomes.size() <= 3 ? String.join(", ", nomes)
                : String.join(", ", nomes.subList(0, 2)) + " e mais " + (nomes.size() - 2);
        String d = "Compra" + (c.getFornecedor() != null ? " " + c.getFornecedor().getNome() : "") + " — " + lista;
        return d.length() > 200 ? d.substring(0, 197) + "…" : d;
    }

    private FornecedorResp paraResp(Fornecedor f, List<Compra> concluidas) {
        BigDecimal total = concluidas.stream().map(Compra::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate ultima = concluidas.stream().map(Compra::getData).max(LocalDate::compareTo).orElse(null);
        return new FornecedorResp(f.getId(), f.getNome(), f.getWhatsapp(), f.getContato(), f.getCidade(),
                f.getObservacoes(), f.isAtivo(), concluidas.size(), total, ultima);
    }

    private CompraResp paraResp(Compra c) {
        return new CompraResp(c.getId(), c.getFornecedor() == null ? null : c.getFornecedor().getId(),
                c.getFornecedor() == null ? null : c.getFornecedor().getNome(), c.getData(), c.getFrete(), c.getTotal(),
                c.getFormaPagamento(), c.getObservacoes(), c.getStatus(),
                c.getId() != null && lancamentos.existsByCompraId(c.getId()),
                c.getItens().stream().map(i -> new CompraItemResp(i.getMaterial().getId(), i.getMaterial().getNome(),
                        i.getMaterial().getUnidade(), i.getQuantidade(), i.getValor(), i.getCustoUnitario())).toList());
    }
}
