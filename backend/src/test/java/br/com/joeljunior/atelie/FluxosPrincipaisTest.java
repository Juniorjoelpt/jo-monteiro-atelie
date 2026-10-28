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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FluxosPrincipaisTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String token;

    @BeforeEach
    void entrar() throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teste@atelie.local\",\"senha\":\"senha-teste-123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = json.readTree(r).get("token").asText();
    }

    @Test
    void rotasDeGestaoExigemLogin() throws Exception {
        mvc.perform(get("/api/painel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/publico/site")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"teste@atelie.local\",\"senha\":\"errada\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void orcamentoDoSiteViraEncomendaEPagamentoAtualizaSaldo() throws Exception {
        var dados = new MockMultipartFile("dados", "", "application/json", """
                {"nome":"Ana Paula","whatsapp":"(98) 98111-2233","tipoPeca":"Caneca personalizada",
                 "cores":"rosa e dourado","dataDesejada":"%s","detalhes":"Para o aniversário da minha mãe"}
                """.formatted(LocalDate.now().plusDays(30)).getBytes());
        mvc.perform(multipart("/api/publico/orcamentos").file(dados)).andExpect(status().isCreated());

        JsonNode lista = getJson("/api/orcamentos");
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("status").asText()).isEqualTo("NOVO");
        assertThat(lista.get(0).get("whatsapp").asText()).isEqualTo("5598981112233");
        long id = lista.get(0).get("id").asLong();

        JsonNode enc = json.readTree(mvc.perform(auth(post("/api/orcamentos/" + id + "/aprovar")).content("""
                {"descricao":"Caneca com foto","frente":"PORCELANA","valorTotal":1200,"valorSinal":400}
                """)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(enc.get("status").asText()).isEqualTo("AGUARDANDO_SINAL");
        assertThat(enc.get("saldo").decimalValue()).isEqualByComparingTo("1200");
        long encId = enc.get("id").asLong();

        // não aprova duas vezes
        mvc.perform(auth(post("/api/orcamentos/" + id + "/aprovar")).content("""
                {"descricao":"x","frente":"PORCELANA","valorTotal":1}
                """)).andExpect(status().isBadRequest());

        JsonNode depois = json.readTree(mvc.perform(auth(post("/api/encomendas/" + encId + "/pagamentos"))
                        .content("{\"valor\":400}")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(depois.get("status").asText()).isEqualTo("EM_PRODUCAO");
        assertThat(depois.get("saldo").decimalValue()).isEqualByComparingTo("800");

        // não aceita pagamento acima do saldo
        mvc.perform(auth(post("/api/encomendas/" + encId + "/pagamentos")).content("{\"valor\":900}"))
                .andExpect(status().isBadRequest());

        JsonNode fin = getJson("/api/financeiro");
        assertThat(fin.get("entradas").decimalValue()).isEqualByComparingTo("400");
        assertThat(fin.get("aReceber").decimalValue()).isEqualByComparingTo("800");

        JsonNode clientes = getJson("/api/clientes");
        assertThat(clientes).hasSize(1);
        assertThat(clientes.get(0).get("tipo").asText()).isEqualTo("CLIENTE");

        JsonNode painel = getJson("/api/painel");
        assertThat(painel.get("emProducao").asLong()).isEqualTo(1);
        assertThat(painel.get("aReceber").decimalValue()).isEqualByComparingTo("800");
    }

    @Test
    void compraDeMaterialViraDespesaEAlertaDeEstoque() throws Exception {
        JsonNode m = json.readTree(mvc.perform(auth(post("/api/materiais")).content("""
                {"nome":"Cordão cru 5 mm","unidade":"rolo","quantidade":3,"estoqueMinimo":2,"custoUnitario":45}
                """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id = m.get("id").asLong();
        assertThat(m.get("abaixoDoMinimo").asBoolean()).isFalse();

        mvc.perform(auth(post("/api/materiais/" + id + "/ajuste")).content("{\"tipo\":\"SAIDA\",\"quantidade\":1}"))
                .andExpect(status().isOk());
        assertThat(getJson("/api/painel").get("materiaisBaixos")).hasSize(1);

        mvc.perform(auth(post("/api/materiais/" + id + "/ajuste")).content("{\"tipo\":\"SAIDA\",\"quantidade\":5}"))
                .andExpect(status().isBadRequest());

        mvc.perform(auth(post("/api/materiais/" + id + "/ajuste"))
                .content("{\"tipo\":\"ENTRADA\",\"quantidade\":4,\"registrarDespesa\":true}")).andExpect(status().isOk());
        JsonNode fin = getJson("/api/financeiro");
        assertThat(fin.get("saidas").decimalValue()).isEqualByComparingTo("180");
        assertThat(getJson("/api/painel").get("materiaisBaixos")).isEmpty();
    }

    @Test
    void fotoDoPortfolioApareceNoSite() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
        var dados = new MockMultipartFile("dados", "", "application/json",
                "{\"titulo\":\"Painel mandala\",\"categoria\":\"PAPELARIA\"}".getBytes());
        var foto = new MockMultipartFile("foto", "mandala.png", "image/png", png);
        mvc.perform(multipart("/api/portfolio").file(dados).file(foto).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        var falsa = new MockMultipartFile("foto", "x.png", "image/png", "<script>".getBytes());
        mvc.perform(multipart("/api/portfolio").file(dados).file(falsa).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        JsonNode site = json.readTree(mvc.perform(get("/api/publico/site")).andReturn().getResponse().getContentAsString());
        assertThat(site.get("portfolio")).hasSize(1);
        String url = site.get("portfolio").get(0).get("url").asText();
        mvc.perform(get(url)).andExpect(status().isOk());
    }

    private JsonNode getJson(String url) throws Exception {
        return json.readTree(mvc.perform(auth(get(url))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }
}
