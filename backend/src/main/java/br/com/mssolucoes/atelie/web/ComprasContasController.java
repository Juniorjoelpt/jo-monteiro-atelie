package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.servico.CompraService;
import br.com.mssolucoes.atelie.servico.ContaFixaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Fornecedores, compras de material e contas fixas (só a administradora). */
@RestController
@RequestMapping("/api")
public class ComprasContasController {

    private final CompraService compras;
    private final ContaFixaService contas;

    public ComprasContasController(CompraService compras, ContaFixaService contas) {
        this.compras = compras;
        this.contas = contas;
    }

    // ---------- Fornecedores ----------

    @GetMapping("/fornecedores")
    public List<FornecedorResp> fornecedores() {
        return compras.listarFornecedores();
    }

    @GetMapping("/fornecedores/{id}/compras")
    public List<CompraResp> comprasDoFornecedor(@PathVariable Long id) {
        return compras.comprasDoFornecedor(id);
    }

    @PostMapping("/fornecedores")
    @ResponseStatus(HttpStatus.CREATED)
    public FornecedorResp criarFornecedor(@Valid @RequestBody FornecedorReq r) {
        return compras.criarFornecedor(r);
    }

    @PutMapping("/fornecedores/{id}")
    public FornecedorResp atualizarFornecedor(@PathVariable Long id, @Valid @RequestBody FornecedorReq r) {
        return compras.atualizarFornecedor(id, r);
    }

    @DeleteMapping("/fornecedores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirFornecedor(@PathVariable Long id) {
        compras.excluirFornecedor(id);
    }

    // ---------- Compras ----------

    @GetMapping("/compras")
    public ResumoCompras comprasDoMes(@RequestParam(required = false) Integer ano,
                                      @RequestParam(required = false) Integer mes) {
        LocalDate hoje = LocalDate.now();
        return compras.doMes(ano == null ? hoje.getYear() : ano, mes == null ? hoje.getMonthValue() : mes);
    }

    @GetMapping("/compras/{id}")
    public CompraResp compra(@PathVariable Long id) {
        return compras.detalhar(id);
    }

    @PostMapping("/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResp registrarCompra(@Valid @RequestBody CompraReq r) {
        return compras.registrar(r);
    }

    @PostMapping("/compras/{id}/cancelar")
    public CompraResp cancelarCompra(@PathVariable Long id) {
        return compras.cancelar(id);
    }

    @GetMapping("/materiais/{id}/precos")
    public HistoricoPrecos historicoPrecos(@PathVariable Long id) {
        return compras.historico(id);
    }

    // ---------- Contas fixas ----------

    @GetMapping("/contas-fixas")
    public ContasDoMes contasDoMes(@RequestParam(required = false) Integer ano,
                                   @RequestParam(required = false) Integer mes) {
        LocalDate hoje = LocalDate.now();
        return contas.doMes(ano == null ? hoje.getYear() : ano, mes == null ? hoje.getMonthValue() : mes);
    }

    @PostMapping("/contas-fixas")
    @ResponseStatus(HttpStatus.CREATED)
    public ContaFixaResp criarConta(@Valid @RequestBody ContaFixaReq r) {
        return contas.criar(r);
    }

    @PutMapping("/contas-fixas/{id}")
    public ContaFixaResp atualizarConta(@PathVariable Long id, @Valid @RequestBody ContaFixaReq r) {
        return contas.atualizar(id, r);
    }

    @DeleteMapping("/contas-fixas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirConta(@PathVariable Long id) {
        contas.excluir(id);
    }

    @PostMapping("/contas-fixas/{id}/pagar")
    public ContaDoMes pagarConta(@PathVariable Long id, @Valid @RequestBody PagarContaReq r) {
        return contas.pagar(id, r);
    }

    @DeleteMapping("/contas-fixas/{id}/pagamento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desfazerPagamento(@PathVariable Long id, @RequestParam int ano, @RequestParam int mes) {
        if (mes < 1 || mes > 12) {
            throw new RegraNegocioException("Mês inválido.");
        }
        contas.desfazerPagamento(id, ano, mes);
    }
}
