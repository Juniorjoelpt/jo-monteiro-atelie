package br.com.mssolucoes.atelie;

import br.com.mssolucoes.atelie.repositorio.EncomendaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Etapa 4: fornecedores e compras, contas fixas, ajudante e depoimentos. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ComprasContasEquipeTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    EncomendaRepository encomendas;

    String token;
    String tokenAdmin;
    final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void preparar() throws Exception {
        tokenAdmin = entrar("teste@atelie.local", "senha-teste-123");
        token = tokenAdmin;
        enviar(put("/api/configuracoes"), """
                {"nomeAtelie":"Ateliê Jô Monteiro","whatsapp":"98988793891","chavePix":"jo@pix.com","cidade":"Governador Nunes Freire — MA"}""",
                status().isOk());
    }

    @Test
    void compraComFreteRateadoAtualizaEstoqueCustoDespesaEHistoricoECancelamentoDesfazTudo() throws Exception {
        long cordao = id(enviar(post("/api/materiais"),
                "{\"nome\":\"Cordão 5 mm\",\"unidade\":\"metro\",\"quantidade\":10,\"estoqueMinimo\":0,\"custoUnitario\":0.40}", status().isCreated()));
        long argola = id(enviar(post("/api/materiais"),
                "{\"nome\":\"Argola\",\"unidade\":\"unidade\",\"quantidade\":0,\"estoqueMinimo\":0,\"custoUnitario\":1}", status().isCreated()));
        long fioArte = id(enviar(post("/api/fornecedores"), "{\"nome\":\"Fio Arte\",\"whatsapp\":\"98981234567\"}", status().isCreated()));
        long barbante = id(enviar(post("/api/fornecedores"), "{\"nome\":\"Barbantes SP\"}", status().isCreated()));

        // 1ª compra (mais barata, mais antiga) no Barbantes SP: 100 m por R$ 45 = 0,45/m
        enviar(post("/api/compras"), """
                {"fornecedorId":%d,"data":"%s","itens":[{"materialId":%d,"quantidade":100,"valor":45}]}
                """.formatted(barbante, hoje.minusDays(20), cordao), status().isCreated());

        // 2ª compra na Fio Arte: 200 m por R$ 100 + 20 argolas por R$ 20, frete R$ 12 (rateio 10 + 2)
        JsonNode compra = enviar(post("/api/compras"), """
                {"fornecedorId":%d,"data":"%s","frete":12,"formaPagamento":"CARTAO",
                 "itens":[{"materialId":%d,"quantidade":200,"valor":100},{"materialId":%d,"quantidade":20,"valor":20}]}
                """.formatted(fioArte, hoje, cordao, argola), status().isCreated());
        assertThat(compra.get("total").decimalValue()).isEqualByComparingTo("132");
        assertThat(compra.get("itens").get(0).get("custoUnitario").decimalValue()).isEqualByComparingTo("0.55");
        assertThat(compra.get("itens").get(1).get("custoUnitario").decimalValue()).isEqualByComparingTo("1.10");
        assertThat(compra.get("despesaLancada").asBoolean()).isTrue();

        JsonNode mat = material(cordao);
        assertThat(mat.get("quantidade").decimalValue()).isEqualByComparingTo("310");
        assertThat(mat.get("custoUnitario").decimalValue()).isEqualByComparingTo("0.55");

        // Despesa automática no financeiro, que não pode ser editada nem apagada por lá
        JsonNode fin = get("/api/financeiro");
        JsonNode despesa = null;
        for (JsonNode l : fin.get("lancamentos")) {
            if (l.get("compraId").asLong() == id(compra)) {
                despesa = l;
            }
        }
        assertThat(despesa).isNotNull();
        assertThat(despesa.get("valor").decimalValue()).isEqualByComparingTo("132");
        assertThat(despesa.get("categoria").asText()).isEqualTo("MATERIAL");
        assertThat(despesa.get("automatico").asBoolean()).isTrue();
        enviar(delete("/api/lancamentos/" + id(despesa)), null, status().isBadRequest());

        // Histórico: 2 preços, o Barbantes SP é o mais barato
        JsonNode hist = get("/api/materiais/" + cordao + "/precos");
        assertThat(hist.get("compras")).hasSize(2);
        assertThat(hist.get("porFornecedor").get(0).get("fornecedorNome").asText()).isEqualTo("Barbantes SP");
        assertThat(hist.get("porFornecedor").get(0).get("maisBarato").asBoolean()).isTrue();
        assertThat(hist.get("porFornecedor").get(1).get("maisBarato").asBoolean()).isFalse();

        JsonNode forn = get("/api/fornecedores");
        JsonNode fa = forn.get(0).get("nome").asText().equals("Fio Arte") ? forn.get(0) : forn.get(1);
        assertThat(fa.get("compras").asInt()).isEqualTo(1);
        assertThat(fa.get("totalComprado").decimalValue()).isEqualByComparingTo("132");

        JsonNode mes = get("/api/compras?ano=" + hoje.getYear() + "&mes=" + hoje.getMonthValue());
        assertThat(mes.get("total").decimalValue()).isGreaterThanOrEqualTo(new java.math.BigDecimal("132"));

        // Fornecedor com compras não pode ser excluído; material comprado também não
        enviar(delete("/api/fornecedores/" + fioArte), null, status().isBadRequest());
        enviar(delete("/api/materiais/" + argola), null, status().isBadRequest());

        // Cancelar: estoque volta, custo volta ao da compra anterior, despesa some
        JsonNode cancelada = enviar(post("/api/compras/" + id(compra) + "/cancelar"), null, status().isOk());
        assertThat(cancelada.get("status").asText()).isEqualTo("CANCELADA");
        mat = material(cordao);
        assertThat(mat.get("quantidade").decimalValue()).isEqualByComparingTo("110");
        assertThat(mat.get("custoUnitario").decimalValue()).isEqualByComparingTo("0.45");
        assertThat(get("/api/financeiro").get("lancamentos").findValues("compraId").stream()
                .filter(n -> n.asLong() == id(compra)).count()).isZero();
        enviar(post("/api/compras/" + id(compra) + "/cancelar"), null, status().isBadRequest());

        // "Comprei" na tela de materiais com valor também entra no histórico
        enviar(post("/api/materiais/" + cordao + "/ajuste"),
                "{\"tipo\":\"ENTRADA\",\"quantidade\":50,\"valorCompra\":30,\"registrarDespesa\":false,\"fornecedorId\":" + fioArte + "}",
                status().isOk());
        assertThat(material(cordao).get("custoUnitario").decimalValue()).isEqualByComparingTo("0.60");
        assertThat(get("/api/materiais/" + cordao + "/precos").get("compras")).hasSize(2);

        // Material repetido na mesma compra é recusado
        enviar(post("/api/compras"), """
                {"data":"%s","itens":[{"materialId":%d,"quantidade":1,"valor":1},{"materialId":%d,"quantidade":2,"valor":2}]}
                """.formatted(hoje, cordao, cordao), status().isBadRequest());
    }

    @Test
    void contasFixasComPagamentoNoFinanceiroPrecificacaoAutomaticaEAvisoNoPainel() throws Exception {
        long aluguel = id(enviar(post("/api/contas-fixas"), """
                {"nome":"Aluguel do ateliê","valor":600,"diaVencimento":31}""", status().isCreated()));
        long internet = id(enviar(post("/api/contas-fixas"), """
                {"nome":"Internet","valor":100,"diaVencimento":%d,"entraNaPrecificacao":false}""".formatted(hoje.getDayOfMonth()),
                status().isCreated()));

        // Precificação automática: custos fixos = só as contas marcadas (R$ 600)
        JsonNode prec = enviar(put("/api/configuracoes/precificacao"),
                "{\"salarioDesejado\":3200,\"horasMes\":160,\"custosFixosMensais\":999,\"margemPadrao\":30,\"custosFixosAutomatico\":true}",
                status().isOk());
        assertThat(prec.get("custosFixosMensais").decimalValue()).isEqualByComparingTo("600");
        assertThat(prec.get("custoFixoHora").decimalValue()).isEqualByComparingTo("3.75");
        // Mudou o valor da conta: a precificação acompanha
        enviar(put("/api/contas-fixas/" + aluguel), """
                {"nome":"Aluguel do ateliê","valor":640,"diaVencimento":31}""", status().isOk());
        assertThat(get("/api/configuracoes/precificacao").get("custosFixosMensais").decimalValue()).isEqualByComparingTo("640");

        int ano = hoje.getYear(), mes = hoje.getMonthValue();
        JsonNode contas = get("/api/contas-fixas?ano=" + ano + "&mes=" + mes);
        assertThat(contas.get("previsto").decimalValue()).isEqualByComparingTo("740");
        assertThat(contas.get("totalPrecificacao").decimalValue()).isEqualByComparingTo("640");
        JsonNode linhaInternet = linha(contas, internet);
        assertThat(linhaInternet.get("situacao").asText()).isEqualTo("HOJE");
        // Dia 31 vence no último dia do mês
        assertThat(linha(contas, aluguel).get("vencimento").asText()).isEqualTo(YearMonth.from(hoje).atEndOfMonth().toString());
        assertThat(linha(get("/api/contas-fixas?ano=2027&mes=2"), aluguel).get("vencimento").asText()).isEqualTo("2027-02-28");

        // Vence hoje: aparece no painel
        JsonNode painel = get("/api/painel");
        assertThat(painel.get("contasAPagar").findValues("nome").stream().map(JsonNode::asText)).contains("Internet");

        // Pagar (valor do mês diferente do previsto) → despesa "Contas fixas"
        JsonNode paga = enviar(post("/api/contas-fixas/" + internet + "/pagar"),
                "{\"ano\":%d,\"mes\":%d,\"valor\":109.90}".formatted(ano, mes), status().isOk());
        assertThat(paga.get("situacao").asText()).isEqualTo("PAGA");
        enviar(post("/api/contas-fixas/" + internet + "/pagar"),
                "{\"ano\":%d,\"mes\":%d,\"valor\":109.90}".formatted(ano, mes), status().isBadRequest());

        contas = get("/api/contas-fixas?ano=" + ano + "&mes=" + mes);
        assertThat(contas.get("pago").decimalValue()).isEqualByComparingTo("109.90");
        assertThat(contas.get("falta").decimalValue()).isEqualByComparingTo("640");
        JsonNode lanc = get("/api/financeiro").get("lancamentos").get(0);
        assertThat(lanc.get("categoria").asText()).isEqualTo("CONTAS_FIXAS");
        assertThat(lanc.get("contaFixaId").asLong()).isEqualTo(internet);
        enviar(put("/api/lancamentos/" + id(lanc)), """
                {"tipo":"SAIDA","categoria":"OUTROS","descricao":"x","valor":1,"data":"%s"}""".formatted(hoje), status().isBadRequest());
        assertThat(get("/api/painel").get("contasAPagar").findValues("nome").stream().map(JsonNode::asText)).doesNotContain("Internet");

        // Agenda mostra os vencimentos
        JsonNode agenda = get("/api/agenda?de=" + hoje.withDayOfMonth(1) + "&ate=" + YearMonth.from(hoje).atEndOfMonth());
        assertThat(agenda.findValues("tipo").stream().map(JsonNode::asText)).contains("CONTA");

        // Conta com pagamento não é excluída; desfazer o pagamento libera
        enviar(delete("/api/contas-fixas/" + internet), null, status().isBadRequest());
        enviar(delete("/api/contas-fixas/" + internet + "/pagamento?ano=" + ano + "&mes=" + mes), null, status().isNoContent());
        enviar(delete("/api/contas-fixas/" + internet), null, status().isNoContent());

        // Conta desativada sai da precificação
        enviar(put("/api/contas-fixas/" + aluguel), """
                {"nome":"Aluguel do ateliê","valor":640,"diaVencimento":31,"ativa":false}""", status().isOk());
        assertThat(get("/api/configuracoes/precificacao").get("custosFixosMensais").decimalValue()).isEqualByComparingTo("0");
        assertThat(get("/api/contas-fixas").get("inativas")).hasSize(1);
    }

    @Test
    void ajudanteVeProducaoSemValoresENaoAcessaFinanceiroNemConfiguracoes() throws Exception {
        long cordao = id(enviar(post("/api/materiais"),
                "{\"nome\":\"Cordão\",\"unidade\":\"metro\",\"quantidade\":100,\"estoqueMinimo\":0,\"custoUnitario\":0.5}", status().isCreated()));
        long cliente = id(enviar(post("/api/clientes"), "{\"nome\":\"Joana\",\"whatsapp\":\"98981112233\"}", status().isCreated()));
        long enc = id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"Painel de parede","frente":"PAPELARIA","valorTotal":350,"valorSinal":100,"status":"EM_PRODUCAO"}
                """.formatted(cliente), status().isCreated()));
        enviar(post("/api/contas-fixas"), "{\"nome\":\"MEI\",\"valor\":76,\"diaVencimento\":20}", status().isCreated());

        JsonNode criada = enviar(post("/api/equipe"),
                "{\"nome\":\"Rita Ajudante\",\"email\":\"Rita@Atelie.local\",\"perfil\":\"AJUDANTE\",\"senha\":\"rita-senha-1\"}",
                status().isCreated());
        assertThat(criada.get("email").asText()).isEqualTo("rita@atelie.local");
        enviar(post("/api/equipe"), "{\"nome\":\"Outra\",\"email\":\"rita@atelie.local\",\"senha\":\"12345678\"}", status().isBadRequest());

        String tokenRita = entrar("rita@atelie.local", "rita-senha-1");
        JsonNode login = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rita@atelie.local\",\"senha\":\"rita-senha-1\"}")).andReturn().getResponse().getContentAsString());
        assertThat(login.get("perfil").asText()).isEqualTo("AJUDANTE");
        token = tokenRita;

        // Vê a encomenda, sem nenhum valor
        JsonNode lista = get("/api/encomendas");
        assertThat(lista.get(0).get("descricao").asText()).isEqualTo("Painel de parede");
        assertThat(lista.get(0).has("valorTotal")).isFalse();
        assertThat(lista.get(0).has("saldo")).isFalse();
        assertThat(lista.get(0).has("custos")).isFalse();
        assertThat(get("/api/encomendas/" + enc).has("valorPago")).isFalse();
        assertThat(get("/api/materiais").get(0).has("custoUnitario")).isFalse();
        assertThat(get("/api/materiais").get(0).get("quantidade").decimalValue()).isEqualByComparingTo("100");

        // Peça: vê o preço de venda (para o caixa), não a composição do custo
        token = tokenAdmin;
        enviar(post("/api/produtos"), """
                {"nome":"Chaveiro","frente":"PRESENTE","horasMaoObra":0.5,"precoVenda":25,"prontaEntrega":true,"estoque":3,
                 "materiais":[{"materialId":%d,"quantidade":2}]}""".formatted(cordao), status().isCreated());
        token = tokenRita;
        JsonNode peca = get("/api/produtos").get(0);
        assertThat(peca.get("custo").get("precoAtual").decimalValue()).isEqualByComparingTo("25");
        assertThat(peca.get("custo").has("custoTotal")).isFalse();
        assertThat(peca.get("custo").has("lucroUnitario")).isFalse();
        assertThat(peca.get("materiais").get(0).has("custoUnitario")).isFalse();
        JsonNode venda = enviar(post("/api/vendas"), "{\"canal\":\"FEIRA\",\"itens\":[{\"produtoId\":%d,\"quantidade\":1}]}".formatted(id(peca)),
                status().isCreated());
        assertThat(venda.get("total").decimalValue()).isEqualByComparingTo("25");
        assertThat(venda.has("custo")).isFalse();

        // Pode: usar material, marcar como pronta
        enviar(post("/api/materiais/" + cordao + "/ajuste"), "{\"tipo\":\"SAIDA\",\"quantidade\":5}", status().isOk());
        enviar(patch("/api/encomendas/" + enc + "/status"), "{\"status\":\"PRONTA\"}", status().isOk());

        // Não pode: compras, cancelar encomenda, financeiro, pagamentos, contrato, configurações, equipe
        enviar(post("/api/materiais/" + cordao + "/ajuste"), "{\"tipo\":\"ENTRADA\",\"quantidade\":5}", status().isForbidden());
        JsonNode negado = enviar(patch("/api/encomendas/" + enc + "/status"), "{\"status\":\"CANCELADA\"}", status().isForbidden());
        assertThat(negado.get("erro").asText()).isNotBlank();
        for (String url : List.of("/api/financeiro", "/api/painel", "/api/relatorios?de=2026-01-01&ate=2026-01-31",
                "/api/encomendas/" + enc + "/pagamentos", "/api/encomendas/" + enc + "/contrato", "/api/configuracoes",
                "/api/equipe", "/api/compras", "/api/contas-fixas", "/api/lembretes", "/api/clientes/" + cliente)) {
            JsonNode r = enviar(get2(url), null, status().isForbidden());
            assertThat(r.get("erro").asText()).contains("acesso");
        }
        enviar(post("/api/encomendas/" + enc + "/pagamentos"), "{\"valor\":10}", status().isForbidden());
        enviar(delete("/api/encomendas/" + enc), null, status().isForbidden());
        enviar(put("/api/equipe/" + id(criada)), "{\"nome\":\"Rita\",\"email\":\"rita@atelie.local\",\"perfil\":\"ADMIN\"}", status().isForbidden());

        // Agenda da ajudante não mostra vencimento de contas
        JsonNode agenda = get("/api/agenda?de=" + hoje.withDayOfMonth(1) + "&ate=" + YearMonth.from(hoje).atEndOfMonth());
        assertThat(agenda.findValues("tipo").stream().map(JsonNode::asText)).doesNotContain("CONTA");

        // Administradora: não pode se rebaixar nem ficar sem nenhuma admin
        token = tokenAdmin;
        long eu = 0;
        for (JsonNode u : get("/api/equipe")) {
            if (u.get("euMesma").asBoolean()) {
                eu = id(u);
            }
        }
        enviar(put("/api/equipe/" + eu), "{\"nome\":\"Teste\",\"email\":\"teste@atelie.local\",\"perfil\":\"AJUDANTE\"}", status().isBadRequest());
        enviar(delete("/api/equipe/" + eu), null, status().isBadRequest());

        // A admin vê os valores normalmente
        assertThat(get("/api/encomendas/" + enc).get("valorTotal").decimalValue()).isEqualByComparingTo("350");

        // Desativada: o token da ajudante deixa de valer na hora
        enviar(put("/api/equipe/" + id(criada)),
                "{\"nome\":\"Rita Ajudante\",\"email\":\"rita@atelie.local\",\"perfil\":\"AJUDANTE\",\"ativo\":false}", status().isOk());
        token = tokenRita;
        enviar(get2("/api/encomendas"), null, status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rita@atelie.local\",\"senha\":\"rita-senha-1\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void depoimentosComFotoAparecemNoSiteNaOrdemELembretePedeDepoimentoDepoisDaEntrega() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
        JsonNode d1 = depoimento("/api/depoimentos", """
                {"nome":"Camila","contexto":"Noiva · casamento na praia","texto":"“Ficou tudo lindo, superou o que eu imaginava!”","ordem":2}""", png);
        assertThat(d1.get("texto").asText()).isEqualTo("Ficou tudo lindo, superou o que eu imaginava!");
        assertThat(d1.get("foto").asText()).startsWith("/uploads/");
        JsonNode d2 = depoimento("/api/depoimentos", """
                {"nome":"Luana","contexto":"Professora","texto":"Kit lindo e caprichado.","estrelas":5,"ordem":1}""", null);
        depoimento("/api/depoimentos", """
                {"nome":"Rascunho","texto":"Ainda vou pedir autorização.","publicado":false}""", null);
        enviar(post("/api/depoimentos"), null, status().is4xxClientError());

        JsonNode site = json.readTree(mvc.perform(MockMvcRequestBuilders.get("/api/publico/site"))
                .andReturn().getResponse().getContentAsString());
        List<String> nomes = new ArrayList<>();
        site.get("depoimentos").forEach(d -> nomes.add(d.get("nome").asText()));
        assertThat(nomes).containsExactly("Luana", "Camila");

        // Tirar a foto
        JsonNode semFoto = depoimento("/api/depoimentos/" + id(d1), """
                {"nome":"Camila","contexto":"Noiva","texto":"Ficou tudo lindo!","ordem":2,"removerFoto":true}""", null);
        assertThat(semFoto.get("foto").isNull()).isTrue();
        enviar(delete("/api/depoimentos/" + id(d2)), null, status().isNoContent());
        assertThat(get("/api/depoimentos")).hasSize(2);

        // Encomenda quitada entregue há 4 dias → lembrete pedindo depoimento
        long cliente = id(enviar(post("/api/clientes"), "{\"nome\":\"Paula Souza\",\"whatsapp\":\"98981119999\"}", status().isCreated()));
        long enc = id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"Cortina boho","frente":"PAPELARIA","valorTotal":200,"valorSinal":0}
                """.formatted(cliente), status().isCreated()));
        enviar(post("/api/encomendas/" + enc + "/pagamentos"), "{\"valor\":200}", status().isOk());
        enviar(patch("/api/encomendas/" + enc + "/status"), "{\"status\":\"ENTREGUE\"}", status().isOk());
        assertThat(get("/api/lembretes").findValues("tipo").stream().map(JsonNode::asText)).doesNotContain("DEPOIMENTO");
        var e = encomendas.findById(enc).orElseThrow();
        e.setEntregueEm(hoje.minusDays(4));
        encomendas.save(e);
        JsonNode lembrete = null;
        for (JsonNode l : get("/api/lembretes")) {
            if (l.get("tipo").asText().equals("DEPOIMENTO")) {
                lembrete = l;
            }
        }
        assertThat(lembrete).isNotNull();
        assertThat(lembrete.get("mensagem").asText()).contains("Paula", "Cortina boho", "site");
    }

    // ---------- Apoio ----------

    private JsonNode depoimento(String url, String dados, byte[] foto) throws Exception {
        var req = multipart(url).file(new MockMultipartFile("dados", "", "application/json", dados.getBytes()));
        if (foto != null) {
            req.file(new MockMultipartFile("foto", "foto.png", "image/png", foto));
        }
        String r = mvc.perform(req.header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return json.readTree(r);
    }

    private JsonNode linha(JsonNode contas, long id) {
        for (JsonNode l : contas.get("contas")) {
            if (l.get("conta").get("id").asLong() == id) {
                return l;
            }
        }
        throw new AssertionError("conta " + id + " não está no mês");
    }

    private JsonNode material(long id) throws Exception {
        for (JsonNode m : get("/api/materiais")) {
            if (m.get("id").asLong() == id) {
                return m;
            }
        }
        throw new AssertionError("material " + id);
    }

    private String entrar(String email, String senha) throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("token").asText();
    }

    private static String texto(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        StringBuilder sb = new StringBuilder();
        PdfTextExtractor ex = new PdfTextExtractor(reader);
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            sb.append(ex.getTextFromPage(i)).append('\n');
        }
        reader.close();
        return sb.toString().replace(' ', ' ').replaceAll("\\s+", " ");
    }

    private byte[] baixar(String url) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.get(url))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private long id(JsonNode n) {
        return n.get("id").asLong();
    }

    private JsonNode get(String url) throws Exception {
        return json.readTree(mvc.perform(auth(MockMvcRequestBuilders.get(url))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private static MockHttpServletRequestBuilder get2(String url) {
        return MockMvcRequestBuilders.get(url);
    }

    private JsonNode enviar(MockHttpServletRequestBuilder b, String corpo, ResultMatcher esperado) throws Exception {
        MockHttpServletRequestBuilder req = auth(b);
        if (corpo != null) {
            req = req.content(corpo);
        }
        String r = mvc.perform(req).andExpect(esperado).andReturn().getResponse().getContentAsString();
        return r.isBlank() ? null : json.readTree(r);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }
}
