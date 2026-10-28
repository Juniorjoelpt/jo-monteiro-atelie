package br.com.mssolucoes.atelie.seguranca;

import br.com.mssolucoes.atelie.web.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite simples em memória para os formulários públicos e o login,
 * evitando spam e tentativas repetidas de senha.
 */
@Component
public class LimitadorDeRequisicoes {

    private final Map<String, Deque<Long>> acessos = new ConcurrentHashMap<>();

    public void verificar(HttpServletRequest req, String acao, int maximo, long janelaSegundos) {
        String chave = acao + ":" + ip(req);
        long agora = System.currentTimeMillis();
        long limite = agora - janelaSegundos * 1000;
        Deque<Long> fila = acessos.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst() < limite) {
                fila.pollFirst();
            }
            if (fila.size() >= maximo) {
                throw new RegraNegocioException("Muitas tentativas em pouco tempo. Aguarde alguns minutos e tente de novo.");
            }
            fila.addLast(agora);
        }
        if (acessos.size() > 10_000) {
            acessos.clear();
        }
    }

    private String ip(HttpServletRequest req) {
        String encaminhado = req.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
