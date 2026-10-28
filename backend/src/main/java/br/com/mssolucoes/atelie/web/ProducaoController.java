package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.servico.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Fichas técnicas, precificação, materiais usados nas encomendas e vendas de pronta-entrega (exigem login). */
@RestController
@RequestMapping("/api")
public class ProducaoController {

    private final ProdutoService produtos;
    private final PrecificacaoService precificacao;
    private final EncomendaService encomendas;
    private final ConsumoService consumos;
    private final VendaService vendas;

    public ProducaoController(ProdutoService produtos, PrecificacaoService precificacao, EncomendaService encomendas,
                              ConsumoService consumos, VendaService vendas) {
        this.produtos = produtos;
        this.precificacao = precificacao;
        this.encomendas = encomendas;
        this.consumos = consumos;
        this.vendas = vendas;
    }

    // ---------- Precificação ----------

    @GetMapping("/configuracoes/precificacao")
    public PrecificacaoResp precificacao() {
        return precificacao.resp();
    }

    @PutMapping("/configuracoes/precificacao")
    public PrecificacaoResp salvarPrecificacao(@Valid @RequestBody PrecificacaoReq r) {
        return precificacao.salvar(r);
    }

    // ---------- Produtos / fichas técnicas ----------

    @GetMapping("/produtos")
    public List<ProdutoResp> listarProdutos() {
        return produtos.listar();
    }

    @GetMapping("/produtos/{id}")
    public ProdutoResp produto(@PathVariable Long id) {
        return produtos.detalhar(id);
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResp criarProduto(@Valid @RequestBody ProdutoReq r) {
        return produtos.criar(r);
    }

    @PutMapping("/produtos/{id}")
    public ProdutoResp atualizarProduto(@PathVariable Long id, @Valid @RequestBody ProdutoReq r) {
        return produtos.atualizar(id, r);
    }

    @DeleteMapping("/produtos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirProduto(@PathVariable Long id) {
        produtos.excluir(id);
    }

    @PostMapping("/produtos/{id}/producao")
    public ProdutoResp produzir(@PathVariable Long id, @Valid @RequestBody ProducaoReq r) {
        return produtos.produzir(id, r.quantidade());
    }

    // ---------- Materiais usados na encomenda ----------

    @GetMapping("/encomendas/{id}/consumos")
    public List<ConsumoResp> consumosDaEncomenda(@PathVariable Long id) {
        return encomendas.consumos(id);
    }

    @PostMapping("/encomendas/{id}/consumos")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsumoResp registrarConsumo(@PathVariable Long id, @Valid @RequestBody ConsumoReq r) {
        return encomendas.registrarConsumo(id, r);
    }

    @PostMapping("/encomendas/{id}/baixar-ficha")
    public EncomendaResp baixarFicha(@PathVariable Long id) {
        return encomendas.baixarFicha(id);
    }

    @DeleteMapping("/consumos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void estornarConsumo(@PathVariable Long id) {
        consumos.estornar(id);
    }

    // ---------- Vendas ----------

    @GetMapping("/vendas")
    public ResumoVendas resumoVendas(@RequestParam(required = false) Integer ano,
                                     @RequestParam(required = false) Integer mes) {
        LocalDate hoje = LocalDate.now();
        return vendas.resumo(ano == null ? hoje.getYear() : ano, mes == null ? hoje.getMonthValue() : mes);
    }

    @PostMapping("/vendas")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResp registrarVenda(@Valid @RequestBody VendaReq r) {
        return vendas.registrar(r);
    }

    @PostMapping("/vendas/{id}/cancelar")
    public VendaResp cancelarVenda(@PathVariable Long id) {
        return vendas.cancelar(id);
    }
}
