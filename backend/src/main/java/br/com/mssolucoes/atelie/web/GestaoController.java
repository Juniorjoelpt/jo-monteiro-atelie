package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dominio.Enums.StatusEncomenda;
import br.com.mssolucoes.atelie.dominio.Enums.StatusOrcamento;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.servico.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.mssolucoes.atelie.seguranca.UsuarioLogado;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Rotas do sistema de gestão (exigem login). */
@RestController
@RequestMapping("/api")
public class GestaoController {

    private final PainelService painel;
    private final ClienteService clientes;
    private final OrcamentoService orcamentos;
    private final EncomendaService encomendas;
    private final FinanceiroService financeiro;
    private final MaterialService materiais;
    private final PortfolioService portfolio;
    private final ConfiguracaoService configuracao;

    public GestaoController(PainelService painel, ClienteService clientes, OrcamentoService orcamentos,
                            EncomendaService encomendas, FinanceiroService financeiro,
                            MaterialService materiais, PortfolioService portfolio, ConfiguracaoService configuracao) {
        this.painel = painel;
        this.clientes = clientes;
        this.orcamentos = orcamentos;
        this.encomendas = encomendas;
        this.financeiro = financeiro;
        this.materiais = materiais;
        this.portfolio = portfolio;
        this.configuracao = configuracao;
    }

    // ---------- Painel ----------

    @GetMapping("/painel")
    public Painel painel() {
        return painel.montar();
    }

    // ---------- Clientes ----------

    @GetMapping("/clientes")
    public List<ClienteResp> listarClientes(@RequestParam(required = false) String busca) {
        return clientes.listar(busca);
    }

    @GetMapping("/clientes/{id}")
    public ClienteDetalhe cliente(@PathVariable Long id) {
        var c = ClienteService.paraResp(clientes.buscar(id));
        List<EncomendaResp> enc = encomendas.doCliente(id);
        BigDecimal pago = enc.stream().map(EncomendaResp::valorPago).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ClienteDetalhe(c, orcamentos.doCliente(id), enc, pago);
    }

    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResp criarCliente(@Valid @RequestBody ClienteReq r) {
        return clientes.criar(r);
    }

    @PutMapping("/clientes/{id}")
    public ClienteResp atualizarCliente(@PathVariable Long id, @Valid @RequestBody ClienteReq r) {
        return clientes.atualizar(id, r);
    }

    @DeleteMapping("/clientes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirCliente(@PathVariable Long id) {
        clientes.excluir(id);
    }

    // ---------- Orçamentos ----------

    @GetMapping("/orcamentos")
    public List<OrcamentoResp> listarOrcamentos(@RequestParam(required = false) StatusOrcamento status) {
        return orcamentos.listar(status);
    }

