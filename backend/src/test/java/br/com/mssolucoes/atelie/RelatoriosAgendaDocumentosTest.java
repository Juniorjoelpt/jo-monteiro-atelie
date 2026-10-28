package br.com.mssolucoes.atelie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cenário: hora R$ 20, custo fixo R$ 3/h. Cordão a R$ 0,50/m. Ficha "Mandala" = 40 m (R$ 20) e 3 h.
 * Hoje: mandala entregue por R$ 150 (sinal 50 + 100), peça única entregue por R$ 200 (5 h),
 * despesa de R$ 30, 3 chaveiros vendidos a R$ 25 com R$ 5 de desconto (= R$ 70).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RelatoriosAgendaDocumentosTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String token;
    long cordao;
    long mandala;
    long ana;
    final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void preparar() throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"teste@atelie.local\",\"senha\":\"senha-teste-123\"}")).andReturn().getResponse().getContentAsString();
        token = json.readTree(r).get("token").asText();
        enviar(put("/api/configuracoes/precificacao"),
                "{\"salarioDesejado\":3200,\"horasMes\":160,\"custosFixosMensais\":480,\"margemPadrao\":30}", status().isOk());
        enviar(put("/api/configuracoes"), """
                {"nomeAtelie":"Ateliê Jô Monteiro","whatsapp":"98988793891","chavePix":"jo@pix.com","cidade":"Governador Nunes Freire — MA"}""",
                status().isOk());
        cordao = id(enviar(post("/api/materiais"),
                "{\"nome\":\"Cordão\",\"unidade\":\"metro\",\"quantidade\":200,\"estoqueMinimo\":0,\"custoUnitario\":0.5}", status().isCreated()));
        mandala = id(enviar(post("/api/produtos"), """
                {"nome":"Mandala","frente":"PAPELARIA","horasMaoObra":3,"materiais":[{"materialId":%d,"quantidade":40}]}
                """.formatted(cordao), status().isCreated()));
        ana = id(enviar(post("/api/clientes"), """
                {"nome":"Ana Paula","whatsapp":"98981112233","aniversario":"%s"}""".formatted(hoje.withYear(1990)), status().isCreated()));
    }

    @Test
    void relatorioDoPeriodoComLucroPorPecaProntaEntregaMateriaisEExportacoes() throws Exception {
        long e1 = id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"Mandala sala","frente":"PAPELARIA","produtoId":%d,"quantidade":1,"valorTotal":150,"valorSinal":50}
                """.formatted(ana, mandala), status().isCreated()));
        enviar(post("/api/encomendas/" + e1 + "/pagamentos"), "{\"valor\":50}", status().isOk());
        enviar(post("/api/encomendas/" + e1 + "/pagamentos"), "{\"valor\":100}", status().isOk());
        JsonNode ent = enviar(patch("/api/encomendas/" + e1 + "/status"), "{\"status\":\"ENTREGUE\"}", status().isOk());
        assertThat(ent.get("entregueEm").asText()).isEqualTo(hoje.toString());

        long e2 = id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"Cortina única","frente":"PAPELARIA","horasTrabalhadas":5,"valorTotal":200,"valorSinal":0}
                """.formatted(ana), status().isCreated()));
        enviar(post("/api/encomendas/" + e2 + "/pagamentos"), "{\"valor\":200}", status().isOk());
        enviar(patch("/api/encomendas/" + e2 + "/status"), "{\"status\":\"ENTREGUE\"}", status().isOk());

        enviar(post("/api/lancamentos"), """
                {"tipo":"SAIDA","categoria":"MATERIAL","descricao":"Embalagens","valor":30,"data":"%s"}""".formatted(hoje), status().isCreated());
        long chaveiro = id(enviar(post("/api/produtos"), """
                {"nome":"Chaveiro","frente":"PRESENTE","horasMaoObra":0.5,"precoVenda":25,"prontaEntrega":true,
                 "materiais":[{"materialId":%d,"quantidade":2}]}""".formatted(cordao), status().isCreated()));
        enviar(post("/api/produtos/" + chaveiro + "/producao"), "{\"quantidade\":4}", status().isOk());
        enviar(post("/api/vendas"), "{\"desconto\":5,\"itens\":[{\"produtoId\":%d,\"quantidade\":3}]}".formatted(chaveiro), status().isCreated());

        JsonNode r = get("/api/relatorios?de=" + hoje + "&ate=" + hoje);
        assertThat(r.get("entradas").get("atual").decimalValue()).isEqualByComparingTo("420");
        assertThat(r.get("saidas").get("atual").decimalValue()).isEqualByComparingTo("30");
        assertThat(r.get("resultado").get("atual").decimalValue()).isEqualByComparingTo("390");
        assertThat(r.get("entradas").get("variacaoPercentual").isNull()).isTrue();
        assertThat(r.get("encomendasEntregues").asInt()).isEqualTo(2);
        assertThat(r.get("ticketMedio").decimalValue()).isEqualByComparingTo("175");

        JsonNode pecas = r.get("porPeca");
        assertThat(pecas.get(0).get("nome").asText()).isEqualTo("Peças únicas (sem ficha)");
        assertThat(pecas.get(0).get("ganho").decimalValue()).isEqualByComparingTo("185");     // 200 − 15 fixos
        assertThat(pecas.get(0).get("ganhoPorHora").decimalValue()).isEqualByComparingTo("37");
        assertThat(pecas.get(1).get("nome").asText()).isEqualTo("Mandala");
        assertThat(pecas.get(1).get("materiais").decimalValue()).isEqualByComparingTo("20");
        assertThat(pecas.get(1).get("ganho").decimalValue()).isEqualByComparingTo("121");     // 150 − 20 − 9
        assertThat(pecas.get(1).get("ganhoPorHora").decimalValue()).isEqualByComparingTo("40.33");

        JsonNode pe = r.get("prontaEntrega").get(0);
        assertThat(pe.get("pecas").asInt()).isEqualTo(3);
        assertThat(pe.get("faturamento").decimalValue()).isEqualByComparingTo("75");
        assertThat(pe.get("custo").decimalValue()).isEqualByComparingTo("3");
        assertThat(r.get("descontosVendas").decimalValue()).isEqualByComparingTo("5");
        assertThat(r.get("maisVendidos").get(0).get("nome").asText()).isEqualTo("Chaveiro");

        JsonNode mat = r.get("materiais").get(0);
        assertThat(mat.get("quantidade").decimalValue()).isEqualByComparingTo("48");          // 40 + 4 × 2
        assertThat(mat.get("custo").decimalValue()).isEqualByComparingTo("24");

        assertThat(r.get("porFrente").get(0).get("nome").asText()).isEqualTo("Papelaria");
        assertThat(r.get("porFrente").get(0).get("percentual").asInt()).isEqualTo(83);
        assertThat(r.get("evolucao")).hasSize(12);
        assertThat(r.get("evolucao").get(11).get("entradas").decimalValue()).isEqualByComparingTo("420");

        MockHttpServletResponse xlsx = baixar("/api/relatorios/exportar?formato=xlsx&de=" + hoje + "&ate=" + hoje);
        assertThat(xlsx.getContentType()).contains("spreadsheetml");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx.getContentAsByteArray()))) {
            assertThat(wb.getSheet("Resumo")).isNotNull();
            assertThat(wb.getSheet("Lançamentos").getLastRowNum()).isEqualTo(5); // cabeçalho + 5 lançamentos
            assertThat(wb.getSheet("Resumo").getRow(4).getCell(1).getNumericCellValue()).isEqualTo(420.0);
        }
        MockHttpServletResponse pdf = baixar("/api/relatorios/exportar?formato=pdf&de=" + hoje + "&ate=" + hoje);
        assertThat(texto(pdf.getContentAsByteArray())).contains("Relatório").contains("Mandala").contains("Chaveiro");

        enviar(get2("/api/relatorios?de=" + hoje + "&ate=" + hoje.minusDays(1)), null, status().isBadRequest());
        enviar(get2("/api/relatorios?de=ontem&ate=" + hoje), null, status().isBadRequest());
    }

    @Test
    void propostaEContratoEmPdf() throws Exception {
        mvc.perform(multipart("/api/publico/orcamentos").file(new MockMultipartFile("dados", "", "application/json",
                        "{\"nome\":\"Ana Paula\",\"whatsapp\":\"98981112233\",\"tipoPeca\":\"Kit Dia dos Professores\",\"quantidade\":12}".getBytes())))
                .andExpect(status().isCreated());
        long orc = get("/api/orcamentos").get(0).get("id").asLong();
        enviar(get2("/api/orcamentos/" + orc + "/proposta"), null, status().isBadRequest()); // sem valor

        enviar(put("/api/configuracoes/documentos"), """
                {"responsavel":"Jô Monteiro","documento":"12.345.678/0001-90","sinalPercentual":40,"validadePropostaDias":10}""",
                status().isOk());
        enviar(put("/api/orcamentos/" + orc), """
                {"nome":"Ana Paula","whatsapp":"98981112233","tipoPeca":"Decoração de casamento ou evento","medidas":"arco 2 m","valorProposto":1000}""",
                status().isOk());
        MockHttpServletResponse prop = baixar("/api/orcamentos/" + orc + "/proposta");
        assertThat(prop.getContentType()).isEqualTo("application/pdf");
        assertThat(prop.getHeader("Content-Disposition")).contains("Proposta-0001-Ana-Paula.pdf");
        String tp = texto(prop.getContentAsByteArray());
        assertThat(tp).contains("Proposta").contains("Ana Paula").contains("1.000,00").contains("400,00").contains("600,00")
                .contains("jo@pix.com").contains(LocalDate.now().plusDays(10).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        long enc = id(enviar(post("/api/orcamentos/" + orc + "/aprovar"),
                "{\"descricao\":\"Arco de cerimônia\",\"frente\":\"PORCELANA\",\"valorTotal\":1000,\"valorSinal\":400,\"prazoEntrega\":\"%s\"}"
                        .formatted(hoje.plusDays(30)), status().isOk()));
        MockHttpServletResponse contrato = baixar("/api/encomendas/" + enc + "/contrato");
        String tc = texto(contrato.getContentAsByteArray());
        assertThat(tc).contains("Contrato de encomenda").contains("Arco de cerimônia").contains("12.345.678/0001-90")
                .contains("Ana Paula").contains("comarca de Governador Nunes Freire").contains("o sinal não é devolvido");

        // Dados de contrato e precificação não vão para o site
        String site = mvc.perform(MockMvcRequestBuilders.get("/api/publico/site")).andReturn().getResponse().getContentAsString();
        assertThat(site).doesNotContain("12.345.678").doesNotContain("politicaCancelamento");
        mvc.perform(MockMvcRequestBuilders.get("/api/encomendas/" + enc + "/contrato")).andExpect(status().isUnauthorized());
    }

    @Test
    void agendaJuntaEntregasCompromissosEAniversariosELembretesSomemAoMarcar() throws Exception {
        enviar(post("/api/compromissos"), """
                {"titulo":"Reunião com a escola","tipo":"REUNIAO","inicio":"%s","local":"Escola Pequeno Príncipe","clienteId":%d}
                """.formatted(hoje.plusDays(2).atTime(10, 0), ana), status().isCreated());
        long enc = id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"Painel 80 cm","frente":"PAPELARIA","valorTotal":300,"valorSinal":100,"prazoEntrega":"%s"}
                """.formatted(ana, hoje.plusDays(3)), status().isCreated()));
        enviar(post("/api/encomendas/" + enc + "/pagamentos"), "{\"valor\":100}", status().isOk());
        enviar(patch("/api/encomendas/" + enc + "/status"), "{\"status\":\"PRONTA\"}", status().isOk());

        JsonNode ag = get("/api/agenda?de=" + hoje + "&ate=" + hoje.plusDays(7));
        List<String> tipos = new ArrayList<>();
        ag.forEach(ev -> tipos.add(ev.get("tipo").asText()));
        assertThat(tipos).containsExactly("ANIVERSARIO", "COMPROMISSO", "ENTREGA");
        assertThat(ag.get(1).get("hora").asText()).startsWith("10:00");

        JsonNode lem = get("/api/lembretes");
        List<String> chaves = new ArrayList<>();
        lem.forEach(l -> chaves.add(l.get("chave").asText()));
        assertThat(chaves).contains("PRONTA:" + enc, "ANIV:" + ana + ":" + hoje.getYear());
        JsonNode pronta = null;
        for (JsonNode l : lem) {
            if (l.get("tipo").asText().equals("PRONTA")) {
                pronta = l;
            }
        }
        assertThat(pronta.get("mensagem").asText()).contains("está pronta").contains("200,00").contains("jo@pix.com");
        assertThat(get("/api/painel").get("lembretes").asInt()).isEqualTo(lem.size());

        enviar(post("/api/lembretes/marcar"), "{\"chave\":\"PRONTA:" + enc + "\"}", status().isNoContent());
        assertThat(get("/api/lembretes").size()).isEqualTo(lem.size() - 1);
        enviar(post("/api/lembretes/marcar"), "{\"chave\":\"<script>\"}", status().isBadRequest());

        enviar(get2("/api/agenda?de=" + hoje + "&ate=" + hoje.plusDays(200)), null, status().isBadRequest());
    }

    @Test
    void fotosDaEncomendaSaoPrivadasESoAProntaVaiParaOPortfolio() throws Exception {
        long enc = id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"Painel com galho","frente":"PAPELARIA","produtoId":%d,"valorTotal":300,"valorSinal":0}
                """.formatted(ana, mandala), status().isCreated()));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
        long ref = id(json.readTree(mvc.perform(multipart("/api/encomendas/" + enc + "/fotos")
                        .file(new MockMultipartFile("foto", "ref.png", "image/png", png)).param("tipo", "REFERENCIA")
                        .param("legenda", "Foto que a cliente mandou").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()));
        long pronta = id(json.readTree(mvc.perform(multipart("/api/encomendas/" + enc + "/fotos")
                        .file(new MockMultipartFile("foto", "pronta.png", "image/png", png)).param("tipo", "PRONTA")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()));

        mvc.perform(MockMvcRequestBuilders.get("/api/fotos/" + ref + "/arquivo")).andExpect(status().isUnauthorized());
        MockHttpServletResponse img = baixar("/api/fotos/" + ref + "/arquivo");
        assertThat(img.getContentType()).isEqualTo("image/png");
        assertThat(img.getHeader("Cache-Control")).contains("no-store");

        enviar(post("/api/fotos/" + ref + "/publicar"), "{}", status().isBadRequest());
        JsonNode pub = enviar(post("/api/fotos/" + pronta + "/publicar"), "{}", status().isOk());
        assertThat(pub.get("portfolioItemId").isNull()).isFalse();
        enviar(post("/api/fotos/" + pronta + "/publicar"), "{}", status().isBadRequest());

        JsonNode site = json.readTree(mvc.perform(MockMvcRequestBuilders.get("/api/publico/site")).andReturn().getResponse().getContentAsString());
        assertThat(site.get("portfolio")).hasSize(1);
        assertThat(site.get("portfolio").get(0).get("titulo").asText()).isEqualTo("Mandala");
        assertThat(site.get("portfolio").get(0).get("categoria").asText()).isEqualTo("PAPELARIA");
        mvc.perform(MockMvcRequestBuilders.get(site.get("portfolio").get(0).get("url").asText())).andExpect(status().isOk());

        // Apagar a foto da encomenda não tira a cópia do portfólio
        mvc.perform(auth(delete("/api/fotos/" + pronta))).andExpect(status().isNoContent());
        assertThat(get("/api/encomendas/" + enc + "/fotos")).hasSize(1);
        assertThat(get("/api/portfolio")).hasSize(1);

        // Encomenda com fotos não pode ser excluída por engano
        mvc.perform(auth(delete("/api/encomendas/" + enc))).andExpect(status().isBadRequest());
    }

    // ---------- apoio ----------

    private static String texto(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        StringBuilder sb = new StringBuilder();
        PdfTextExtractor ex = new PdfTextExtractor(reader);
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            sb.append(ex.getTextFromPage(i)).append('\n');
        }
        reader.close();
        return sb.toString().replace(' ', ' ');
    }

    private MockHttpServletResponse baixar(String url) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.get(url))).andExpect(status().isOk()).andReturn().getResponse();
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
