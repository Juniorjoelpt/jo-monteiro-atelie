package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Encomenda;
import br.com.mssolucoes.atelie.dominio.Enums.*;
import br.com.mssolucoes.atelie.dominio.Lancamento;
import br.com.mssolucoes.atelie.dominio.Produto;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.EncomendaRepository;
import br.com.mssolucoes.atelie.repositorio.LancamentoRepository;
import br.com.mssolucoes.atelie.repositorio.ProdutoRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class EncomendaService {

    /** Encomendas que ainda não foram entregues nem canceladas. */
    public static final Set<StatusEncomenda> ATIVAS =
            EnumSet.of(StatusEncomenda.AGUARDANDO_SINAL, StatusEncomenda.EM_PRODUCAO, StatusEncomenda.PRONTA);

    private final EncomendaRepository repo;
    private final LancamentoRepository lancamentos;
    private final ClienteService clientes;
    private final ProdutoRepository produtos;
    private final ConsumoService consumos;
    private final PrecificacaoService precificacao;
    private final br.com.mssolucoes.atelie.repositorio.EncomendaFotoRepository fotos;
    private final br.com.mssolucoes.atelie.repositorio.CompromissoRepository compromissos;
    private final br.com.mssolucoes.atelie.repositorio.ArteVersaoRepository artes;

    public EncomendaService(EncomendaRepository repo, LancamentoRepository lancamentos, ClienteService clientes,
                            ProdutoRepository produtos, ConsumoService consumos, PrecificacaoService precificacao,
                            br.com.mssolucoes.atelie.repositorio.EncomendaFotoRepository fotos,
                            br.com.mssolucoes.atelie.repositorio.CompromissoRepository compromissos,
                            br.com.mssolucoes.atelie.repositorio.ArteVersaoRepository artes) {
        this.artes = artes;
        this.fotos = fotos;
        this.compromissos = compromissos;
        this.repo = repo;
        this.lancamentos = lancamentos;
        this.clientes = clientes;
        this.produtos = produtos;
        this.consumos = consumos;
        this.precificacao = precificacao;
    }

    @Transactional(readOnly = true)
    public List<EncomendaResp> listar(StatusEncomenda status) {
        List<Encomenda> lista = status == null
                ? repo.findAllByOrderByCriadoEmDesc()
                : repo.findByStatusInOrderByPrazoEntregaAsc(EnumSet.of(status));
        return lista.stream().map(this::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<EncomendaResp> doCliente(Long clienteId) {
        return repo.findByClienteIdOrderByCriadoEmDesc(clienteId).stream().map(this::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public EncomendaResp detalhar(Long id) {
        return paraResp(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<LancamentoResp> pagamentos(Long id) {
        buscar(id);
        return lancamentos.findByEncomendaIdOrderByDataAsc(id).stream().map(FinanceiroService::paraResp).toList();
    }

    @Transactional
    public EncomendaResp criar(EncomendaReq r) {
        Encomenda e = new Encomenda();
        preencher(e, r);
        if (r.status() != null) {
            e.setStatus(r.status());
        }
        return salvarEProcessar(e);
    }

    @Transactional
    public EncomendaResp atualizar(Long id, EncomendaReq r) {
        Encomenda e = buscar(id);
        preencher(e, r);
        if (r.status() != null) {
            e.setStatus(r.status());
        }
        return salvarEProcessar(e);
    }

    @Transactional
    public EncomendaResp mudarStatus(Long id, StatusEncomenda status) {
        Encomenda e = buscar(id);
        e.setStatus(status);
        return salvarEProcessar(e);
    }

    /** Salva e, se a peça entrou em produção, baixa os materiais da ficha. */
    @Transactional
    public EncomendaResp salvarEProcessar(Encomenda e) {
        if (e.getStatus() == StatusEncomenda.ENTREGUE) {
            if (e.getEntregueEm() == null) {
                e.setEntregueEm(LocalDate.now());
            }
        } else {
            e.setEntregueEm(null);
        }
        repo.save(e);
        consumos.aposMudarStatus(e);
        return paraResp(repo.save(e));
    }

    @Transactional
    public EncomendaResp registrarPagamento(Long id, PagamentoReq r) {
        Encomenda e = buscar(id);
        if (e.getStatus() == StatusEncomenda.CANCELADA) {
            throw new RegraNegocioException("Esta encomenda está cancelada.");
        }
        BigDecimal pago = lancamentos.totalPagoEncomenda(id);
        BigDecimal saldo = e.getValorTotal().subtract(pago);
        if (r.valor().compareTo(saldo) > 0) {
            throw new RegraNegocioException("O valor é maior que o saldo em aberto (R$ " + saldo.setScale(2) + ").");
        }
        Lancamento l = new Lancamento();
        l.setTipo(TipoLancamento.ENTRADA);
        l.setCategoria(categoria(e.getFrente()));
        boolean primeiro = pago.signum() == 0;
        String desc = Texto.limpar(r.descricao());
        l.setDescricao(desc != null ? desc
                : (primeiro && r.valor().compareTo(e.getValorTotal()) < 0 ? "Sinal" : "Pagamento")
                + " — " + e.getDescricao() + " (" + e.getCliente().getNome() + ")");
        l.setValor(r.valor());
        l.setData(r.data() == null ? LocalDate.now() : r.data());
        l.setFormaPagamento(r.formaPagamento() == null ? FormaPagamento.PIX : r.formaPagamento());
        l.setEncomenda(e);
        lancamentos.save(l);

        BigDecimal novoPago = pago.add(r.valor());
        if (e.getStatus() == StatusEncomenda.AGUARDANDO_SINAL && novoPago.compareTo(e.getValorSinal()) >= 0) {
            e.setStatus(StatusEncomenda.EM_PRODUCAO);
            return salvarEProcessar(e);
        }
        return paraResp(e);
    }

    // ---------- Materiais usados ----------

    @Transactional(readOnly = true)
    public List<ConsumoResp> consumos(Long id) {
        buscar(id);
        return consumos.daEncomenda(id);
    }

    @Transactional
    public ConsumoResp registrarConsumo(Long id, ConsumoReq r) {
        return consumos.registrarManual(buscar(id), r.materialId(), r.quantidade());
    }

    /** Baixa a ficha agora, sem esperar a mudança de status. */
    @Transactional
    public EncomendaResp baixarFicha(Long id) {
        Encomenda e = buscar(id);
        if (e.getProduto() == null) {
            throw new RegraNegocioException("Esta encomenda não tem ficha técnica.");
        }
        if (e.isMateriaisBaixados()) {
            throw new RegraNegocioException("Os materiais da ficha já foram baixados.");
        }
        consumos.baixarFicha(e);
        return paraResp(repo.save(e));
    }

    @Transactional
    public void excluir(Long id) {
        Encomenda e = buscar(id);
        if (!lancamentos.findByEncomendaIdOrderByDataAsc(id).isEmpty()) {
            throw new RegraNegocioException("Esta encomenda tem pagamentos registrados. Cancele em vez de excluir.");
        }
        if (fotos.countByEncomendaId(id) > 0 || artes.countByEncomendaId(id) > 0 || compromissos.existsByEncomendaId(id)) {
            throw new RegraNegocioException("Esta encomenda tem fotos, artes ou compromissos na agenda. Cancele em vez de excluir.");
        }
        consumos.estornarTudo(id);
        repo.delete(e);
    }

    Encomenda buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Encomenda"));
    }

    private void preencher(Encomenda e, EncomendaReq r) {
        BigDecimal sinal = r.valorSinal() == null ? BigDecimal.ZERO : r.valorSinal();
        if (sinal.compareTo(r.valorTotal()) > 0) {
            throw new RegraNegocioException("O sinal não pode ser maior que o valor total.");
        }
        Produto novo = r.produtoId() == null ? null
                : produtos.findById(r.produtoId()).orElseThrow(() -> new NaoEncontradoException("Peça"));
        int qtd = r.quantidade() == null ? 1 : r.quantidade();
        if (e.isMateriaisBaixados() && e.getProduto() != null
                && (novo == null || !novo.getId().equals(e.getProduto().getId()) || qtd != e.getQuantidade())) {
            throw new RegraNegocioException("Os materiais da ficha já foram baixados. Para trocar a ficha ou a "
                    + "quantidade, remova antes os materiais usados na encomenda.");
        }
        e.setCliente(clientes.buscar(r.clienteId()));
        e.setDescricao(r.descricao().trim());
        e.setFrente(r.frente());
        e.setProduto(novo);
        e.setQuantidade(qtd);
        e.setHorasTrabalhadas(r.horasTrabalhadas());
        e.setValorTotal(r.valorTotal());
        e.setValorSinal(sinal);
        e.setPrazoEntrega(r.prazoEntrega());
        e.setObservacoes(Texto.limpar(r.observacoes()));
        e.setPersonalizacao(Texto.limpar(r.personalizacao()));
        definirSeTemArte(e, r.precisaArte());
    }

    /** Liga ou desliga a etapa de arte. Depois de enviada uma versão, o andamento segue pelas artes. */
    static void definirSeTemArte(Encomenda e, Boolean precisaArte) {
        if (precisaArte == null) {
            return;
        }
        if (precisaArte && e.getStatusArte() == StatusArte.SEM_ARTE) {
            e.setStatusArte(StatusArte.A_CRIAR);
        } else if (!precisaArte && e.getStatusArte() == StatusArte.A_CRIAR) {
            e.setStatusArte(StatusArte.SEM_ARTE);
        }
    }

    public static CategoriaLancamento categoria(Frente f) {
        return switch (f) {
            case PAPELARIA -> CategoriaLancamento.PAPELARIA;
            case PORCELANA -> CategoriaLancamento.PORCELANA;
            case PRESENTE -> CategoriaLancamento.PRESENTE;
        };
    }

    /** Custos reais (ou estimados pela ficha, se ainda não baixou) e quanto sobrou. */
    private CustosEncomenda custos(Encomenda e) {
        var par = precificacao.parametros();
        Produto p = e.getProduto();
        BigDecimal qtd = BigDecimal.valueOf(e.getQuantidade());
        BigDecimal materiais = e.getId() == null ? BigDecimal.ZERO : consumos.custoDaEncomenda(e.getId());
        boolean estimado = false;
        if (p != null && !e.isMateriaisBaixados()) {
            materiais = materiais.add(PrecificacaoService.custoMateriais(p).multiply(qtd));
            estimado = true;
        }
        BigDecimal horas = e.getHorasTrabalhadas() != null ? e.getHorasTrabalhadas()
                : p != null ? p.getHorasMaoObra().multiply(qtd) : BigDecimal.ZERO;
        BigDecimal fixo = horas.multiply(par.custoFixoHora()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal outros = p != null ? p.getOutrosCustos().multiply(qtd) : BigDecimal.ZERO;
        BigDecimal ganho = e.getValorTotal().subtract(materiais).subtract(fixo).subtract(outros);
        BigDecimal porHora = horas.signum() > 0 ? ganho.divide(horas, 2, RoundingMode.HALF_UP) : null;
        BigDecimal lucro = ganho.subtract(horas.multiply(par.valorHora())).setScale(2, RoundingMode.HALF_UP);
        return new CustosEncomenda(materiais.setScale(2, RoundingMode.HALF_UP), horas, fixo, outros, ganho,
                porHora, par.valorHora(), lucro, estimado);
    }

    public EncomendaResp paraResp(Encomenda e) {
        BigDecimal pago = e.getId() == null ? BigDecimal.ZERO : lancamentos.totalPagoEncomenda(e.getId());
        BigDecimal saldo = e.getStatus() == StatusEncomenda.CANCELADA
                ? BigDecimal.ZERO : e.getValorTotal().subtract(pago).max(BigDecimal.ZERO);
        var c = e.getCliente();
        var p = e.getProduto();
        return new EncomendaResp(e.getId(), c.getId(), c.getNome(), c.getWhatsapp(),
                e.getOrcamento() == null ? null : e.getOrcamento().getId(),
                e.getDescricao(), e.getFrente(), p == null ? null : p.getId(), p == null ? null : p.getNome(),
                e.getQuantidade(), e.getHorasTrabalhadas(), e.isMateriaisBaixados(),
                e.getValorTotal(), e.getValorSinal(), pago, saldo,
                e.getPrazoEntrega(), e.getStatus(), e.getObservacoes(), e.getCriadoEm(), custos(e), e.getEntregueEm(),
                e.getPersonalizacao(), e.getStatusArte(), e.getId() == null ? 0 : (int) artes.countByEncomendaId(e.getId()));
    }
}
