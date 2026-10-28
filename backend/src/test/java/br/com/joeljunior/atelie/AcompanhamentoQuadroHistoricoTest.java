package br.com.joeljunior.atelie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Etapa 8: acompanhamento do pedido pela cliente, arte vista, urgente (quadro) e histórico de quem fez o quê. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AcompanhamentoQuadroHistoricoTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String token;
    long cliente;

    @BeforeEach
    void preparar() throws Exception {
        token = login("teste@atelie.local", "senha-teste-123");
        enviar(put("/api/configuracoes"), """
                {"nomeAtelie":"Ateliê Jô Monteiro","whatsapp":"98988887777","chavePix":"jo@pix.com","titularPix":"Jô Monteiro"}""",
                status().isOk(), token);
        cliente = id(enviar(post("/api/clientes"), "{\"nome\":\"Carla Souza\",\"whatsapp\":\"98981234567\"}",
                status().isCreated(), token));
    }

    @Test
    void clienteAcompanhaOPedidoPeloLinkComEtapasDataESaldo() throws Exception {
        LocalDate entrega = LocalDate.now().plusDays(10);
        JsonNode enc = enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"Kit Dia dos Professores","frente":"PRESENTE","quantidade":12,
                 "valorTotal":300,"valorSinal":150,"prazoEntrega":"%s","precisaArte":true}""".formatted(cliente, entrega),
                status().isCreated(), token);
        long id = id(enc);
        // O link existe desde a criação, mesmo sem arte
        String link = enc.get("linkPedido").asText();
        assertThat(link).contains("/pedido/");
        String codigo = link.substring(link.lastIndexOf('/') + 1);

        JsonNode pub = publico(codigo);
        JsonNode and = pub.get("andamento");
        assertThat(and.get("status").asText()).isEqualTo("AGUARDANDO_SINAL");
        assertThat(and.get("prazoEntrega").asText()).isEqualTo(entrega.toString());
        assertThat(and.get("quantidade").asInt()).isEqualTo(12);
        assertThat(and.get("saldo").decimalValue()).isEqualByComparingTo("300");
        assertThat(and.get("valorSinal").decimalValue()).isEqualByComparingTo("150");
        assertThat(and.get("chavePix").asText()).isEqualTo("jo@pix.com");
        assertThat(pub.get("status").asText()).isEqualTo("A_CRIAR");
        assertThat(pub.toString()).doesNotContain("98981234567").doesNotContain("custos");
        // O endereço novo devolve o mesmo
        assertThat(publicoGet("/api/publico/pedidos/" + codigo).get("descricao").asText()).isEqualTo("Kit Dia dos Professores");

        // Sinal pago: entra em produção; tudo pago: some a chave Pix
        enviar(post("/api/encomendas/" + id + "/pagamentos"), "{\"valor\":150}", status().isOk(), token);
        assertThat(publico(codigo).get("andamento").get("status").asText()).isEqualTo("EM_PRODUCAO");
        enviar(post("/api/encomendas/" + id + "/pagamentos"), "{\"valor\":150}", status().isOk(), token);
        JsonNode quitado = publico(codigo).get("andamento");
        assertThat(quitado.get("saldo").decimalValue()).isEqualByComparingTo("0");
        assertThat(quitado.get("chavePix").isNull()).isTrue();

        // Entregue: mostra a data da entrega
        enviar(patch("/api/encomendas/" + id + "/status"), "{\"status\":\"ENTREGUE\"}", status().isOk(), token);
        assertThat(publico(codigo).get("andamento").get("entregueEm").asText()).isEqualTo(LocalDate.now().toString());
    }

    @Test
    void registraQuandoAClienteAbreOLinkEVeAArteSemContarQuemEstaLogada() throws Exception {
        long enc = criarEncomenda("Caneca com foto");
        String codigo = codigo(enc);
        long v1 = id(enviarArte(enc).get("versoes").get(0));
        assertThat(ler("/api/encomendas/" + enc + "/artes").get("versoes").get(0).get("vistaEm").isNull()).isTrue();

        // A Jô conferindo o link logada: não conta
        mvc.perform(post("/api/publico/artes/" + codigo + "/visto").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"versaoId\":" + v1 + "}")).andExpect(status().isNoContent());
        assertThat(ler("/api/encomendas/" + enc).get("acompanhadoEm").isNull()).isTrue();

        // A cliente abre: marca a primeira vista e conta uma visita; reabrir em seguida não conta outra
        visto(codigo, v1);
        visto(codigo, v1);
        JsonNode v = ler("/api/encomendas/" + enc + "/artes").get("versoes").get(0);
        assertThat(v.get("vistaEm").isNull()).isFalse();
        assertThat(v.get("visualizacoes").asInt()).isEqualTo(1);
        JsonNode e = ler("/api/encomendas/" + enc);
        assertThat(e.get("acompanhadoEm").isNull()).isFalse();
        assertThat(e.get("arteVistaEm").isNull()).isFalse();

        // Nova versão: começa como não vista; avisar a versão antiga não marca a nova
        long v2 = id(enviarArte(enc).get("versoes").get(0));
        visto(codigo, v1);
        assertThat(ler("/api/encomendas/" + enc + "/artes").get("versoes").get(0).get("vistaEm").isNull()).isTrue();
        assertThat(ler("/api/encomendas/" + enc).get("arteVistaEm").isNull()).isTrue();
        visto(codigo, v2);
        assertThat(ler("/api/encomendas/" + enc + "/artes").get("versoes").get(0).get("vistaEm").isNull()).isFalse();

        // Código inválido: 404
        mvc.perform(post("/api/publico/artes/00000000-0000-0000-0000-000000000000/visto")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    @Test
    void urgenteEHistoricoComQuemFezOQue() throws Exception {
        long enc = criarEncomenda("Bíblia com nome");
        String codigo = codigo(enc);

        // Urgente
        assertThat(enviar(patch("/api/encomendas/" + enc + "/urgente"), "{\"urgente\":true}", status().isOk(), token)
                .get("urgente").asBoolean()).isTrue();

        // Edição: muda a data (geral) e o valor (sensível)
        enviar(put("/api/encomendas/" + enc), """
                {"clienteId":%d,"descricao":"Bíblia com nome","frente":"PAPELARIA","valorTotal":120,"valorSinal":0,
                 "prazoEntrega":"%s","precisaArte":true,"urgente":true}""".formatted(cliente, LocalDate.now().plusDays(3)),
                status().isOk(), token);
        long v1 = id(enviarArte(enc).get("versoes").get(0));
        visto(codigo, v1);
        mvc.perform(post("/api/publico/artes/" + codigo + "/resposta").contentType(MediaType.APPLICATION_JSON)
                .content("{\"versaoId\":" + v1 + ",\"aprovar\":true,\"comentario\":\"Linda!\"}")).andExpect(status().isOk());
        enviar(post("/api/encomendas/" + enc + "/pagamentos"), "{\"valor\":50,\"formaPagamento\":\"PIX\"}", status().isOk(), token);

        List<String> linhas = textos(ler("/api/encomendas/" + enc + "/historico"));
        assertThat(linhas).anyMatch(l -> l.startsWith("Jô Monteiro") && l.contains("Criou a encomenda para Carla Souza"));
        assertThat(linhas).anyMatch(l -> l.contains("Marcou como urgente"));
        assertThat(linhas).anyMatch(l -> l.contains("Editou:") && l.contains("Entrega:"));
        assertThat(linhas).anyMatch(l -> l.contains("Mudou valores:") && l.contains("Valor total"));
        assertThat(linhas).anyMatch(l -> l.contains("Enviou a versão 1 da arte"));
        assertThat(linhas).anyMatch(l -> l.startsWith("Carla Souza (pelo link)") && l.contains("Viu a versão 1"));
        assertThat(linhas).anyMatch(l -> l.startsWith("Carla Souza (pelo link)") && l.contains("Aprovou a versão 1"));
        assertThat(linhas).anyMatch(l -> l.contains("Registrou pagamento de R$") && l.contains("Pix"));

        // Ajudante: vê a linha do tempo sem as linhas de dinheiro, não marca urgente e não vê o histórico geral
        String ajudante = criarAjudante();
        List<String> daAjudante = textos(enviar(get("/api/encomendas/" + enc + "/historico"), null, status().isOk(), ajudante));
        assertThat(daAjudante).anyMatch(l -> l.contains("Enviou a versão 1"));
        assertThat(daAjudante).noneMatch(l -> l.contains("R$"));
        enviar(patch("/api/encomendas/" + enc + "/urgente"), "{\"urgente\":false}", status().isForbidden(), ajudante);
        enviar(get("/api/historico"), null, status().isForbidden(), ajudante);

        // O que a ajudante faz fica com o nome dela
        enviar(patch("/api/encomendas/" + enc + "/status"), "{\"status\":\"PRONTA\"}", status().isOk(), ajudante);
        assertThat(textos(ler("/api/encomendas/" + enc + "/historico")))
                .anyMatch(l -> l.startsWith("Rita Ajudante") && l.contains("Em produção → Pronta"));

        // Histórico geral: filtro pela cliente (pelo link) e por área
        JsonNode soCliente = ler("/api/historico?usuario=-1");
        assertThat(soCliente).isNotEmpty();
        soCliente.forEach(h -> assertThat(h.get("cliente").asBoolean()).isTrue());
        JsonNode encomendas = ler("/api/historico?area=ENCOMENDA");
        assertThat(encomendas.get(0).get("encomenda").asText()).isEqualTo("Bíblia com nome");
    }

    @Test
    void excluirEncomendaDeixaRegistroNoHistoricoGeral() throws Exception {
        long enc = criarEncomenda("Encomenda lançada por engano");
        enviar(delete("/api/encomendas/" + enc), null, status().isNoContent(), token);
        List<String> geral = textos(ler("/api/historico"));
        assertThat(geral).anyMatch(l -> l.contains("Excluiu a encomenda “Encomenda lançada por engano” de Carla Souza"));
    }

    // ---------- apoio ----------

    private long criarEncomenda(String descricao) throws Exception {
        return id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"%s","frente":"PAPELARIA","valorTotal":90,"valorSinal":0,"precisaArte":true}
                """.formatted(cliente, descricao), status().isCreated(), token));
    }

    private String codigo(long enc) throws Exception {
        String link = ler("/api/encomendas/" + enc).get("linkPedido").asText();
        return link.substring(link.lastIndexOf('/') + 1);
    }

    private JsonNode enviarArte(long enc) throws Exception {
        return json.readTree(mvc.perform(multipart("/api/encomendas/" + enc + "/artes")
                        .file(new MockMultipartFile("arquivo", "arte.png", "image/png", PNG))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private void visto(String codigo, long versao) throws Exception {
        mvc.perform(post("/api/publico/artes/" + codigo + "/visto").contentType(MediaType.APPLICATION_JSON)
                .content("{\"versaoId\":" + versao + "}")).andExpect(status().isNoContent());
    }

    private JsonNode publico(String codigo) throws Exception {
        return publicoGet("/api/publico/artes/" + codigo);
    }

    private JsonNode publicoGet(String url) throws Exception {
        return json.readTree(mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String criarAjudante() throws Exception {
        enviar(post("/api/equipe"), "{\"nome\":\"Rita Ajudante\",\"email\":\"rita@atelie.local\",\"perfil\":\"AJUDANTE\",\"senha\":\"rita-senha-1\"}",
                status().isCreated(), token);
        return login("rita@atelie.local", "rita-senha-1");
    }

    private String login(String email, String senha) throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("token").asText();
    }

    /** "autor: descrição" de cada linha do histórico. */
    private static List<String> textos(JsonNode lista) {
        List<String> r = new ArrayList<>();
        lista.forEach(h -> r.add(h.get("autor").asText() + ": " + h.get("descricao").asText()));
        return r;
    }

    private JsonNode ler(String url) throws Exception {
        return enviar(get(url), null, status().isOk(), token);
    }

    private JsonNode enviar(MockHttpServletRequestBuilder b, String corpo, ResultMatcher esperado, String tk) throws Exception {
        MockHttpServletRequestBuilder req = b.header("Authorization", "Bearer " + tk).contentType(MediaType.APPLICATION_JSON);
        if (corpo != null) {
            req.content(corpo);
        }
        String r = mvc.perform(req).andExpect(esperado).andReturn().getResponse().getContentAsString();
        return r.isBlank() ? null : json.readTree(r);
    }

    private static long id(JsonNode n) {
        return n.get("id").asLong();
    }
}
