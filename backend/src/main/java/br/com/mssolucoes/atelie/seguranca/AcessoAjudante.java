package br.com.mssolucoes.atelie.seguranca;

import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Set;

/**
 * O que a ajudante pode fazer: acompanhar e produzir encomendas, enviar e acompanhar as artes, usar material,
 * ver a agenda e vender no caixa. Financeiro, relatórios, compras, contas, orçamentos,
 * configurações e equipe ficam só com a administradora.
 */
public final class AcessoAjudante {

    private AcessoAjudante() {
    }

    public record Rota(HttpMethod metodo, String caminho) {
    }

    public static final List<Rota> ROTAS = List.of(
            new Rota(HttpMethod.GET, "/api/encomendas"),
            new Rota(HttpMethod.GET, "/api/encomendas/*"),
            new Rota(HttpMethod.PATCH, "/api/encomendas/*/status"),
            new Rota(HttpMethod.GET, "/api/encomendas/*/consumos"),
            new Rota(HttpMethod.POST, "/api/encomendas/*/consumos"),
            new Rota(HttpMethod.POST, "/api/encomendas/*/baixar-ficha"),
            new Rota(HttpMethod.DELETE, "/api/consumos/*"),
            new Rota(HttpMethod.GET, "/api/encomendas/*/fotos"),
            new Rota(HttpMethod.POST, "/api/encomendas/*/fotos"),
            new Rota(HttpMethod.GET, "/api/fotos/*/arquivo"),
            new Rota(HttpMethod.GET, "/api/agenda"),
            new Rota(HttpMethod.GET, "/api/compromissos/*"),
            new Rota(HttpMethod.POST, "/api/compromissos"),
            new Rota(HttpMethod.PUT, "/api/compromissos/*"),
            new Rota(HttpMethod.DELETE, "/api/compromissos/*"),
            new Rota(HttpMethod.GET, "/api/clientes"),
            new Rota(HttpMethod.GET, "/api/materiais"),
            new Rota(HttpMethod.POST, "/api/materiais/*/ajuste"),
            new Rota(HttpMethod.GET, "/api/produtos"),
            new Rota(HttpMethod.POST, "/api/produtos/*/producao"),
            new Rota(HttpMethod.POST, "/api/vendas"),
            new Rota(HttpMethod.GET, "/api/artes/resumo"),
            new Rota(HttpMethod.GET, "/api/encomendas/*/artes"),
            new Rota(HttpMethod.POST, "/api/encomendas/*/artes"),
            new Rota(HttpMethod.PATCH, "/api/encomendas/*/arte"),
            new Rota(HttpMethod.DELETE, "/api/artes/*"),
            new Rota(HttpMethod.GET, "/api/artes/*/arquivo"));

    /**
     * Campos com valores em dinheiro (custos, valor cobrado, saldos, composição do preço) que a ajudante não vê.
     * O preço de venda das peças (precoAtual, precoVenda) continua, porque ela usa o caixa.
     */
    public static final Set<String> CAMPOS_OCULTOS = Set.of(
            "valorTotal", "valorSinal", "valorPago", "saldo", "custos", "custoUnitario", "custoTotal", "custoLinha",
            "outrosCustos", "margem", "lucro", "custoMateriais", "custoMaoObra", "custoFixo", "lucroUnitario",
            "ganhoPorHora", "precoSugerido", "desconto");

    /** "custo" é um número na venda (custo dos materiais, oculto) e um objeto na peça (fica, sem os custos). */
    public static final String CUSTO = "custo";
}
