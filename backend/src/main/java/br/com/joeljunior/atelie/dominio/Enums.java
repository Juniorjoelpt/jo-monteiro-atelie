package br.com.joeljunior.atelie.dominio;

/** Enumerações do domínio, agrupadas para facilitar a leitura. */
public final class Enums {

    private Enums() {
    }

    /** Escola e empresa costumam pedir em quantidade (kits, lembrancinhas, brindes). */
    public enum TipoCliente { CLIENTE, ESCOLA, EMPRESA, PARCEIRO }

    public enum StatusOrcamento { NOVO, RESPONDIDO, APROVADO, RECUSADO }

    public enum Origem { SITE, MANUAL }

    /** Frente de receita: papelaria (cadernetas, agendas, Bíblias), porcelana (canecas) e presentes (cestas, kits, garrafas). */
    public enum Frente { PAPELARIA, PORCELANA, PRESENTE }

    public enum StatusEncomenda { AGUARDANDO_SINAL, EM_PRODUCAO, PRONTA, ENTREGUE, CANCELADA }

    /**
     * Arte da peça personalizada.
     * SEM_ARTE = não precisa (pronta-entrega, peça lisa); A_CRIAR = a Jô ainda vai fazer;
     * AGUARDANDO_CLIENTE = enviada pelo link; AJUSTE_PEDIDO = a cliente pediu mudança; APROVADA = pode produzir.
     */
    public enum StatusArte { SEM_ARTE, A_CRIAR, AGUARDANDO_CLIENTE, AJUSTE_PEDIDO, APROVADA }

    /** Resposta da cliente a uma versão da arte. */
    public enum RespostaArte { APROVADA, AJUSTE }

    public enum TipoLancamento { ENTRADA, SAIDA }

    public enum CategoriaLancamento { PAPELARIA, PORCELANA, PRESENTE, VENDA, MATERIAL, CONTAS_FIXAS, OUTROS }

    /** De onde veio a saída de material. */
    public enum OrigemConsumo { FICHA_ENCOMENDA, MANUAL_ENCOMENDA, PRODUCAO_ESTOQUE }

    public enum CanalVenda { ATELIE, INSTAGRAM, WHATSAPP, FEIRA, OUTRO }

    public enum StatusVenda { CONCLUIDA, CANCELADA }

    public enum TipoCompromisso { ENTREGA, REUNIAO, PRODUCAO, COMPRA, OUTRO }

    /** Referência = o que a cliente mandou; pronta = foto da peça terminada. */
    public enum TipoFoto { REFERENCIA, PRONTA }

    public enum StatusCompra { CONCLUIDA, CANCELADA }

    /** ADMIN = dona do ateliê; AJUDANTE = produção, sem acesso a valores e configurações. */
    public enum Perfil { ADMIN, AJUDANTE }

    public enum FormaPagamento { PIX, DINHEIRO, CARTAO, OUTRO }

    public enum CategoriaPortfolio { PAPELARIA, CANECAS, PRESENTES, DATAS_ESPECIAIS }
}
