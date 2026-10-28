package br.com.joeljunior.atelie.servico;

import br.com.joeljunior.atelie.dominio.*;
import br.com.joeljunior.atelie.dominio.Enums.StatusEncomenda;
import br.com.joeljunior.atelie.dominio.Enums.StatusArte;
import br.com.joeljunior.atelie.dto.Dtos.LembreteResp;
import br.com.joeljunior.atelie.repositorio.*;
import br.com.joeljunior.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Monta os lembretes do dia com a mensagem já escrita. O envio é pelo WhatsApp dela (link wa.me):
 * depois de enviar, ela marca como enviado e o lembrete some.
 */
@Service
public class LembreteService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEE, dd/MM", PT_BR);
    private static final Pattern CHAVE_VALIDA = Pattern.compile("[A-Z]+(:[0-9-]+)+");
    /** Cobrança de saldo volta a aparecer depois de alguns dias, se continuar em aberto. */
    private static final int DIAS_ENTRE_COBRANCAS = 7;
    /** Arte enviada e sem resposta: lembra a cliente depois destes dias. */
    private static final int DIAS_LEMBRAR_ARTE = 2;

    private final LembreteRepository repo;
    private final EncomendaRepository encomendas;
    private final LancamentoRepository lancamentos;
    private final ClienteRepository clientes;
    private final ConfiguracaoService configuracao;
    private final UrlPublica url;
    private final ArteVersaoRepository artes;

    public LembreteService(LembreteRepository repo, EncomendaRepository encomendas,
                           LancamentoRepository lancamentos, ClienteRepository clientes,
                           ConfiguracaoService configuracao, UrlPublica url, ArteVersaoRepository artes) {
        this.url = url;
        this.artes = artes;
        this.repo = repo;
        this.encomendas = encomendas;
        this.lancamentos = lancamentos;
        this.clientes = clientes;
        this.configuracao = configuracao;
    }

    @Transactional(readOnly = true)
    public List<LembreteResp> pendentes() {
        LocalDate hoje = LocalDate.now();
        Configuracao cfg = configuracao.atual();
        List<LembreteResp> todos = new ArrayList<>();

        // Artes: mandar o link para a cliente aprovar e, se ela não responder em alguns dias, lembrar
        for (Encomenda e : encomendas.findByStatusInAndStatusArte(EncomendaService.ATIVAS, StatusArte.AGUARDANDO_CLIENTE)) {
            ArteVersao v = artes.findFirstByEncomendaIdOrderByNumeroDesc(e.getId()).orElse(null);
            if (v == null || e.getCodigoArte() == null) {
                continue;
            }
            Cliente c = e.getCliente();
            LocalDate enviada = v.getCriadoEm().toLocalDate();
            String link = url.arte(e.getCodigoArte());
            Lembrete jaEnviado = repo.findByChave("ARTE:" + v.getId()).orElse(null);
            if (jaEnviado == null) {
                todos.add(new LembreteResp("ARTE:" + v.getId(), "ARTE",
                        "Arte para aprovar: " + e.getDescricao(), "Versão " + v.getNumero(), c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! A arte da sua encomenda (" + e.getDescricao()
                                + ") está pronta" + (v.getNumero() > 1 ? ", já com os ajustes" : "")
                                + ". Dá uma olhadinha neste link e me diz se posso produzir ou se quer mudar algo: "
                                + link + " " + assinatura(cfg), enviada));
            } else if (!jaEnviado.getEnviadoEm().toLocalDate().isAfter(hoje.minusDays(DIAS_LEMBRAR_ARTE))) {
                todos.add(new LembreteResp("ARTELEMBRAR:" + v.getId(), "ARTE",
                        "Arte sem resposta: " + e.getDescricao(),
                        "Enviada em " + DIA.format(jaEnviado.getEnviadoEm().toLocalDate()), c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! Tudo bem? Conseguiu ver a arte da sua encomenda ("
                                + e.getDescricao() + ")? Só começo a produzir depois da sua aprovação: " + link
                                + " " + assinatura(cfg), hoje));
            }
        }

        for (Encomenda e : encomendas.findByStatusIn(EnumSet.of(StatusEncomenda.EM_PRODUCAO,
                StatusEncomenda.PRONTA, StatusEncomenda.ENTREGUE))) {
            Cliente c = e.getCliente();
            BigDecimal saldo = e.getValorTotal().subtract(lancamentos.totalPagoEncomenda(e.getId()));
            String textoSaldo = saldo.signum() > 0 ? " Fica faltando o saldo de " + moeda(saldo)
                    + (cfg.getChavePix() != null ? ", que pode ser pelo Pix " + cfg.getChavePix() : "") + "." : "";
            if (e.getStatus() == StatusEncomenda.PRONTA) {
                todos.add(new LembreteResp("PRONTA:" + e.getId(), "PRONTA", "Peça pronta: " + e.getDescricao(),
                        saldo.signum() > 0 ? "Saldo de " + moeda(saldo) : "Quitada", c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! Sua encomenda (" + e.getDescricao() + ") está pronta!"
                                + textoSaldo + " Quando fica bom para você buscar? " + assinatura(cfg), hoje));
            } else if (e.getStatus() == StatusEncomenda.EM_PRODUCAO && e.getPrazoEntrega() != null
                    && e.getPrazoEntrega().equals(hoje.plusDays(1))) {
                todos.add(new LembreteResp("ENTREGA:" + e.getId() + ":" + e.getPrazoEntrega(), "ENTREGA",
                        "Entrega amanhã: " + e.getDescricao(), "Ainda em produção", c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! Tudo certo para a entrega da sua encomenda ("
                                + e.getDescricao() + ") amanhã, " + DIA.format(e.getPrazoEntrega())
                                + "? Me confirma o melhor horário para você. " + assinatura(cfg), e.getPrazoEntrega()));
            } else if (e.getStatus() == StatusEncomenda.ENTREGUE && saldo.signum() <= 0 && e.getEntregueEm() != null
                    && !e.getEntregueEm().isAfter(hoje.minusDays(3)) && !e.getEntregueEm().isBefore(hoje.minusDays(10))) {
                // Alguns dias depois da entrega (e já quitada): pedir um depoimento para o site
                todos.add(new LembreteResp("DEPOIMENTO:" + e.getId(), "DEPOIMENTO", "Pedir depoimento: " + e.getDescricao(),
                        "Entregue em " + DIA.format(e.getEntregueEm()), c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! Tudo bem? Espero que esteja gostando da sua peça ("
                                + e.getDescricao() + "). Se puder, me conta em poucas palavras o que achou? Com a sua "
                                + "permissão, coloco no site do ateliê. Obrigada pelo carinho! " + assinatura(cfg), hoje));
            } else if (e.getStatus() == StatusEncomenda.ENTREGUE && saldo.signum() > 0) {
                todos.add(new LembreteResp("SALDO:" + e.getId(), "SALDO", "Saldo em aberto: " + e.getDescricao(),
                        moeda(saldo) + " a receber", c.getNome(), c.getWhatsapp(),
                        "Oi, " + primeiro(c.getNome()) + "! Tudo bem? Passando para lembrar do saldo de "
                                + moeda(saldo) + " da sua encomenda (" + e.getDescricao() + ")."
                                + (cfg.getChavePix() != null ? " Pode ser pelo Pix " + cfg.getChavePix() + "." : "")
                                + " Obrigada! " + assinatura(cfg), hoje));
            }
        }

        MonthDay hojeMd = MonthDay.from(hoje);
        for (Cliente c : clientes.findByAniversarioIsNotNull()) {
            MonthDay md = MonthDay.from(c.getAniversario());
            boolean bissextoCaiHoje = md.getMonthValue() == 2 && md.getDayOfMonth() == 29
                    && !hoje.isLeapYear() && hojeMd.equals(MonthDay.of(2, 28));
            if (md.equals(hojeMd) || bissextoCaiHoje) {
                todos.add(new LembreteResp("ANIV:" + c.getId() + ":" + hoje.getYear(), "ANIVERSARIO",
                        "Aniversário de " + c.getNome(), "Hoje", c.getNome(), c.getWhatsapp(),
                        "Feliz aniversário, " + primeiro(c.getNome()) + "! Que seu novo ano seja leve e cheio de "
                                + "coisas bonitas, feitas com carinho. Um abraço com carinho do ateliê! " + assinatura(cfg), hoje));
            }
        }

        Map<String, Lembrete> enviados = repo.findByChaveIn(todos.stream().map(LembreteResp::chave).toList())
                .stream().collect(Collectors.toMap(Lembrete::getChave, Function.identity()));
        return todos.stream().filter(l -> {
            Lembrete env = enviados.get(l.chave());
            if (env == null) {
                return true;
            }
            return l.tipo().equals("SALDO")
                    && env.getEnviadoEm().toLocalDate().plusDays(DIAS_ENTRE_COBRANCAS).isBefore(hoje.plusDays(1));
        }).sorted(Comparator.comparing(LembreteResp::data)).toList();
    }

    @Transactional(readOnly = true)
    public int quantidade() {
        return pendentes().size();
    }

    /** Enviado ou dispensado: não aparece de novo (a cobrança de saldo volta depois de 7 dias). */
    @Transactional
    public void marcar(String chave) {
        if (!CHAVE_VALIDA.matcher(chave).matches()) {
            throw new RegraNegocioException("Lembrete inválido.");
        }
        Lembrete l = repo.findByChave(chave).orElseGet(() -> {
            Lembrete novo = new Lembrete();
            novo.setChave(chave);
            return novo;
        });
        l.setEnviadoEm(LocalDateTime.now());
        repo.save(l);
    }

    private static String primeiro(String nome) {
        String n = nome == null ? "" : nome.trim();
        int espaco = n.indexOf(' ');
        return espaco > 0 ? n.substring(0, espaco) : n;
    }

    private static String assinatura(Configuracao c) {
        String responsavel = c.getResponsavel() != null ? primeiro(c.getResponsavel()) : "";
        // "Ateliê Jô Monteiro" já tem o nome dela: não repete
        if (responsavel.isEmpty() || c.getNomeAtelie().toLowerCase().contains(responsavel.toLowerCase())) {
            return "— " + c.getNomeAtelie();
        }
        return "— " + responsavel + ", " + c.getNomeAtelie();
    }

    static String moeda(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(v);
    }
}
