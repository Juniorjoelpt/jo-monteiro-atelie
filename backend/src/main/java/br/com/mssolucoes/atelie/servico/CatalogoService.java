package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Produto;
import br.com.mssolucoes.atelie.dominio.ProdutoFoto;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.ProdutoFotoRepository;
import br.com.mssolucoes.atelie.repositorio.ProdutoRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Catálogo do site: as peças das fichas técnicas que a Jô escolhe mostrar, com texto e fotos próprios.
 * O preço que aparece é o que ela cobra (ou "sob consulta"); custos nunca saem daqui.
 */
@Service
public class CatalogoService {

    public static final int MAX_FOTOS = 8;

    private final ProdutoRepository produtos;
    private final ProdutoFotoRepository fotos;
    private final ProdutoService produtoService;
    private final PrecificacaoService precificacao;
    private final ArmazenamentoService armazenamento;

    public CatalogoService(ProdutoRepository produtos, ProdutoFotoRepository fotos, ProdutoService produtoService,
                           PrecificacaoService precificacao, ArmazenamentoService armazenamento) {
        this.produtos = produtos;
        this.fotos = fotos;
        this.produtoService = produtoService;
        this.precificacao = precificacao;
        this.armazenamento = armazenamento;
    }

    @Transactional(readOnly = true)
    public List<ItemCatalogo> publico() {
        var par = precificacao.parametros();
        return produtos.todosComMateriais().stream()
                .filter(p -> p.isAtivo() && p.isNoCatalogo())
                .sorted(Comparator.comparingInt(Produto::getOrdemCatalogo).thenComparing(Produto::getNome))
                .map(p -> {
                    BigDecimal preco = p.isMostrarPreco() ? precificacao.calcular(p, par).precoAtual() : null;
                    return new ItemCatalogo(p.getId(), p.getNome(),
                            p.getTextoCatalogo() != null ? p.getTextoCatalogo() : null, p.getFrente(), preco,
                            p.isProntaEntrega(), p.isProntaEntrega() ? p.getEstoque() : 0, p.getPrazoProducao(),
                            p.getFotos().stream().map(f -> "/uploads/" + f.getArquivo()).toList());
                }).toList();
    }

    @Transactional
    public CatalogoInfo atualizar(Long produtoId, CatalogoReq r) {
        Produto p = produtoService.buscar(produtoId);
        p.setNoCatalogo(Boolean.TRUE.equals(r.noCatalogo()));
        p.setMostrarPreco(r.mostrarPreco() == null || r.mostrarPreco());
        p.setTextoCatalogo(Texto.limpar(r.textoCatalogo()));
        p.setPrazoProducao(Texto.limpar(r.prazoProducao()));
        p.setOrdemCatalogo(r.ordemCatalogo() == null ? 0 : r.ordemCatalogo());
        produtos.save(p);
        return ProdutoService.catalogo(p);
    }

    @Transactional
    public CatalogoInfo enviarFoto(Long produtoId, MultipartFile arquivo) {
        Produto p = produtoService.buscar(produtoId);
        if (p.getFotos().size() >= MAX_FOTOS) {
            throw new RegraNegocioException("Cada peça pode ter até " + MAX_FOTOS + " fotos. Tire uma antes de enviar outra.");
        }
        ProdutoFoto f = new ProdutoFoto();
        f.setProduto(p);
        f.setArquivo(armazenamento.salvarImagem(arquivo));
        f.setOrdem(p.getFotos().stream().mapToInt(ProdutoFoto::getOrdem).max().orElse(0) + 1);
        p.getFotos().add(f);
        produtos.save(p);
        return ProdutoService.catalogo(p);
    }

    /** Põe a foto como capa (primeira). */
    @Transactional
    public CatalogoInfo capa(Long fotoId) {
        ProdutoFoto escolhida = buscarFoto(fotoId);
        Produto p = escolhida.getProduto();
        int n = 2;
        for (ProdutoFoto f : p.getFotos()) {
            f.setOrdem(f == escolhida ? 1 : n++);
        }
        p.getFotos().sort(Comparator.comparingInt(ProdutoFoto::getOrdem));
        produtos.save(p);
        return ProdutoService.catalogo(p);
    }

    @Transactional
    public CatalogoInfo excluirFoto(Long fotoId) {
        ProdutoFoto f = buscarFoto(fotoId);
        Produto p = f.getProduto();
        p.getFotos().remove(f);
        produtos.save(p);
        armazenamento.excluir(f.getArquivo());
        return ProdutoService.catalogo(p);
    }

    private ProdutoFoto buscarFoto(Long id) {
        return fotos.findById(id).orElseThrow(() -> new NaoEncontradoException("Foto"));
    }
}