    @GetMapping("/orcamentos/fotos/{id}/arquivo")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> fotoOrcamento(@PathVariable Long id) {
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(orcamentos.tipoMimeFoto(id)))
                .cacheControl(org.springframework.http.CacheControl.noStore().cachePrivate())
                .body(orcamentos.arquivoFoto(id));
    }

    @PostMapping("/orcamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResp criarOrcamento(@Valid @RequestBody OrcamentoReq r) {
        return orcamentos.criar(r);
    }

    @PutMapping("/orcamentos/{id}")
    public OrcamentoResp atualizarOrcamento(@PathVariable Long id, @Valid @RequestBody OrcamentoReq r) {
        return orcamentos.atualizar(id, r);
    }

    @PatchMapping("/orcamentos/{id}/status")
    public OrcamentoResp statusOrcamento(@PathVariable Long id, @Valid @RequestBody StatusOrcamentoReq r) {
        return orcamentos.mudarStatus(id, r.status());
    }

    @PostMapping("/orcamentos/{id}/aprovar")
    public EncomendaResp aprovarOrcamento(@PathVariable Long id, @Valid @RequestBody AprovarOrcamentoReq r) {
        return orcamentos.aprovar(id, r);
    }

    @DeleteMapping("/orcamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirOrcamento(@PathVariable Long id) {
        orcamentos.excluir(id);
    }

    // ---------- Encomendas ----------

    @GetMapping("/encomendas")
    public List<EncomendaResp> listarEncomendas(@RequestParam(required = false) StatusEncomenda status) {
        return encomendas.listar(status);
    }

    @GetMapping("/encomendas/{id}")
    public EncomendaResp encomenda(@PathVariable Long id) {
        return encomendas.detalhar(id);
    }

    @GetMapping("/encomendas/{id}/pagamentos")
    public List<LancamentoResp> pagamentos(@PathVariable Long id) {
        return encomendas.pagamentos(id);
    }

    @PostMapping("/encomendas")
    @ResponseStatus(HttpStatus.CREATED)
    public EncomendaResp criarEncomenda(@Valid @RequestBody EncomendaReq r) {
        return encomendas.criar(r);
    }

    @PutMapping("/encomendas/{id}")
    public EncomendaResp atualizarEncomenda(@PathVariable Long id, @Valid @RequestBody EncomendaReq r) {
        return encomendas.atualizar(id, r);
    }

    @PatchMapping("/encomendas/{id}/status")
    public EncomendaResp statusEncomenda(@PathVariable Long id, @Valid @RequestBody StatusEncomendaReq r,
                                         @AuthenticationPrincipal UsuarioLogado eu) {
        if (eu.ajudante() && r.status() != StatusEncomenda.PRONTA && r.status() != StatusEncomenda.ENTREGUE) {
            throw new AcessoNegadoException("A ajudante pode marcar a encomenda como pronta ou entregue.");
        }
        return encomendas.mudarStatus(id, r.status());
    }

    @PostMapping("/encomendas/{id}/pagamentos")
    public EncomendaResp registrarPagamento(@PathVariable Long id, @Valid @RequestBody PagamentoReq r) {
        return encomendas.registrarPagamento(id, r);
    }

    @DeleteMapping("/encomendas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirEncomenda(@PathVariable Long id) {
        encomendas.excluir(id);
    }

    // ---------- Financeiro ----------

    @GetMapping("/financeiro")
    public ResumoFinanceiro resumo(@RequestParam(required = false) Integer ano,
                                   @RequestParam(required = false) Integer mes) {
        LocalDate hoje = LocalDate.now();
        return financeiro.resumo(ano == null ? hoje.getYear() : ano, mes == null ? hoje.getMonthValue() : mes);
    }

    @PostMapping("/lancamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResp criarLancamento(@Valid @RequestBody LancamentoReq r) {
        return financeiro.criar(r);
    }

    @PutMapping("/lancamentos/{id}")
    public LancamentoResp atualizarLancamento(@PathVariable Long id, @Valid @RequestBody LancamentoReq r) {
        return financeiro.atualizar(id, r);
    }

    @DeleteMapping("/lancamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirLancamento(@PathVariable Long id) {
        financeiro.excluir(id);
    }

    // ---------- Materiais ----------

    @GetMapping("/materiais")
    public List<MaterialResp> listarMateriais() {
        return materiais.listar();
    }

    @PostMapping("/materiais")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResp criarMaterial(@Valid @RequestBody MaterialReq r) {
        return materiais.criar(r);
    }

    @PutMapping("/materiais/{id}")
    public MaterialResp atualizarMaterial(@PathVariable Long id, @Valid @RequestBody MaterialReq r) {
        return materiais.atualizar(id, r);
    }

    @PostMapping("/materiais/{id}/ajuste")
    public MaterialResp ajustarMaterial(@PathVariable Long id, @Valid @RequestBody AjusteEstoqueReq r,
                                        @AuthenticationPrincipal UsuarioLogado eu) {
        if (eu.ajudante() && r.tipo() != br.com.mssolucoes.atelie.dominio.Enums.TipoLancamento.SAIDA) {
            throw new AcessoNegadoException("Compras de material são registradas pela administradora.");
        }
        return materiais.ajustar(id, r);
    }

    @DeleteMapping("/materiais/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirMaterial(@PathVariable Long id) {
        materiais.excluir(id);
    }

    // ---------- Portfólio ----------

    @GetMapping("/portfolio")
    public List<PortfolioResp> listarPortfolio() {
        return portfolio.listar();
    }

    @PostMapping(value = "/portfolio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioResp enviarFoto(@Valid @RequestPart("dados") PortfolioReq dados,
                                    @RequestPart("foto") MultipartFile foto) {
        return portfolio.criar(dados, foto);
    }

    @PutMapping("/portfolio/{id}")
    public PortfolioResp atualizarFoto(@PathVariable Long id, @Valid @RequestBody PortfolioReq r) {
        return portfolio.atualizar(id, r);
    }

    @DeleteMapping("/portfolio/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirFoto(@PathVariable Long id) {
        portfolio.excluir(id);
    }

    // ---------- Configurações ----------

    @GetMapping("/configuracoes")
    public ConfiguracaoDto configuracoes() {
        return configuracao.dto();
    }

    @PutMapping("/configuracoes")
    public ConfiguracaoDto salvarConfiguracoes(@Valid @RequestBody ConfiguracaoDto r) {
        return configuracao.salvar(r);
    }
}
