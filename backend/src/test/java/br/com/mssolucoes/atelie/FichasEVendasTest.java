package br.com.mssolucoes.atelie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cenário de referência:
 * hora = R$ 3.200 ÷ 160 h = R$ 20/h; custo fixo = R$ 480 ÷ 160 h = R$ 3/h; margem 30%.
 * Ficha "Mandala 40 cm": 40 m de cordão a R$ 0,50 + 1 argola a R$ 9,50 = R$ 29,50; 3 h; R$ 2 de embalagem.
 * Custo total = 29,50 + 60 + 9 + 2 = 100,50 → × 1,30 = 130,65 → preço sugerido R$ 131,00.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FichasEVendasTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String token;
    long cordao;
    long argola;
    long mandala;

    @BeforeEach
    void preparar() throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"teste@atelie.local\",\"senha\":\"senha-teste-123\"}")).andReturn().getResponse().getContentAsString();
        token = json.readTree(r).get("token").asText();

        enviar(put("/api/configuracoes/precificacao"), """
                {"salarioDesejado":3200,"horasMes":160,"custosFixosMensais":480,"margemPadrao":30}""", status().isOk());
        cordao = enviar(post("/api/materiais"), """
                {"nome":"Cordão cru 5 mm","unidade":"metro","quantidade":100,"estoqueMinimo":20,"custoUnitario":0.5}""",
                status().isCreated()).get("id").asLong();
        argola = enviar(post("/api/materiais"), """
                {"nome":"Argola 15 cm","unidade":"unidade","quantidade":5,"estoqueMinimo":1,"custoUnitario":9.5}""",
                status().isCreated()).get("id").asLong();
        mandala = enviar(post("/api/produtos"), """
                {"nome":"Mandala 40 cm","frente":"PAPELARIA","horasMaoObra":3,"outrosCustos":2,
                 "materiais":[{"materialId":%d,"quantidade":40},{"materialId":%d,"quantidade":1}]}
                """.formatted(cordao, argola), status().isCreated()).get("id").asLong();
    }

    @Test
    void fichaCalculaPrecoSugeridoEPrecificacaoNaoVaiParaOSite() throws Exception {
        JsonNode p = get("/api/produtos/" + mandala).get("custo");
        assertThat(p.get("custoMateriais").decimalValue()).isEqualByComparingTo("29.50");
        assertThat(p.get("custoMaoObra").decimalValue()).isEqualByComparingTo("60");
        assertThat(p.get("custoFixo").decimalValue()).isEqualByComparingTo("9");
        assertThat(p.get("custoTotal").decimalValue()).isEqualByComparingTo("100.50");
        assertThat(p.get("precoSugerido").decimalValue()).isEqualByComparingTo("131");
        assertThat(p.get("lucroUnitario").decimalValue()).isEqualByComparingTo("30.50");
        assertThat(p.get("ganhoPorHora").decimalValue()).isEqualByComparingTo("30.17");

        JsonNode cfg = get("/api/configuracoes/precificacao");
        assertThat(cfg.get("valorHora").decimalValue()).isEqualByComparingTo("20");

        String site = mvc.perform(MockMvcRequestBuilders_get("/api/publico/site")).andReturn().getResponse().getContentAsString();
        assertThat(site).doesNotContain("salarioDesejado").doesNotContain("3200");

        // Compra com valor atualiza o custo do material e a ficha acompanha: 100 m por R$ 60 = R$ 0,60/m
        enviar(post("/api/materiais/" + cordao + "/ajuste"),
                "{\"tipo\":\"ENTRADA\",\"quantidade\":100,\"registrarDespesa\":true,\"valorCompra\":60}", status().isOk());
        assertThat(get("/api/produtos/" + mandala).get("custo").get("custoMateriais").decimalValue()).isEqualByComparingTo("33.50");

        // Material em ficha não pode ser excluído
        mvc.perform(auth(delete("/api/materiais/" + argola))).andExpect(status().isBadRequest());
    }

    @Test
    void encomendaComFichaBaixaMateriaisQuandoEntraEmProducaoEMostraOLucro() throws Exception {
        long cliente = enviar(post("/api/clientes"), "{\"nome\":\"Carla\",\"whatsapp\":\"98982223344\"}",
                status().isCreated()).get("id").asLong();
        JsonNode enc = enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"2 mandalas","frente":"PAPELARIA","produtoId":%d,"quantidade":2,
                 "valorTotal":300,"valorSinal":100}""".formatted(cliente, mandala), status().isCreated());
        long id = enc.get("id").asLong();
        assertThat(enc.get("status").asText()).isEqualTo("AGUARDANDO_SINAL");
        assertThat(enc.get("custos").get("estimado").asBoolean()).isTrue();
        assertThat(enc.get("custos").get("custoMateriais").decimalValue()).isEqualByComparingTo("59");
        assertThat(estoque(cordao)).isEqualByComparingTo("100");

        // Pagou o sinal: vai para produção e baixa 80 m de cordão e 2 argolas
        enc = enviar(post("/api/encomendas/" + id + "/pagamentos"), "{\"valor\":100}", status().isOk());
        assertThat(enc.get("status").asText()).isEqualTo("EM_PRODUCAO");
        assertThat(enc.get("materiaisBaixados").asBoolean()).isTrue();
        assertThat(estoque(cordao)).isEqualByComparingTo("20");
        assertThat(estoque(argola)).isEqualByComparingTo("3");

        // Ganho = 300 − 59 materiais − 18 fixos (6 h × 3) − 4 embalagem = 219; por hora 36,50; lucro 219 − 120 = 99
        JsonNode c = enc.get("custos");
        assertThat(c.get("estimado").asBoolean()).isFalse();
        assertThat(c.get("ganho").decimalValue()).isEqualByComparingTo("219");
        assertThat(c.get("ganhoPorHora").decimalValue()).isEqualByComparingTo("36.50");
        assertThat(c.get("lucro").decimalValue()).isEqualByComparingTo("99");

        // Material extra usado na peça
        long extra = enviar(post("/api/encomendas/" + id + "/consumos"),
                "{\"materialId\":%d,\"quantidade\":5}".formatted(cordao), status().isCreated()).get("id").asLong();
        assertThat(estoque(cordao)).isEqualByComparingTo("15");
        assertThat(get("/api/encomendas/" + id).get("custos").get("custoMateriais").decimalValue()).isEqualByComparingTo("61.50");
        mvc.perform(auth(delete("/api/consumos/" + extra))).andExpect(status().isNoContent());
        assertThat(estoque(cordao)).isEqualByComparingTo("20");

        // Avançar o status não baixa de novo
        enviar(patch("/api/encomendas/" + id + "/status"), "{\"status\":\"PRONTA\"}", status().isOk());
        assertThat(estoque(cordao)).isEqualByComparingTo("20");
        assertThat(get("/api/encomendas/" + id + "/consumos")).hasSize(2);

        // Não deixa trocar a quantidade depois da baixa
        enviar(put("/api/encomendas/" + id), """
                {"clienteId":%d,"descricao":"3 mandalas","frente":"PAPELARIA","produtoId":%d,"quantidade":3,
                 "valorTotal":450,"valorSinal":100}""".formatted(cliente, mandala), status().isBadRequest());

        // Ficha com histórico não pode ser excluída
        mvc.perform(auth(delete("/api/produtos/" + mandala))).andExpect(status().isBadRequest());
    }

    @Test
    void vendaDeProntaEntregaTiraDoEstoqueLancaNoFinanceiroECancelaDesfazendoTudo() throws Exception {
        long chaveiro = enviar(post("/api/produtos"), """
                {"nome":"Chaveiro mini","frente":"PRESENTE","horasMaoObra":0.5,"precoVenda":25,"prontaEntrega":true,
                 "materiais":[{"materialId":%d,"quantidade":2}]}""".formatted(cordao), status().isCreated()).get("id").asLong();

        // Produziu 3: estoque 3 e 6 m de cordão a menos
        JsonNode p = enviar(post("/api/produtos/" + chaveiro + "/producao"), "{\"quantidade\":3}", status().isOk());
        assertThat(p.get("estoque").asInt()).isEqualTo(3);
        assertThat(estoque(cordao)).isEqualByComparingTo("94");

        // 2 chaveiros (preço da ficha, R$ 25) + 1 item avulso de R$ 15, desconto de R$ 5 = R$ 60
        JsonNode v = enviar(post("/api/vendas"), """
                {"canal":"FEIRA","formaPagamento":"DINHEIRO","desconto":5,
                 "itens":[{"produtoId":%d,"quantidade":2},{"descricao":"Marcador de página","quantidade":1,"precoUnitario":15}]}
                """.formatted(chaveiro), status().isCreated());
        assertThat(v.get("total").decimalValue()).isEqualByComparingTo("60");
        assertThat(v.get("custo").decimalValue()).isEqualByComparingTo("2"); // 2 × (2 m × R$ 0,50)
        assertThat(get("/api/produtos/" + chaveiro).get("estoque").asInt()).isEqualTo(1);

        JsonNode fin = get("/api/financeiro");
        assertThat(fin.get("entradas").decimalValue()).isEqualByComparingTo("60");
        JsonNode lanc = fin.get("lancamentos").get(0);
        assertThat(lanc.get("categoria").asText()).isEqualTo("VENDA");
        assertThat(lanc.get("automatico").asBoolean()).isTrue();
        mvc.perform(auth(delete("/api/lancamentos/" + lanc.get("id").asLong()))).andExpect(status().isBadRequest());

        // Sem estoque suficiente
        enviar(post("/api/vendas"), "{\"itens\":[{\"produtoId\":%d,\"quantidade\":2}]}".formatted(chaveiro), status().isBadRequest());
        // Peça que não é pronta-entrega
        enviar(post("/api/vendas"), "{\"itens\":[{\"produtoId\":%d,\"quantidade\":1}]}".formatted(mandala), status().isBadRequest());

        JsonNode resumo = get("/api/vendas");
        assertThat(resumo.get("totalVendido").decimalValue()).isEqualByComparingTo("60");
        assertThat(resumo.get("pecas").asInt()).isEqualTo(3);
        assertThat(resumo.get("porCanal").get(0).get("nome").asText()).isEqualTo("Feira");

        enviar(post("/api/vendas/" + v.get("id").asLong() + "/cancelar"), "{}", status().isOk());
        assertThat(get("/api/produtos/" + chaveiro).get("estoque").asInt()).isEqualTo(3);
        assertThat(get("/api/financeiro").get("entradas").decimalValue()).isEqualByComparingTo("0");
        assertThat(get("/api/vendas").get("totalVendido").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void aprovarOrcamentoComFichaSemSinalJaBaixaOsMateriais() throws Exception {
        mvc.perform(multipart("/api/publico/orcamentos").file(new org.springframework.mock.web.MockMultipartFile("dados", "",
                        "application/json", "{\"nome\":\"Bia\",\"whatsapp\":\"98990001111\",\"tipoPeca\":\"Caderneta\"}".getBytes())))
                .andExpect(status().isCreated());
        long orc = get("/api/orcamentos").get(0).get("id").asLong();
        JsonNode enc = enviar(post("/api/orcamentos/" + orc + "/aprovar"), """
                {"descricao":"Mandala","frente":"PAPELARIA","produtoId":%d,"quantidade":1,"valorTotal":131,"valorSinal":0}
                """.formatted(mandala), status().isOk());
        assertThat(enc.get("status").asText()).isEqualTo("EM_PRODUCAO");
        assertThat(enc.get("produtoNome").asText()).isEqualTo("Mandala 40 cm");
        assertThat(estoque(cordao)).isEqualByComparingTo("60");
    }

    // ---------- apoio ----------

    private java.math.BigDecimal estoque(long materialId) throws Exception {
        for (JsonNode m : get("/api/materiais")) {
            if (m.get("id").asLong() == materialId) {
                return m.get("quantidade").decimalValue();
            }
        }
        throw new AssertionError("material não encontrado");
    }

    private JsonNode get(String url) throws Exception {
        return json.readTree(mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static MockHttpServletRequestBuilder MockMvcRequestBuilders_get(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url);
    }

    private JsonNode enviar(MockHttpServletRequestBuilder b, String corpo, ResultMatcher esperado) throws Exception {
        String r = mvc.perform(auth(b).content(corpo)).andExpect(esperado).andReturn().getResponse().getContentAsString();
        return r.isBlank() ? null : json.readTree(r);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }
}
