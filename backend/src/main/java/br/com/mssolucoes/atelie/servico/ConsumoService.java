package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.ConsumoMaterial;
import br.com.mssolucoes.atelie.dominio.Encomenda;
import br.com.mssolucoes.atelie.dominio.Enums.OrigemConsumo;
import br.com.mssolucoes.atelie.dominio.Enums.StatusEncomenda;
import br.com.mssolucoes.atelie.dominio.Material;
import br.com.mssolucoes.atelie.dominio.Produto;
import br.com.mssolucoes.atelie.dominio.ProdutoMaterial;
import br.com.mssolucoes.atelie.dto.Dtos.ConsumoResp;
import br.com.mssolucoes.atelie.repositorio.ConsumoMaterialRepository;
import br.com.mssolucoes.atelie.repositorio.MaterialRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Saídas de material do estoque. Nunca bloqueia a produção por falta de saldo:
 * se o estoque ficar negativo, o material aparece como "acabando" para ela registrar a compra.
 */
@Service
public class ConsumoService {

    /** A partir destes status a peça já está sendo feita: hora de baixar os materiais da ficha. */
    public static final Set<StatusEncomenda> EM_PRODUCAO_OU_DEPOIS =
            EnumSet.of(StatusEncomenda.EM_PRODUCAO, StatusEncomenda.PRONTA, StatusEncomenda.ENTREGUE);

    private final ConsumoMaterialRepository repo;
    private final MaterialRepository materiais;

    public ConsumoService(ConsumoMaterialRepository repo, MaterialRepository materiais) {
        this.repo = repo;
        this.materiais = materiais;
    }

    ConsumoMaterial baixar(Material m, BigDecimal quantidade, OrigemConsumo origem, Encomenda e, Produto p) {
        m.setQuantidade(m.getQuantidade().subtract(quantidade));
        materiais.save(m);
        ConsumoMaterial c = new ConsumoMaterial();
        c.setMaterial(m);
        c.setQuantidade(quantidade);
        c.setCustoUnitario(m.getCustoUnitario());
        c.setCustoTotal(quantidade.multiply(m.getCustoUnitario()).setScale(2, RoundingMode.HALF_UP));
        c.setOrigem(origem);
        c.setEncomenda(e);
        c.setProduto(p);
        return repo.save(c);
    }

    /** Baixa a receita da ficha × quantidade, uma única vez por encomenda. */
    @Transactional
    public boolean baixarFicha(Encomenda e) {
        if (e.getProduto() == null || e.isMateriaisBaixados() || e.getStatus() == StatusEncomenda.CANCELADA) {
            return false;
        }
        BigDecimal qtd = BigDecimal.valueOf(e.getQuantidade());
        for (ProdutoMaterial pm : e.getProduto().getMateriais()) {
            baixar(pm.getMaterial(), pm.getQuantidade().multiply(qtd), OrigemConsumo.FICHA_ENCOMENDA, e, null);
        }
        e.setMateriaisBaixados(true);
        return true;
    }

    /** Chamado sempre que a encomenda muda de status. */
    @Transactional
    public void aposMudarStatus(Encomenda e) {
        if (EM_PRODUCAO_OU_DEPOIS.contains(e.getStatus())) {
            baixarFicha(e);
        }
    }

    /** Peças de pronta-entrega produzidas: baixa a receita × quantidade. */
    @Transactional
    public void baixarProducao(Produto p, int quantidade) {
        BigDecimal qtd = BigDecimal.valueOf(quantidade);
        for (ProdutoMaterial pm : p.getMateriais()) {
            baixar(pm.getMaterial(), pm.getQuantidade().multiply(qtd), OrigemConsumo.PRODUCAO_ESTOQUE, null, p);
        }
    }

    @Transactional
    public ConsumoResp registrarManual(Encomenda e, Long materialId, BigDecimal quantidade) {
        if (e.getStatus() == StatusEncomenda.CANCELADA) {
            throw new RegraNegocioException("Esta encomenda está cancelada.");
        }
        Material m = materiais.findById(materialId).orElseThrow(() -> new NaoEncontradoException("Material"));
        return paraResp(baixar(m, quantidade, OrigemConsumo.MANUAL_ENCOMENDA, e, null));
    }

    /** Desfaz uma saída: o material volta para o estoque. */
    @Transactional
    public void estornar(Long consumoId) {
        ConsumoMaterial c = repo.findById(consumoId).orElseThrow(() -> new NaoEncontradoException("Registro de material"));
        if (c.getEncomenda() == null) {
            throw new RegraNegocioException("Este registro é de produção para o estoque e não pode ser desfeito aqui.");
        }
        devolver(c);
    }

    /** Devolve tudo o que a encomenda consumiu (usado ao excluir uma encomenda lançada por engano). */
    @Transactional
    public void estornarTudo(Long encomendaId) {
        repo.findByEncomendaIdOrderByIdAsc(encomendaId).forEach(this::devolver);
    }

    private void devolver(ConsumoMaterial c) {
        Material m = c.getMaterial();
        m.setQuantidade(m.getQuantidade().add(c.getQuantidade()));
        materiais.save(m);
        if (c.getOrigem() == OrigemConsumo.FICHA_ENCOMENDA && c.getEncomenda() != null) {
            // Se tirar uma linha da ficha, não baixa de novo automaticamente
            c.getEncomenda().setMateriaisBaixados(true);
        }
        repo.delete(c);
    }

    @Transactional(readOnly = true)
    public List<ConsumoResp> daEncomenda(Long encomendaId) {
        return repo.findByEncomendaIdOrderByIdAsc(encomendaId).stream().map(ConsumoService::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal custoDaEncomenda(Long encomendaId) {
        return repo.custoDaEncomenda(encomendaId);
    }

    public static ConsumoResp paraResp(ConsumoMaterial c) {
        Material m = c.getMaterial();
        return new ConsumoResp(c.getId(), m.getId(), m.getNome(), m.getUnidade(), c.getQuantidade(),
                c.getCustoUnitario(), c.getCustoTotal(), c.getOrigem(), c.getData());
    }
}
