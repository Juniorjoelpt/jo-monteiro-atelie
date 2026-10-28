package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Configuracao;
import br.com.mssolucoes.atelie.dominio.Produto;
import br.com.mssolucoes.atelie.dominio.ProdutoMaterial;
import br.com.mssolucoes.atelie.dto.Dtos.CustoFicha;
import br.com.mssolucoes.atelie.dto.Dtos.PrecificacaoReq;
import br.com.mssolucoes.atelie.dto.Dtos.PrecificacaoResp;
import br.com.mssolucoes.atelie.repositorio.ConfiguracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Método de precificação de artesanato:
 * preço = (materiais + horas × valor da hora + horas × custo fixo por hora + outros) × (1 + margem).
 * Valor da hora = salário desejado ÷ horas de produção no mês.
 * Custo fixo por hora = custos fixos do mês ÷ horas de produção no mês.
 */
@Service
public class PrecificacaoService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final ConfiguracaoService configuracao;
    private final ConfiguracaoRepository repo;
    private final ContaFixaService contas;

    public PrecificacaoService(ConfiguracaoService configuracao, ConfiguracaoRepository repo, ContaFixaService contas) {
        this.configuracao = configuracao;
        this.repo = repo;
        this.contas = contas;
    }

    /** Parâmetros já calculados, lidos uma vez por requisição. */
    public record Parametros(BigDecimal valorHora, BigDecimal custoFixoHora, BigDecimal margemPadrao) {
    }

    @Transactional(readOnly = true)
    public Parametros parametros() {
        Configuracao c = configuracao.atual();
        BigDecimal horas = BigDecimal.valueOf(Math.max(1, c.getHorasMes()));
        return new Parametros(
                c.getSalarioDesejado().divide(horas, 2, RoundingMode.HALF_UP),
                c.getCustosFixosMensais().divide(horas, 2, RoundingMode.HALF_UP),
                c.getMargemPadrao());
    }

    @Transactional(readOnly = true)
    public PrecificacaoResp resp() {
        return paraResp(configuracao.atual(), contas.totalPrecificacao());
    }

    @Transactional
    public PrecificacaoResp salvar(PrecificacaoReq r) {
        Configuracao c = repo.findById(Configuracao.ID_UNICO).orElseGet(ConfiguracaoService::padrao);
        c.setSalarioDesejado(r.salarioDesejado());
        c.setHorasMes(r.horasMes());
        BigDecimal totalContas = contas.totalPrecificacao();
        c.setCustosFixosAutomatico(Boolean.TRUE.equals(r.custosFixosAutomatico()));
        c.setCustosFixosMensais(c.isCustosFixosAutomatico() ? totalContas : r.custosFixosMensais());
        c.setMargemPadrao(r.margemPadrao());
        c.setAtualizadoEm(LocalDateTime.now());
        return paraResp(repo.save(c), totalContas);
    }

    /** Custo dos materiais de UMA peça, pela ficha, com o preço atual de cada material. */
    public static BigDecimal custoMateriais(Produto p) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ProdutoMaterial pm : p.getMateriais()) {
            soma = soma.add(pm.getQuantidade().multiply(pm.getMaterial().getCustoUnitario()));
        }
        return soma.setScale(2, RoundingMode.HALF_UP);
    }

    public CustoFicha calcular(Produto p, Parametros par) {
        BigDecimal materiais = custoMateriais(p);
        BigDecimal horas = p.getHorasMaoObra();
        BigDecimal maoObra = horas.multiply(par.valorHora()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fixo = horas.multiply(par.custoFixoHora()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal outros = p.getOutrosCustos();
        BigDecimal total = materiais.add(maoObra).add(fixo).add(outros);
        BigDecimal margem = p.getMargem() != null ? p.getMargem() : par.margemPadrao();

        // Arredonda para cima no real inteiro: R$ 87,32 vira R$ 88,00
        BigDecimal sugerido = total.multiply(BigDecimal.ONE.add(margem.divide(CEM, 6, RoundingMode.HALF_UP)))
                .setScale(0, RoundingMode.CEILING).setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal atual = p.getPrecoVenda() != null ? p.getPrecoVenda() : sugerido;
        BigDecimal lucro = atual.subtract(total);
        BigDecimal porHora = horas.signum() > 0
                ? atual.subtract(materiais).subtract(fixo).subtract(outros).divide(horas, 2, RoundingMode.HALF_UP)
                : null;
        return new CustoFicha(materiais, maoObra, fixo, outros, total, margem, sugerido, atual, lucro, porHora);
    }

    private static PrecificacaoResp paraResp(Configuracao c, BigDecimal totalContas) {
        BigDecimal horas = BigDecimal.valueOf(Math.max(1, c.getHorasMes()));
        return new PrecificacaoResp(c.getSalarioDesejado(), c.getHorasMes(), c.getCustosFixosMensais(),
                c.getMargemPadrao(),
                c.getSalarioDesejado().divide(horas, 2, RoundingMode.HALF_UP),
                c.getCustosFixosMensais().divide(horas, 2, RoundingMode.HALF_UP),
                c.getSalarioDesejado().signum() > 0, c.isCustosFixosAutomatico(), totalContas);
    }
}
