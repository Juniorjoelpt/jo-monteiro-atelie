package br.com.joeljunior.atelie.dto;

import br.com.joeljunior.atelie.dominio.Enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Objetos de entrada e saída da API, agrupados por módulo. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Autenticação ----------

    public record LoginReq(
            @NotBlank(message = "Informe o e-mail.") String email,
            @NotBlank(message = "Informe a senha.") String senha) {
    }

    public record LoginResp(String token, String nome, String email, Perfil perfil) {
    }

    public record TrocarSenhaReq(
            @NotBlank(message = "Informe a senha atual.") String senhaAtual,
            @NotBlank @Size(min = 8, message = "A nova senha precisa ter pelo menos 8 caracteres.") String novaSenha) {
    }

    // ---------- Configurações do ateliê ----------

    public record ConfiguracaoDto(
            @NotBlank(message = "Informe o nome do ateliê.") @Size(max = 120) String nomeAtelie,
            @NotBlank(message = "Informe o WhatsApp.") @Size(max = 20) String whatsapp,
            @Size(max = 80) String instagram,
            @Size(max = 140) String chavePix,
            @Size(max = 120) String titularPix,
            @Size(max = 255) String endereco,
            @Size(max = 255) String horario,
            @Size(max = 120) String cidade,
            @Size(max = 4000) String sobreTexto) {
    }

    // ---------- Clientes ----------

    public record ClienteReq(
            @NotBlank(message = "Informe o nome.") @Size(max = 140) String nome,
            @Size(max = 20) String whatsapp,
            @Email(message = "E-mail inválido.") @Size(max = 160) String email,
            @Size(max = 120) String cidade,
            LocalDate aniversario,
            TipoCliente tipo,
            @Size(max = 4000) String observacoes,
            @Size(max = 30) String documento,
            @Size(max = 255) String endereco) {
    }

    public record ClienteResp(Long id, String nome, String whatsapp, String email, String cidade,
                              LocalDate aniversario, TipoCliente tipo, String observacoes, LocalDateTime criadoEm,
                              String documento, String endereco) {
    }

    public record ClienteDetalhe(ClienteResp cliente, List<OrcamentoResp> orcamentos, List<EncomendaResp> encomendas,
                                 BigDecimal totalPago) {
    }

    // ---------- Orçamentos ----------

    public record OrcamentoPublicoReq(
            @NotBlank(message = "Informe seu nome.") @Size(max = 140) String nome,
            @NotBlank(message = "Informe seu WhatsApp.") @Size(max = 20) String whatsapp,
            @NotBlank(message = "Escolha o tipo de peça.") @Size(max = 80) String tipoPeca,
            /** Peça do catálogo (quando o pedido sai do botão "Pedir orçamento" de uma peça). */
            Long produtoId,
            /** Peças da sacola do site (várias de uma vez). */
            @Size(max = 30, message = "A sacola pode ter até 30 peças diferentes.") List<@jakarta.validation.Valid ItemSacolaReq> itens,
            @Min(value = 1, message = "A quantidade mínima é 1.") @Max(value = 5000, message = "Quantidade muito alta: fale com o ateliê pelo WhatsApp.") Integer quantidade,
            @Size(max = 1000, message = "Personalização de até 1000 caracteres.") String personalizacao,
            @Size(max = 160) String medidas,
            @Size(max = 160) String cores,
            LocalDate dataDesejada,
            @Size(max = 2000) String detalhes,
            /** Campo invisível no formulário: se vier preenchido, é robô. */
            String site) {
    }

    public record OrcamentoReq(
            Long clienteId,
            @NotBlank(message = "Informe o nome.") @Size(max = 140) String nome,
            @Size(max = 20) String whatsapp,
            @NotBlank(message = "Informe o tipo de peça.") @Size(max = 80) String tipoPeca,
            Long produtoId,
            @Min(value = 1, message = "A quantidade mínima é 1.") Integer quantidade,
            @Size(max = 2000) String personalizacao,
            @Size(max = 160) String medidas,
            @Size(max = 160) String cores,
            LocalDate dataDesejada,
            @Size(max = 4000) String detalhes,
            @PositiveOrZero(message = "Valor inválido.") BigDecimal valorProposto) {
    }

    public record OrcamentoResp(Long id, Long clienteId, String nome, String whatsapp, String tipoPeca, String medidas,
                                String cores, LocalDate dataDesejada, String detalhes, BigDecimal valorProposto,
                                StatusOrcamento status, Origem origem, LocalDateTime criadoEm, Long encomendaId,
                                Long produtoId, String produtoNome, int quantidade, String personalizacao,
                                List<FotoOrcamentoResp> fotos, List<ItemOrcamentoResp> itens) {
    }

    public record ItemSacolaReq(
            @NotNull(message = "Peça não informada.") Long produtoId,
            @NotNull @Min(value = 1, message = "A quantidade mínima é 1.") @Max(value = 5000, message = "Quantidade muito alta.") Integer quantidade) {
    }

    /** precoReferencia = preço do catálogo hoje (unitário), só para a Jô ter uma base; pode vir vazio. */
    public record ItemOrcamentoResp(Long produtoId, String nome, int quantidade, BigDecimal precoReferencia) {
    }

    public record FotoOrcamentoResp(Long id, String url) {
    }

    public record StatusOrcamentoReq(@NotNull(message = "Informe o status.") StatusOrcamento status) {
    }

    public record AprovarOrcamentoReq(
            @NotBlank(message = "Descreva a encomenda.") @Size(max = 200) String descricao,
            @NotNull(message = "Escolha a frente.") Frente frente,
            Long produtoId,
            @Min(value = 1, message = "A quantidade mínima é 1.") Integer quantidade,
            @NotNull(message = "Informe o valor total.") @PositiveOrZero BigDecimal valorTotal,
            @PositiveOrZero BigDecimal valorSinal,
            LocalDate prazoEntrega,
            @Size(max = 2000) String personalizacao,
            /** Peça personalizada que passa pela aprovação de arte (padrão: sim). */
            Boolean precisaArte) {
    }

    // ---------- Encomendas ----------

    public record EncomendaReq(
            @NotNull(message = "Escolha a cliente.") Long clienteId,
            @NotBlank(message = "Descreva a encomenda.") @Size(max = 200) String descricao,
            @NotNull(message = "Escolha a frente.") Frente frente,
            Long produtoId,
            @Min(value = 1, message = "A quantidade mínima é 1.") Integer quantidade,
            @PositiveOrZero(message = "Horas inválidas.") BigDecimal horasTrabalhadas,
            @NotNull(message = "Informe o valor total.") @PositiveOrZero BigDecimal valorTotal,
            @PositiveOrZero BigDecimal valorSinal,
            LocalDate prazoEntrega,
            StatusEncomenda status,
            @Size(max = 4000) String observacoes,
            @Size(max = 2000, message = "Personalização de até 2000 caracteres.") String personalizacao,
            Boolean precisaArte,
            Boolean urgente) {
    }

    public record UrgenteReq(@NotNull(message = "Informe se é urgente.") Boolean urgente) {
    }

    public record EncomendaResp(Long id, Long clienteId, String clienteNome, String clienteWhatsapp, Long orcamentoId,
                                String descricao, Frente frente, Long produtoId, String produtoNome, int quantidade,
                                BigDecimal horasTrabalhadas, boolean materiaisBaixados,
                                BigDecimal valorTotal, BigDecimal valorSinal,
                                BigDecimal valorPago, BigDecimal saldo, LocalDate prazoEntrega,
                                StatusEncomenda status, String observacoes, LocalDateTime criadoEm,
                                CustosEncomenda custos, LocalDate entregueEm,
                                String personalizacao, StatusArte statusArte, int versoesArte,
                                boolean urgente, String linkPedido, LocalDateTime acompanhadoEm,
                                LocalDateTime arteVistaEm) {
    }

    /**
     * Quanto a encomenda custou e quanto sobrou.
     * "ganho" = valor cobrado − materiais − custos fixos − outros (o que remunera o trabalho dela + lucro).
     * "estimado" = materiais ainda não baixados; o custo vem da ficha técnica.
     */
    public record CustosEncomenda(BigDecimal custoMateriais, BigDecimal horas, BigDecimal custoFixo,
                                  BigDecimal outrosCustos, BigDecimal ganho, BigDecimal ganhoPorHora,
                                  BigDecimal valorHoraDesejado, BigDecimal lucro, boolean estimado) {
    }

    public record ConsumoReq(
            @NotNull(message = "Escolha o material.") Long materialId,
            @NotNull @Positive(message = "A quantidade precisa ser maior que zero.") BigDecimal quantidade) {
    }

    public record ConsumoResp(Long id, Long materialId, String materialNome, String unidade, BigDecimal quantidade,
                              BigDecimal custoUnitario, BigDecimal custoTotal, OrigemConsumo origem, LocalDate data) {
    }

    public record StatusEncomendaReq(@NotNull(message = "Informe o status.") StatusEncomenda status) {
    }

    public record PagamentoReq(
            @NotNull(message = "Informe o valor.") @Positive(message = "O valor precisa ser maior que zero.") BigDecimal valor,
            LocalDate data,
            FormaPagamento formaPagamento,
            @Size(max = 200) String descricao) {
    }

    // ---------- Aprovação de arte ----------

    public record ArteVersaoResp(Long id, int numero, String url, String observacao, RespostaArte resposta,
                                 String comentarioCliente, boolean respondidaNoSistema, LocalDateTime respondidoEm,
                                 LocalDateTime criadoEm, LocalDateTime vistaEm, LocalDateTime vistaUltimaEm,
                                 int visualizacoes) {
    }

    public record ResumoArtes(long aFazer, long ajustesPedidos, long comCliente) {
    }

    /** link = página do pedido, em que a cliente acompanha o andamento e aprova a arte. */
    public record ArtesEncomenda(StatusArte status, String link, List<ArteVersaoResp> versoes) {
    }

    public record StatusArteReq(
            @NotNull(message = "Informe a situação da arte.") StatusArte status,
            @Size(max = 1000, message = "Comentário de até 1000 caracteres.") String comentario) {
    }

    public record VersaoPublica(Long id, int numero, String url, String observacao, RespostaArte resposta,
                                String comentarioCliente, LocalDateTime respondidoEm, LocalDateTime criadoEm,
                                boolean atual) {
    }

    public record ArtePublica(String atelie, String whatsappAtelie, String cliente, String descricao,
                              String personalizacao, StatusArte status, List<VersaoPublica> versoes,
                              AndamentoPublico andamento) {
    }

    /**
     * O que a cliente vê do andamento pelo link: etapa, data combinada e quanto falta pagar.
     * Sem telefone, sem custos. A chave Pix só vai quando há saldo.
     */
    public record AndamentoPublico(StatusEncomenda status, int quantidade, LocalDate prazoEntrega, LocalDate entregueEm,
                                   BigDecimal valorTotal, BigDecimal valorPago, BigDecimal saldo, BigDecimal valorSinal,
                                   String chavePix, String titularPix, LocalDateTime criadoEm) {
    }

    /** A página do pedido avisa qual versão da arte apareceu na tela. */
    public record VistoReq(Long versaoId) {
    }

    // ---------- Histórico ----------

    /** cliente = feito pela cliente pelo link. */
    public record HistoricoResp(Long id, AreaHistorico area, Long encomendaId, String encomenda, String autor,
                                boolean cliente, String acao, String descricao, LocalDateTime criadoEm) {
    }

    public record RespostaArteReq(
            @NotNull(message = "Versão da arte não informada.") Long versaoId,
            @NotNull(message = "Escolha aprovar ou pedir ajuste.") Boolean aprovar,
            @Size(max = 1000, message = "Use até 1000 caracteres.") String comentario,
            /** Campo invisível: se vier preenchido, é robô. */
            String site) {
    }

    // ---------- Financeiro ----------

    public record LancamentoReq(
            @NotNull(message = "Escolha entrada ou saída.") TipoLancamento tipo,
            @NotNull(message = "Escolha a categoria.") CategoriaLancamento categoria,
            @NotBlank(message = "Descreva o lançamento.") @Size(max = 200) String descricao,
            @NotNull(message = "Informe o valor.") @Positive(message = "O valor precisa ser maior que zero.") BigDecimal valor,
            @NotNull(message = "Informe a data.") LocalDate data,
            FormaPagamento formaPagamento) {
    }

    public record LancamentoResp(Long id, TipoLancamento tipo, CategoriaLancamento categoria, String descricao,
                                 BigDecimal valor, LocalDate data, FormaPagamento formaPagamento,
                                 Long encomendaId, Long vendaId, Long compraId, Long contaFixaId,
                                 boolean automatico) {
    }

    public record ValorPorNome(String nome, BigDecimal valor, int percentual) {
    }

    public record ResumoFinanceiro(int ano, int mes, BigDecimal entradas, BigDecimal saidas, BigDecimal saldo,
                                   BigDecimal aReceber, List<ValorPorNome> entradasPorCategoria,
                                   List<LancamentoResp> lancamentos, List<EncomendaResp> contasAReceber) {
    }

    // ---------- Materiais ----------

    public record MaterialReq(
            @NotBlank(message = "Informe o nome.") @Size(max = 140) String nome,
            @NotBlank(message = "Informe a unidade.") @Size(max = 20) String unidade,
            @NotNull(message = "Informe a quantidade em estoque.") BigDecimal quantidade,
            @NotNull @PositiveOrZero BigDecimal estoqueMinimo,
            @NotNull @PositiveOrZero BigDecimal custoUnitario) {
    }

    public record MaterialResp(Long id, String nome, String unidade, BigDecimal quantidade, BigDecimal estoqueMinimo,
                               BigDecimal custoUnitario, boolean abaixoDoMinimo) {
    }

    public record AjusteEstoqueReq(
            @NotNull(message = "Escolha entrada ou saída.") TipoLancamento tipo,
            @NotNull @Positive(message = "A quantidade precisa ser maior que zero.") BigDecimal quantidade,
            /** Na entrada (compra), registra a despesa no financeiro. */
            boolean registrarDespesa,
            @PositiveOrZero BigDecimal valorCompra,
            Long fornecedorId) {
    }

    // ---------- Precificação ----------

    public record PrecificacaoReq(
            @NotNull @PositiveOrZero(message = "Valor inválido.") BigDecimal salarioDesejado,
            @NotNull @Min(value = 1, message = "Informe as horas de produção por mês.") @Max(value = 400, message = "Horas por mês acima de 400.") Integer horasMes,
            @NotNull @PositiveOrZero(message = "Valor inválido.") BigDecimal custosFixosMensais,
            @NotNull @PositiveOrZero @Max(value = 500, message = "Margem acima de 500%.") BigDecimal margemPadrao,
            /** Usa a soma das contas fixas como custos fixos. */
            Boolean custosFixosAutomatico) {
    }

    public record PrecificacaoResp(BigDecimal salarioDesejado, int horasMes, BigDecimal custosFixosMensais,
                                   BigDecimal margemPadrao, BigDecimal valorHora, BigDecimal custoFixoHora,
                                   boolean configurada, boolean custosFixosAutomatico, BigDecimal totalContasFixas) {
    }

    // ---------- Produtos / fichas técnicas ----------

    public record ItemFichaReq(
            @NotNull(message = "Escolha o material.") Long materialId,
            @NotNull @Positive(message = "Informe a quantidade de cada material.") BigDecimal quantidade) {
    }

    public record ProdutoReq(
            @NotBlank(message = "Informe o nome da peça.") @Size(max = 140) String nome,
            @Size(max = 4000) String descricao,
            @NotNull(message = "Escolha a frente.") Frente frente,
            @NotNull @PositiveOrZero(message = "Horas inválidas.") BigDecimal horasMaoObra,
            @PositiveOrZero(message = "Valor inválido.") BigDecimal outrosCustos,
            @PositiveOrZero @Max(value = 500, message = "Margem acima de 500%.") BigDecimal margem,
            @PositiveOrZero(message = "Preço inválido.") BigDecimal precoVenda,
            Boolean prontaEntrega,
            @PositiveOrZero(message = "Estoque inválido.") Integer estoque,
            Boolean ativo,
            List<@jakarta.validation.Valid ItemFichaReq> materiais) {
    }

    public record ItemFichaResp(Long materialId, String materialNome, String unidade, BigDecimal quantidade,
                                BigDecimal custoUnitario, BigDecimal custoLinha) {
    }

    /** Composição do preço de UMA peça. */
    public record CustoFicha(BigDecimal custoMateriais, BigDecimal custoMaoObra, BigDecimal custoFixo,
                             BigDecimal outrosCustos, BigDecimal custoTotal, BigDecimal margem,
                             BigDecimal precoSugerido, BigDecimal precoAtual, BigDecimal lucroUnitario,
                             BigDecimal ganhoPorHora) {
    }

    public record ProdutoResp(Long id, String nome, String descricao, Frente frente, BigDecimal horasMaoObra,
                              BigDecimal outrosCustos, BigDecimal margem, BigDecimal precoVenda, boolean prontaEntrega,
                              int estoque, boolean ativo, List<ItemFichaResp> materiais, CustoFicha custo,
                              CatalogoInfo catalogo) {
    }

    public record ProducaoReq(@NotNull @Min(value = 1, message = "Informe quantas peças ficaram prontas.") Integer quantidade) {
    }

    // ---------- Vendas (pronta-entrega) ----------

    public record VendaItemReq(
            Long produtoId,
            @Size(max = 160) String descricao,
            @NotNull @Min(value = 1, message = "A quantidade mínima é 1.") Integer quantidade,
            @PositiveOrZero(message = "Preço inválido.") BigDecimal precoUnitario) {
    }

    public record VendaReq(
            LocalDate data,
            Long clienteId,
            CanalVenda canal,
            FormaPagamento formaPagamento,
            @PositiveOrZero(message = "Desconto inválido.") BigDecimal desconto,
            @Size(max = 255) String observacao,
            @NotEmpty(message = "Adicione pelo menos uma peça.") List<@jakarta.validation.Valid VendaItemReq> itens) {
    }

    public record VendaItemResp(Long produtoId, String descricao, int quantidade, BigDecimal precoUnitario,
                                BigDecimal custoUnitario) {
    }

    public record VendaResp(Long id, LocalDate data, Long clienteId, String clienteNome, CanalVenda canal,
                            FormaPagamento formaPagamento, BigDecimal desconto, BigDecimal total, BigDecimal custo,
                            String observacao, StatusVenda status, List<VendaItemResp> itens) {
    }

    public record ResumoVendas(int ano, int mes, BigDecimal totalVendido, BigDecimal custoDireto, BigDecimal sobra,
                               int pecas, int quantidadeVendas, List<ValorPorNome> porCanal, List<VendaResp> vendas) {
    }

    // ---------- Proposta e contrato ----------

    public record DocumentosConfig(
            @Size(max = 120) String responsavel,
            @Size(max = 30) String documento,
            @NotNull @PositiveOrZero @Max(value = 100, message = "O sinal vai de 0 a 100%.") BigDecimal sinalPercentual,
            @NotNull @Min(value = 1, message = "Validade mínima de 1 dia.") @Max(value = 365) Integer validadePropostaDias,
            @Size(max = 4000) String condicoesProposta,
            @Size(max = 4000) String politicaCancelamento) {
    }

    // ---------- Agenda ----------

    public record CompromissoReq(
            @NotBlank(message = "Informe o título.") @Size(max = 160) String titulo,
            TipoCompromisso tipo,
            @NotNull(message = "Informe data e hora.") LocalDateTime inicio,
            LocalDateTime fim,
            @Size(max = 160) String local,
            Long clienteId,
            Long encomendaId,
            @Size(max = 4000) String observacoes,
            Boolean concluido) {
    }

    public record CompromissoResp(Long id, String titulo, TipoCompromisso tipo, LocalDateTime inicio,
                                  LocalDateTime fim, String local, Long clienteId, String clienteNome,
                                  Long encomendaId, String encomendaDescricao, String observacoes, boolean concluido) {
    }

    /**
     * Item da agenda. tipo: ENTREGA, COMPROMISSO, CONTA, ANIVERSARIO.
     * hora vazia = o dia todo.
     */
    public record EventoAgenda(String tipo, Long id, LocalDate data, java.time.LocalTime hora, String titulo,
                               String detalhe, String status, boolean concluido) {
    }

    // ---------- Lembretes ----------

    /** Lembrete pronto para enviar pelo WhatsApp (a mensagem já vem escrita). */
    public record LembreteResp(String chave, String tipo, String titulo, String detalhe, String nome,
                               String whatsapp, String mensagem, LocalDate data) {
    }

    public record MarcarLembreteReq(@NotBlank @Size(max = 120) String chave) {
    }

    // ---------- Fotos da encomenda ----------

    public record FotoResp(Long id, Long encomendaId, TipoFoto tipo, String legenda, String url,
                           Long portfolioItemId, LocalDateTime criadoEm) {
    }

    public record PublicarFotoReq(@Size(max = 140) String titulo, CategoriaPortfolio categoria) {
    }

    // ---------- Relatórios ----------

    public record Comparativo(BigDecimal atual, BigDecimal anterior, Integer variacaoPercentual) {
    }

    public record MesValores(String mes, BigDecimal entradas, BigDecimal saidas) {
    }

    /** Resultado das encomendas entregues, agrupadas pela ficha técnica. */
    public record LinhaPeca(String nome, int pecas, BigDecimal faturamento, BigDecimal materiais,
                            BigDecimal ganho, BigDecimal horas, BigDecimal ganhoPorHora) {
    }

    public record LinhaVenda(String nome, int pecas, BigDecimal faturamento, BigDecimal custo, BigDecimal sobra) {
    }

    public record LinhaRanking(String nome, int pecas, int emEncomendas, int emVendas, BigDecimal faturamento) {
    }

    public record LinhaMaterial(String nome, String unidade, BigDecimal quantidade, BigDecimal custo) {
    }

    public record Relatorio(LocalDate de, LocalDate ate, LocalDate deAnterior, LocalDate ateAnterior,
                            Comparativo entradas, Comparativo saidas, Comparativo resultado,
                            int encomendasEntregues, BigDecimal ticketMedio,
                            List<MesValores> evolucao, List<ValorPorNome> porFrente, List<LinhaPeca> porPeca,
                            List<LinhaVenda> prontaEntrega, BigDecimal descontosVendas,
                            List<LinhaRanking> maisVendidos, List<LinhaMaterial> materiais,
                            List<LancamentoResp> lancamentos) {
    }

    // ---------- Portfólio ----------

    public record PortfolioResp(Long id, String titulo, CategoriaPortfolio categoria, String url, int ordem,
                                boolean publicado) {
    }

    public record PortfolioReq(
            @NotBlank(message = "Informe o título.") @Size(max = 140) String titulo,
            @NotNull(message = "Escolha a categoria.") CategoriaPortfolio categoria,
            Integer ordem,
            Boolean publicado) {
    }

    // ---------- Site e painel ----------

    /** maisPedidos = ids das peças do catálogo mais pedidas nos últimos meses (só a ordem, sem números). */
    public record SitePublico(ConfiguracaoDto atelie, List<PortfolioResp> portfolio,
                              List<DepoimentoResp> depoimentos, List<ItemCatalogo> catalogo, List<Long> maisPedidos) {
    }

    public record Barra(String rotulo, BigDecimal valor) {
    }

    public record Painel(
            String saudacao,
            long orcamentosAbertos,
            long orcamentosNovos,
            long emProducao,
            long entregasSemana,
            BigDecimal aReceber,
            long contasAReceber,
            long artesAFazer,
            long ajustesPedidos,
            long artesComCliente,
            List<Barra> recebidoPorSemana,
            List<ValorPorNome> receitaPorFrente,
            List<EncomendaResp> proximasEntregas,
            List<MaterialResp> materiaisBaixos,
            String mesReferencia,
            int lembretes,
            List<ContaAVencer> contasAPagar) {
    }

    // ---------- Fornecedores e compras ----------

    public record FornecedorReq(
            @NotBlank(message = "Informe o nome do fornecedor.") @Size(max = 140) String nome,
            @Size(max = 20) String whatsapp,
            @Size(max = 160) String contato,
            @Size(max = 120) String cidade,
            @Size(max = 4000) String observacoes,
            Boolean ativo) {
    }

    public record FornecedorResp(Long id, String nome, String whatsapp, String contato, String cidade,
                                 String observacoes, boolean ativo, int compras, BigDecimal totalComprado,
                                 LocalDate ultimaCompra) {
    }

    public record CompraItemReq(
            @NotNull(message = "Escolha o material.") Long materialId,
            @NotNull @Positive(message = "Informe a quantidade comprada.") BigDecimal quantidade,
            @NotNull @Positive(message = "Informe quanto pagou em cada material.") BigDecimal valor) {
    }

    public record CompraReq(
            Long fornecedorId,
            @NotNull(message = "Informe a data da compra.") LocalDate data,
            @PositiveOrZero(message = "Frete inválido.") BigDecimal frete,
            FormaPagamento formaPagamento,
            @Size(max = 255) String observacoes,
            /** Vazio ou true: lança a despesa no financeiro. */
            Boolean registrarDespesa,
            @NotEmpty(message = "Adicione pelo menos um material.") List<@jakarta.validation.Valid CompraItemReq> itens) {
    }

    public record CompraItemResp(Long materialId, String materialNome, String unidade, BigDecimal quantidade,
                                 BigDecimal valor, BigDecimal custoUnitario) {
    }

    public record CompraResp(Long id, Long fornecedorId, String fornecedorNome, LocalDate data, BigDecimal frete,
                             BigDecimal total, FormaPagamento formaPagamento, String observacoes, StatusCompra status,
                             boolean despesaLancada, List<CompraItemResp> itens) {
    }

    public record ResumoCompras(int ano, int mes, BigDecimal total, int quantidade, List<ValorPorNome> porFornecedor,
                                List<CompraResp> compras) {
    }

    public record PrecoPago(Long compraId, LocalDate data, String fornecedorNome, BigDecimal quantidade,
                            BigDecimal custoUnitario) {
    }

    /** Último preço pago em cada fornecedor; o mais barato vem marcado. */
    public record PrecoPorFornecedor(Long fornecedorId, String fornecedorNome, BigDecimal ultimoCusto, LocalDate data,
                                     boolean maisBarato) {
    }

    public record HistoricoPrecos(Long materialId, String nome, String unidade, BigDecimal custoAtual,
                                  List<PrecoPago> compras, List<PrecoPorFornecedor> porFornecedor) {
    }

    // ---------- Contas fixas ----------

    public record ContaFixaReq(
            @NotBlank(message = "Informe o nome da conta.") @Size(max = 120) String nome,
            @NotNull(message = "Informe o valor.") @Positive(message = "O valor precisa ser maior que zero.") BigDecimal valor,
            @NotNull(message = "Informe o dia do vencimento.") @Min(value = 1, message = "Dia de 1 a 31.") @Max(value = 31, message = "Dia de 1 a 31.") Integer diaVencimento,
            Boolean entraNaPrecificacao,
            Boolean ativa,
            @Size(max = 255) String observacoes) {
    }

    public record ContaFixaResp(Long id, String nome, BigDecimal valor, int diaVencimento,
                                boolean entraNaPrecificacao, boolean ativa, String observacoes) {
    }

    /** situacao: PAGA, VENCIDA, HOJE, PROXIMA (até 3 dias), A_VENCER. */
    public record ContaDoMes(ContaFixaResp conta, LocalDate vencimento, String situacao, Long lancamentoId,
                             BigDecimal valorPago, LocalDate pagoEm, FormaPagamento formaPagamento) {
    }

    public record ContasDoMes(int ano, int mes, BigDecimal previsto, BigDecimal pago, BigDecimal falta,
                              int vencidas, BigDecimal totalPrecificacao, boolean custosFixosAutomatico,
                              List<ContaDoMes> contas, List<ContaFixaResp> inativas) {
    }

    public record PagarContaReq(
            @NotNull Integer ano,
            @NotNull @Min(1) @Max(12) Integer mes,
            @NotNull(message = "Informe o valor pago.") @Positive(message = "O valor precisa ser maior que zero.") BigDecimal valor,
            LocalDate data,
            FormaPagamento formaPagamento) {
    }

    public record ContaAVencer(Long id, String nome, BigDecimal valor, LocalDate vencimento, boolean vencida) {
    }

    // ---------- Equipe ----------

    public record UsuarioReq(
            @NotBlank(message = "Informe o nome.") @Size(max = 120) String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 160) String email,
            Perfil perfil,
            /** Obrigatória ao criar; ao editar, em branco mantém a atual. */
            @Size(min = 8, max = 100, message = "A senha precisa ter pelo menos 8 caracteres.") String senha,
            Boolean ativo) {
    }

    public record UsuarioResp(Long id, String nome, String email, Perfil perfil, boolean ativo,
                              LocalDateTime criadoEm, boolean euMesma) {
    }

    // ---------- Depoimentos ----------

    public record DepoimentoReq(
            @NotBlank(message = "Informe o nome de quem escreveu.") @Size(max = 120) String nome,
            @Size(max = 120) String contexto,
            @NotBlank(message = "Escreva o depoimento.") @Size(max = 600, message = "Use até 600 caracteres.") String texto,
            @Min(value = 1, message = "De 1 a 5 estrelas.") @Max(value = 5, message = "De 1 a 5 estrelas.") Integer estrelas,
            Boolean publicado,
            Integer ordem,
            /** true = tira a foto atual. */
            Boolean removerFoto) {
    }

    public record DepoimentoResp(Long id, String nome, String contexto, String texto, int estrelas, String foto,
                                 boolean publicado, int ordem) {
    }

    // ---------- Catálogo ----------

    public record CatalogoReq(
            Boolean noCatalogo,
            Boolean mostrarPreco,
            @Size(max = 2000, message = "Texto de até 2000 caracteres.") String textoCatalogo,
            @Size(max = 80) String prazoProducao,
            Integer ordemCatalogo) {
    }

    public record FotoProdutoResp(Long id, String url, int ordem) {
    }

    public record CatalogoInfo(boolean noCatalogo, boolean mostrarPreco, String textoCatalogo, String prazoProducao,
                               int ordemCatalogo, List<FotoProdutoResp> fotos) {
    }

    /** Peça como aparece no site. preco vazio = "sob consulta". Nunca leva custos. */
    public record ItemCatalogo(Long id, String nome, String texto, Frente frente, BigDecimal preco,
                               boolean prontaEntrega, int disponiveis, String prazoProducao, List<String> fotos) {
    }
}
