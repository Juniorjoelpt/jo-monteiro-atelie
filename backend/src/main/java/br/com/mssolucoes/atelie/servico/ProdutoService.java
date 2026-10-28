package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Material;
import br.com.mssolucoes.atelie.dominio.Produto;
import br.com.mssolucoes.atelie.dominio.ProdutoMaterial;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.ConsumoMaterialRepository;
import br.com.mssolucoes.atelie.repositorio.EncomendaRepository;
import br.com.mssolucoes.atelie.repositorio.MaterialRepository;
import br.com.mssolucoes.atelie.repositorio.ProdutoRepository;
import br.com.mssolucoes.atelie.repositorio.VendaRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProdutoService {

    private final ProdutoRepository repo;
    private final MaterialRepository materiais;
    private final EncomendaRepository encomendas;
    private final VendaRepository vendas;
    private final ConsumoMaterialRepository consumos;
    private final ConsumoService consumoService;
    private final PrecificacaoService precificacao;
    private final ArmazenamentoService armazenamento;

    public ProdutoService(ProdutoRepository repo, MaterialRepository materiais, EncomendaRepository encomendas,
                          VendaRepository vendas, ConsumoMaterialRepository consumos, ConsumoService consumoService,
                          PrecificacaoService precificacao, ArmazenamentoService armazenamento) {
        this.armazenamento = armazenamento;
        this.repo = repo;
        this.materiais = materiais;
        this.encomendas = encomendas;
        this.vendas = vendas;
        this.consumos = consumos;
        this.consumoService = consumoService;
        this.precificacao = precificacao;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResp> listar() {
        var par = precificacao.parametros();
        return repo.todosComMateriais().stream().map(p -> paraResp(p, par)).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResp detalhar(Long id) {
        return paraResp(buscar(id), precificacao.parametros());
    }

    @Transactional
    public ProdutoResp criar(ProdutoReq r) {
        Produto p = new Produto();
        preencher(p, r);
        return paraResp(repo.save(p), precificacao.parametros());
    }

    @Transactional
    public ProdutoResp atualizar(Long id, ProdutoReq r) {
        Produto p = buscar(id);
        preencher(p, r);
        return paraResp(repo.save(p), precificacao.parametros());
    }

    @Transactional
    public void excluir(Long id) {
        Produto p = buscar(id);
        if (encomendas.existsByProdutoId(id) || vendas.produtoJaVendido(id) || consumos.existsByProdutoId(id)) {
            throw new RegraNegocioException("Esta peça já tem encomendas, vendas ou produção registradas. "
                    + "Desmarque \"Ativa\" para tirá-la das listas sem perder o histórico.");
        }
        repo.delete(p);
        p.getFotos().forEach(f -> armazenamento.excluir(f.getArquivo()));
    }

    /** Peças de pronta-entrega que ficaram prontas: soma no estoque e baixa os materiais. */
    @Transactional
    public ProdutoResp produzir(Long id, int quantidade) {
        Produto p = buscar(id);
        if (!p.isProntaEntrega()) {
            throw new RegraNegocioException("Marque esta peça como pronta-entrega para controlar o estoque dela.");
        }
        consumoService.baixarProducao(p, quantidade);
        p.setEstoque(p.getEstoque() + quantidade);
        return paraResp(repo.save(p), precificacao.parametros());
    }

    public Produto buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Peça"));
    }

    private void preencher(Produto p, ProdutoReq r) {
        p.setNome(r.nome().trim());
        p.setDescricao(Texto.limpar(r.descricao()));
        p.setFrente(r.frente());
        p.setHorasMaoObra(r.horasMaoObra());
        p.setOutrosCustos(r.outrosCustos() == null ? BigDecimal.ZERO : r.outrosCustos());
        p.setMargem(r.margem());
        p.setPrecoVenda(r.precoVenda() == null || r.precoVenda().signum() == 0 ? null : r.precoVenda());
        p.setProntaEntrega(Boolean.TRUE.equals(r.prontaEntrega()));
        if (r.estoque() != null) {
            p.setEstoque(r.estoque());
        }
        p.setAtivo(r.ativo() == null || r.ativo());

        // Junta linhas repetidas do mesmo material
        Map<Long, BigDecimal> receita = new LinkedHashMap<>();
        if (r.materiais() != null) {
            r.materiais().forEach(i -> receita.merge(i.materialId(), i.quantidade(), BigDecimal::add));
        }
        p.getMateriais().removeIf(pm -> !receita.containsKey(pm.getMaterial().getId()));
        for (var item : receita.entrySet()) {
            ProdutoMaterial linha = p.getMateriais().stream()
                    .filter(pm -> pm.getMaterial().getId().equals(item.getKey())).findFirst().orElse(null);
            if (linha == null) {
                Material m = materiais.findById(item.getKey()).orElseThrow(() -> new NaoEncontradoException("Material"));
                linha = new ProdutoMaterial();
                linha.setProduto(p);
                linha.setMaterial(m);
                p.getMateriais().add(linha);
            }
            linha.setQuantidade(item.getValue());
        }
    }

    public ProdutoResp paraResp(Produto p, PrecificacaoService.Parametros par) {
        List<ItemFichaResp> itens = p.getMateriais().stream().map(pm -> {
            Material m = pm.getMaterial();
            return new ItemFichaResp(m.getId(), m.getNome(), m.getUnidade(), pm.getQuantidade(), m.getCustoUnitario(),
                    pm.getQuantidade().multiply(m.getCustoUnitario()).setScale(2, RoundingMode.HALF_UP));
        }).toList();
        return new ProdutoResp(p.getId(), p.getNome(), p.getDescricao(), p.getFrente(), p.getHorasMaoObra(),
                p.getOutrosCustos(), p.getMargem(), p.getPrecoVenda(), p.isProntaEntrega(), p.getEstoque(),
                p.isAtivo(), itens, precificacao.calcular(p, par), catalogo(p));
    }

    public static CatalogoInfo catalogo(Produto p) {
        return new CatalogoInfo(p.isNoCatalogo(), p.isMostrarPreco(), p.getTextoCatalogo(), p.getPrazoProducao(),
                p.getOrdemCatalogo(), p.getFotos().stream()
                .map(f -> new FotoProdutoResp(f.getId(), "/uploads/" + f.getArquivo(), f.getOrdem())).toList());
    }
}
