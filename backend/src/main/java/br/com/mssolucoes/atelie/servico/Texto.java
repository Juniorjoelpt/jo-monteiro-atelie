package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.web.RegraNegocioException;

public final class Texto {

    private Texto() {
    }

    /** Remove espaços e devolve null para texto vazio. */
    public static String limpar(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Deixa só os dígitos e acrescenta o DDI 55 quando faltar.
     * "(98) 98879-3891" vira "5598988793891".
     */
    public static String whatsapp(String s) {
        if (s == null) {
            return null;
        }
        String d = s.replaceAll("\\D", "");
        if (d.isEmpty()) {
            return null;
        }
        if (d.length() == 10 || d.length() == 11) {
            d = "55" + d;
        }
        if (d.length() < 12 || d.length() > 13) {
            throw new RegraNegocioException("WhatsApp inválido. Informe com DDD, ex.: (98) 98879-3891.");
        }
        return d;
    }
}
