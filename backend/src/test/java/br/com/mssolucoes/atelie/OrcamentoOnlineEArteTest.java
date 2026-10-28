package br.com.mssolucoes.atelie;

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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Etapa 6: orçamento online com personalização e fotos; aprovação de arte pelo link. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrcamentoOnlineEArteTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String token;
    long cliente;

    @BeforeEach
    void preparar() throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"teste@atelie.local\",\"senha\":\"senha-teste-123\"}")).andReturn().getResponse().getContentAsString();
        token = json.readTree(r).get("token").asText();
        enviar(put("/api/configuracoes"), """
                {"nomeAtelie":"Ateliê Jô Monteiro","whatsapp":"98988887777","chavePix":"jo@pix.com","cidade":"Governador Nunes Freire — MA"}""",
                status().isOk());
        cliente = id(enviar(post("/api/clientes"), "{\"nome\":\"Carla Souza\",\"whatsapp\":\"98981234567\"}", status().isCreated()));
    }

    @Test
    void orcamentoDoSiteComPecaDoCatalogoPersonalizacaoEFotosViraEncomendaComArteACriar() throws Exception {
        long caderneta = id(enviar(post("/api/produtos"), """
                {"nome":"Caderneta personalizada","frente":"PAPELARIA","horasMaoObra":1,"precoVenda":35}""", status().isCreated()));
        long escondida = id(enviar(post("/api/produtos"), """
                {"nome":"Peça fora do catálogo","frente":"PAPELARIA","horasMaoObra":1}""", status().isCreated()));
        enviar(put("/api/produtos/" + caderneta + "/catalogo"), "{\"noCatalogo\":true}", status().isOk());

        // Pedido pelo site com duas fotos de referência
        String dados = """
                {"nome":"Professora Lúcia","whatsapp":"(98) 98111-2233","tipoPeca":"Caderneta","produtoId":%d,"quantidade":12,
                 "personalizacao":"Nome de cada aluno na capa, tema fundo do mar","dataDesejada":"%s"}
                """.formatted(caderneta, LocalDate.now().plusDays(20));
        mvc.perform(multipart("/api/publico/orcamentos")
                        .file(new MockMultipartFile("dados", "", "application/json", dados.getBytes()))
                        .file(new MockMultipartFile("fotos", "a.png", "image/png", PNG))
                        .file(new MockMultipartFile("fotos", "b.png", "image/png", PNG)))
                .andExpect(status().isCreated());

        // Arquivo que não é imagem: recusa o pedido inteiro
        mvc.perform(multipart("/api/publico/orcamentos")
                        .file(new MockMultipartFile("dados", "", "application/json", dados.getBytes()))
                        .file(new MockMultipartFile("fotos", "x.png", "image/png", "<script>".getBytes())))
                .andExpect(status().isBadRequest());
        // Mais de 3 fotos
        MockMultipartHttpServletRequestBuilder quatro = multipart("/api/publico/orcamentos")
                .file(new MockMultipartFile("dados", "", "application/json", dados.getBytes()));
        for (int i = 0; i < 4; i++) {
            quatro.file(new MockMultipartFile("fotos", i + ".png", "image/png", PNG));
        }
        mvc.perform(quatro).andExpect(status().isBadRequest());
        // Peça que não está no catálogo não é ligada ao pedido
        mvc.perform(multipart("/api/publico/orcamentos").file(new MockMultipartFile("dados", "", "application/json",
                ("{\"nome\":\"Rui\",\"whatsapp\":\"98981110000\",\"tipoPeca\":\"Caneca\",\"produtoId\":" + escondida + "}").getBytes())))
                .andExpect(status().isCreated());

        JsonNode lista = ler("/api/orcamentos");
        assertThat(lista).hasSize(2);
        JsonNode rui = lista.get(0);
        assertThat(rui.get("produtoId").isNull()).isTrue();
        JsonNode o = lista.get(1);
        assertThat(o.get("produtoNome").asText()).isEqualTo("Caderneta personalizada");
        assertThat(o.get("quantidade").asInt()).isEqualTo(12);
        assertThat(o.get("personalizacao").asText()).contains("fundo do mar");
        assertThat(o.get("fotos")).hasSize(2);
        String urlFoto = o.get("fotos").get(0).get("url").asText();
        mvc.perform(auth(get(urlFoto))).andExpect(status().isOk());
        mvc.perform(get(urlFoto)).andExpect(status().isUnauthorized());

        // Aprovar: a encomenda herda peça, quantidade, personalização e fotos; a arte fica "a criar"
        JsonNode enc = enviar(post("/api/orcamentos/" + id(o) + "/aprovar"), """
                {"descricao":"12 cadernetas fundo do mar","frente":"PAPELARIA","valorTotal":420,"valorSinal":210}""",
                status().isOk());
        assertThat(enc.get("produtoNome").asText()).isEqualTo("Caderneta personalizada");
        assertThat(enc.get("quantidade").asInt()).isEqualTo(12);
        assertThat(enc.get("personalizacao").asText()).contains("fundo do mar");
        assertThat(enc.get("statusArte").asText()).isEqualTo("A_CRIAR");
        JsonNode fotos = ler("/api/encomendas/" + id(enc) + "/fotos");
        assertThat(fotos).hasSize(2);
        assertThat(fotos.get(0).get("tipo").asText()).isEqualTo("REFERENCIA");
        mvc.perform(auth(get(fotos.get(0).get("url").asText()))).andExpect(status().isOk());
        assertThat(ler("/api/painel").get("artesAFazer").asLong()).isEqualTo(1);

        // Excluir o orçamento recusado apaga também as fotos dele (o do Rui não tem)
        enviar(delete("/api/orcamentos/" + id(rui)), null, status().isNoContent());
    }

    @Test
    void arteVaiPeloLinkClientePedeAjusteDepoisAprovaEHistoricoFica() throws Exception {
        long enc = id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"Caneca Dia das Mães","frente":"PORCELANA","valorTotal":60,"valorSinal":0,
                 "personalizacao":"Foto da família e a frase 'Mãe, você é o nosso lar'","precisaArte":true}""".formatted(cliente),
                status().isCreated()));
        assertThat(ler("/api/encomendas/" + enc).get("statusArte").asText()).isEqualTo("A_CRIAR");
        assertThat(ler("/api/encomendas/" + enc + "/artes").get("link").isNull()).isTrue();

        // Versão 1
        JsonNode artes = enviarArte(enc, "Primeira ideia, com a foto no centro");
        assertThat(artes.get("status").asText()).isEqualTo("AGUARDANDO_CLIENTE");
        String link = artes.get("link").asText();
        String codigo = link.substring(link.lastIndexOf('/') + 1);
        long v1 = id(artes.get("versoes").get(0));
        assertThat(ler("/api/painel").get("artesComCliente").asLong()).isEqualTo(1);

        // Lembrete com a mensagem e o link
        JsonNode lembrete = lembrete("ARTE:" + v1);
        assertThat(lembrete).isNotNull();
        assertThat(lembrete.get("mensagem").asText()).contains("Carla").contains("/arte/" + codigo);

        // Página da cliente (sem login)
        JsonNode pub = publico("/api/publico/artes/" + codigo, status().isOk());
        assertThat(pub.get("cliente").asText()).isEqualTo("Carla");
        assertThat(pub.get("descricao").asText()).isEqualTo("Caneca Dia das Mães");
        assertThat(pub.get("versoes")).hasSize(1);
        assertThat(pub.get("versoes").get(0).get("atual").asBoolean()).isTrue();
        assertThat(pub.toString()).doesNotContain("valorTotal").doesNotContain("98981234567");
        mvc.perform(get(pub.get("versoes").get(0).get("url").asText())).andExpect(status().isOk());
        mvc.perform(get("/api/publico/artes/" + codigo + "/versoes/999/arquivo")).andExpect(status().isNotFound());
        mvc.perform(get("/api/publico/artes/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
        mvc.perform(get("/api/publico/artes/abc")).andExpect(status().isNotFound());
        mvc.perform(get("/api/artes/" + v1 + "/arquivo")).andExpect(status().isUnauthorized());

        // Ajuste sem dizer o quê: recusa; com comentário: registra
        responder(codigo, v1, false, null, status().isBadRequest());
        JsonNode aj = responder(codigo, v1, false, "Pode trocar a fonte da frase por uma mais delicada?", status().isOk());
        assertThat(aj.get("status").asText()).isEqualTo("AJUSTE_PEDIDO");
        responder(codigo, v1, true, null, status().isBadRequest());     // já respondida
        assertThat(ler("/api/painel").get("ajustesPedidos").asLong()).isEqualTo(1);
        enviar(delete("/api/artes/" + v1), null, status().isBadRequest()); // respondida fica no histórico

        // Versão 2 com o ajuste
        JsonNode artes2 = enviarArte(enc, "Troquei a fonte");
        long v2 = id(artes2.get("versoes").get(0));
        assertThat(artes2.get("versoes")).hasSize(2);
        assertThat(artes2.get("versoes").get(0).get("numero").asInt()).isEqualTo(2);
        assertThat(lembrete("ARTE:" + v2).get("mensagem").asText()).contains("já com os ajustes");

        // Responder a versão antiga não vale
        responder(codigo, v1, true, null, status().isBadRequest());
        JsonNode ok = responder(codigo, v2, true, "Ficou perfeita!", status().isOk());
        assertThat(ok.get("status").asText()).isEqualTo("APROVADA");
        assertThat(ok.get("versoes").get(0).get("resposta").asText()).isEqualTo("APROVADA");
        assertThat(ok.get("versoes").get(1).get("comentarioCliente").asText()).contains("fonte");
        assertThat(ler("/api/encomendas/" + enc).get("statusArte").asText()).isEqualTo("APROVADA");
        assertThat(lembrete("ARTE:" + v2)).isNull();                    // aprovada: some dos lembretes
        assertThat(ler("/api/painel").get("artesComCliente").asLong()).isZero();

        // Encomenda cancelada: o link deixa de abrir
        enviar(patch("/api/encomendas/" + enc + "/status"), "{\"status\":\"CANCELADA\"}", status().isOk());
        mvc.perform(get("/api/publico/artes/" + codigo)).andExpect(status().isNotFound());
    }

    @Test
    void respostaPeloWhatsappMarcadaNoSistemaEExclusaoDaUltimaVersao() throws Exception {
        long enc = id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"descricao":"Bíblia com nome","frente":"PAPELARIA","valorTotal":90,"valorSinal":0,"precisaArte":true}
                """.formatted(cliente), status().isCreated()));

        // Sem versão: ajuste não faz sentido; aprovar direto pode (arte combinada fora do sistema)
        enviar(patch("/api/encomendas/" + enc + "/arte"), "{\"status\":\"AJUSTE_PEDIDO\",\"comentario\":\"x\"}", status().isBadRequest());
        enviar(patch("/api/encomendas/" + enc + "/arte"), "{\"status\":\"SEM_ARTE\"}", status().isOk());
        assertThat(ler("/api/encomendas/" + enc).get("statusArte").asText()).isEqualTo("SEM_ARTE");
        enviar(patch("/api/encomendas/" + enc + "/arte"), "{\"status\":\"A_CRIAR\"}", status().isOk());

        long v1 = id(enviarArte(enc, null).get("versoes").get(0));
        enviar(patch("/api/encomendas/" + enc + "/arte"), "{\"status\":\"SEM_ARTE\"}", status().isBadRequest());
        enviar(patch("/api/encomendas/" + enc + "/arte"), "{\"status\":\"AJUSTE_PEDIDO\"}", status().isBadRequest()); // sem comentário
        JsonNode aj = enviar(patch("/api/encomendas/" + enc + "/arte"),
                "{\"status\":\"AJUSTE_PEDIDO\",\"comentario\":\"Ela pediu letra dourada (WhatsApp)\"}", status().isOk());
        assertThat(aj.get("versoes").get(0).get("respondidaNoSistema").asBoolean()).isTrue();

        // A versão 2 ainda não foi vista: pode excluir, e a situação volta a ser a da versão 1
        long v2 = id(enviarArte(enc, "Dourado").get("versoes").get(0));
        JsonNode depois = enviar(delete("/api/artes/" + v2), null, status().isOk());
        assertThat(depois.get("status").asText()).isEqualTo("AJUSTE_PEDIDO");
        assertThat(depois.get("versoes")).hasSize(1);

        long v3 = id(enviarArte(enc, "Dourado de novo").get("versoes").get(0));
        assertThat(v3).isNotEqualTo(v1);
        JsonNode aprov = enviar(patch("/api/encomendas/" + enc + "/arte"), "{\"status\":\"APROVADA\"}", status().isOk());
        assertThat(aprov.get("status").asText()).isEqualTo("APROVADA");
        assertThat(aprov.get("versoes").get(0).get("resposta").asText()).isEqualTo("APROVADA");

        // Não exclui encomenda com arte (cancela em vez disso)
        enviar(delete("/api/encomendas/" + enc), null, status().isBadRequest());
    }

    // ---------- apoio ----------

    private JsonNode enviarArte(long enc, String obs) throws Exception {
        MockMultipartHttpServletRequestBuilder b = multipart("/api/encomendas/" + enc + "/artes");
        b.file(new MockMultipartFile("arquivo", "arte.png", "image/png", PNG));
        if (obs != null) {
            b.param("observacao", obs);
        }
        return json.readTree(mvc.perform(b.header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode responder(String codigo, long versao, boolean aprovar, String comentario, ResultMatcher esperado)
            throws Exception {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("versaoId", versao);
        m.put("aprovar", aprovar);
        if (comentario != null) {
            m.put("comentario", comentario);
        }
        String corpo = json.writeValueAsString(m);
        String r = mvc.perform(post("/api/publico/artes/" + codigo + "/resposta").contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(esperado).andReturn().getResponse().getContentAsString();
        return r.isBlank() ? null : json.readTree(r);
    }

    private JsonNode publico(String url, ResultMatcher esperado) throws Exception {
        return json.readTree(mvc.perform(get(url)).andExpect(esperado).andReturn().getResponse().getContentAsString());
    }

    private JsonNode lembrete(String chave) throws Exception {
        List<JsonNode> achados = new ArrayList<>();
        ler("/api/lembretes").forEach(l -> {
            if (l.get("chave").asText().equals(chave)) {
                achados.add(l);
            }
        });
        return achados.isEmpty() ? null : achados.get(0);
    }

    private JsonNode ler(String url) throws Exception {
        return json.readTree(mvc.perform(auth(get(url)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode enviar(MockHttpServletRequestBuilder b, String corpo, ResultMatcher esperado) throws Exception {
        MockHttpServletRequestBuilder req = auth(b);
        if (corpo != null) {
            req.content(corpo);
        }
        String r = mvc.perform(req).andExpect(esperado).andReturn().getResponse().getContentAsString();
        return r.isBlank() ? null : json.readTree(r);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private static long id(JsonNode n) {
        return n.get("id").asLong();
    }
}
