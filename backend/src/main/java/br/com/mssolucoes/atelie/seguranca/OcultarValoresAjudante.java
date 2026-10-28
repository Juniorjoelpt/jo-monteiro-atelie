package br.com.mssolucoes.atelie.seguranca;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Iterator;
import java.util.Map;

/**
 * Para a ajudante, tira das respostas da API os campos de dinheiro (custos, valor cobrado, saldo).
 * Fica num só lugar: qualquer tela nova já nasce protegida.
 */
@RestControllerAdvice
public class OcultarValoresAjudante implements ResponseBodyAdvice<Object> {

    private final ObjectMapper json;

    public OcultarValoresAjudante(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public boolean supports(MethodParameter tipo, Class<? extends HttpMessageConverter<?>> conversor) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(conversor);
    }

    @Override
    public Object beforeBodyWrite(Object corpo, MethodParameter tipo, MediaType midia,
                                  Class<? extends HttpMessageConverter<?>> conversor,
                                  ServerHttpRequest req, ServerHttpResponse resp) {
        if (corpo == null || !ehAjudante() || !req.getURI().getPath().startsWith("/api/")
                || req.getURI().getPath().startsWith("/api/auth/") || corpo instanceof Map<?, ?> m && m.containsKey("erro")) {
            return corpo;
        }
        JsonNode arvore = json.valueToTree(corpo);
        limpar(arvore);
        return arvore;
    }

    private static boolean ehAjudante() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof UsuarioLogado u && u.ajudante();
    }

    private static void limpar(JsonNode no) {
        if (no instanceof ObjectNode obj) {
            Iterator<String> nomes = obj.fieldNames();
            while (nomes.hasNext()) {
                String campo = nomes.next();
                JsonNode valor = obj.get(campo);
                if (AcessoAjudante.CAMPOS_OCULTOS.contains(campo)
                        || (AcessoAjudante.CUSTO.equals(campo) && !valor.isObject())) {
                    nomes.remove();
                } else {
                    limpar(valor);
                }
            }
        } else if (no instanceof ArrayNode arr) {
            arr.forEach(OcultarValoresAjudante::limpar);
        }
    }
}
