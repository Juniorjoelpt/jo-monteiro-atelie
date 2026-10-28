package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Enums.CategoriaLancamento;
import br.com.mssolucoes.atelie.dominio.Enums.FormaPagamento;
import br.com.mssolucoes.atelie.dominio.Enums.TipoLancamento;
import br.com.mssolucoes.atelie.dominio.Lancamento;
import br.com.mssolucoes.atelie.dominio.Material;
import br.com.mssolucoes.atelie.dto.Dtos.AjusteEstoqueReq;
import br.com.mssolucoes.atelie.dto.Dtos.MaterialReq;
import br.com.mssolucoes.atelie.dto.Dtos.MaterialResp;
import br.com.mssolucoes.atelie.repositorio.LancamentoRepository;
import br.com.mssolucoes.atelie.repositorio.MaterialRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MaterialService {

    private final MaterialRepository repo;
    private final LancamentoRepository lancamentos;
    private final br.com.mssolucoes.atelie.repositorio.ProdutoRepository produtos;
    private final br.com.mssolucoes.atelie.repositorio.ConsumoMaterialRepository consumos;
    private final br.com.mssolucoes.atelie.repositorio.CompraItemRepository itensCompra;
    private final CompraService compras;

    public MaterialService(MaterialRepository repo, LancamentoRepository lancamentos,
                           br.com.mssolucoes.atelie.repositorio.ProdutoRepository produtos,
                           br.com.mssolucoes.atelie.repositorio.ConsumoMaterialRepository consumos,
                           br.com.mssolucoes.atelie.repositorio.CompraItemRepository itensCompra,
                           CompraService compras) {
        this.repo = repo;
        this.lancamentos = lancamentos;
        this.produtos = produtos;
        this.consumos = consumos;
        this.itensCompra = itensCompra;
        this.compras = compras;
    }

    @Transactional(readOnly = true)
    public List<MaterialResp> listar() {
        return repo.findAllByOrderByNomeAsc().stream().map(MaterialService::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialResp> abaixoDoMinimo() {
        return repo.findAllByOrderByNomeAsc().stream().filter(Material::isAbaixoDoMinimo)
                .map(MaterialService::paraResp).toList();
    }

    @Transactional
    public MaterialResp criar(MaterialReq r) {
        Material m = new Material();
        preencher(m, r);
        return paraResp(repo.save(m));
    }

    @Transactional
    public MaterialResp atualizar(Long id, MaterialReq r) {
        Material m = buscar(id);
        preencher(m, r);
        return paraResp(repo.save(m));
    }

    @Transactional
    public MaterialResp ajustar(Long id, AjusteEstoqueReq r) {
        Material m = buscar(id);
        if (r.tipo() == TipoLancamento.SAIDA) {
            if (r.quantidade().compareTo(m.getQuantidade()) > 0) {
                throw new RegraNegocioException("Só há " + m.getQuantidade().stripTrailingZeros().toPlainString()
                        + " " + m.getUnidade() + " em estoque.");
            }
            m.setQuantidade(m.getQuantidade().subtract(r.quantidade()));
        } else if (r.valorCompra() != null && r.valorCompra().signum() > 0) {
            // Compra com valor: entra no histórico de preços (e no fornecedor, se escolhido)
            compras.registrar(new br.com.mssolucoes.atelie.dto.Dtos.CompraReq(r.fornecedorId(), LocalDate.now(),
                    BigDecimal.ZERO, FormaPagamento.PIX, null, r.registrarDespesa(),
                    List.of(new br.com.mssolucoes.atelie.dto.Dtos.CompraItemReq(m.getId(), r.quantidade(), r.valorCompra()))));
            return paraResp(buscar(id));
        } else {
            m.setQuantidade(m.getQuantidade().add(r.quantidade()));
            boolean informouValor = r.valorCompra() != null && r.valorCompra().signum() > 0;
            BigDecimal valor = informouValor
                    ? r.valorCompra() : m.getCustoUnitario().multiply(r.quantidade()).setScale(2, java.math.RoundingMode.HALF_UP);
            if (informouValor) {
                // O custo passa a ser o da última compra: as fichas técnicas se atualizam sozinhas
                m.setCustoUnitario(r.valorCompra().divide(r.quantidade(), 4, java.math.RoundingMode.HALF_UP));
            }
            if (r.registrarDespesa() && valor.signum() > 0) {
                Lancamento l = new Lancamento();
                l.setTipo(TipoLancamento.SAIDA);
                l.setCategoria(CategoriaLancamento.MATERIAL);
                l.setDescricao("Compra — " + m.getNome() + " ("
                        + r.quantidade().stripTrailingZeros().toPlainString() + " " + m.getUnidade() + ")");
                l.setValor(valor);
                l.setData(LocalDate.now());
                l.setFormaPagamento(FormaPagamento.PIX);
                lancamentos.save(l);
            }
        }
        return paraResp(repo.save(m));
    }

    @Transactional
    public void excluir(Long id) {
        Material m = buscar(id);
        if (produtos.existsByMateriaisMaterialId(id)) {
            throw new RegraNegocioException("Este material está em fichas técnicas. Tire-o das fichas antes de excluir.");
        }
        if (consumos.existsByMaterialId(id) || itensCompra.existsByMaterialId(id)) {
            throw new RegraNegocioException("Este material já foi comprado ou usado e faz parte do histórico.");
        }
        repo.delete(m);
    }

    private Material buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Material"));
    }

    private void preencher(Material m, MaterialReq r) {
        m.setNome(r.nome().trim());
        m.setUnidade(r.unidade().trim());
        m.setQuantidade(r.quantidade());
        m.setEstoqueMinimo(r.estoqueMinimo());
        m.setCustoUnitario(r.custoUnitario());
    }

    public static MaterialResp paraResp(Material m) {
        return new MaterialResp(m.getId(), m.getNome(), m.getUnidade(), m.getQuantidade(), m.getEstoqueMinimo(),
                m.getCustoUnitario(), m.isAbaixoDoMinimo());
    }
}
