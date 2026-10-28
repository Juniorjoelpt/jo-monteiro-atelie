package br.com.mssolucoes.atelie.servico;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Endereço do site usado nos links enviados às clientes (aprovação de arte, por exemplo).
 * Na VPS vem de URL_PUBLICA; no computador, do endereço que está sendo usado no navegador.
 */
@Component
public class UrlPublica {

    private final String configurada;

    public UrlPublica(@Value("${atelie.url-publica:}") String configurada) {
        this.configurada = configurada == null ? "" : configurada.trim().replaceAll("/+$", "");
    }

    public String base() {
        if (!configurada.isEmpty()) {
            return configurada;
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return ServletUriComponentsBuilder.fromContextPath(attrs.getRequest()).replacePath(null)
                    .replaceQuery(null).toUriString();
        }
        return "";
    }

    /** Página em que a cliente vê e aprova a arte da encomenda. */
    public String arte(String codigo) {
        return base() + "/arte/" + codigo;
    }
}
