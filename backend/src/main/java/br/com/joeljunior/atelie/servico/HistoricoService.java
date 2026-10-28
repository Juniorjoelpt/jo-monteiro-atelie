package br.com.joeljunior.atelie.servico;

import br.com.joeljunior.atelie.dominio.Encomenda;
import br.com.joeljunior.atelie.dominio.Enums.AreaHistorico;
import br.com.joeljunior.atelie.dominio.Historico;
import br.com.joeljunior.atelie.dto.Dtos.HistoricoResp;
import br.com.joeljunior.atelie.repositorio.HistoricoRepository;
import br.com.joeljunior.atelie.seguranca.UsuarioLogado;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Histórico de quem fez o quê: cada ação importante (status, valores, pagamentos, artes, estoque, vendas)
 * vira uma linha com o nome de quem fez. Ações da cliente pelo link ficam como "Cliente (pelo link)".
 */
@Service
public class HistoricoService {

    public static final String CLIENTE = "Cliente (pelo link)";
    private static final int LIMITE_LISTA = 500;

    private final HistoricoRepository repo;

    public HistoricoService(HistoricoRepository repo) {
        this.repo = repo;
    }

    /** Ação de quem está logada (ou "Sistema", se não houver login). */
    @Transactional
    public void registrar(AreaHistorico area, Encomenda e, String acao, String descricao, boolean sensivel) {
        UsuarioLogado u = usuarioAtual();
        salvar(area, e, u == null ? null : u.id(), u == null ? "Sistema" : u.nome(), acao, descricao, sensivel);
    }

    public void encomenda(Encomenda e, String acao, String descricao) {
        registrar(AreaHistorico.ENCOMENDA, e, acao, descricao, false);
    }

    /** Fala de dinheiro: a ajudante não vê esta linha. */
    public void encomendaValor(Encomenda e, String acao, String descricao) {
        registrar(AreaHistorico.ENCOMENDA, e, acao, descricao, true);
    }

    /** O que a cliente fez pelo link do pedido. */
    @Transactional
    public void daCliente(Encomenda e, String acao, String descricao) {
        String nome = e.getCliente() == null ? null : e.getCliente().getNome();
        salvar(AreaHistorico.ENCOMENDA, e, null, nome == null ? CLIENTE : nome + " (pelo link)", acao, descricao, false);
    }

    private void salvar(AreaHistorico area, Encomenda e, Long usuarioId, String autor, String acao, String descricao,
                        boolean sensivel) {
        Historico h = new Historico();
        h.setArea(area);
        h.setEncomenda(e);
        h.setUsuarioId(usuarioId);
        h.setAutor(cortar(autor, 140));
        h.setAcao(cortar(acao, 40));
        h.setDescricao(cortar(descricao, 600));
        h.setSensivel(sensivel);
        repo.save(h);
    }

    @Transactional(readOnly = true)
    public List<HistoricoResp> daEncomenda(Long encomendaId) {
        boolean ajudante = ajudante();
        return repo.findByEncomendaIdOrderByCriadoEmDescIdDesc(encomendaId).stream()
                .filter(h -> !ajudante || !h.isSensivel())
                .map(HistoricoService::paraResp).toList();
    }

    /** Tela geral (só a administradora): período, área e quem fez. usuarioId = -1 traz só o que a cliente fez. */
    @Transactional(readOnly = true)
    public List<HistoricoResp> buscar(LocalDate de, LocalDate ate, AreaHistorico area, Long usuarioId) {
        LocalDate fim = ate == null ? LocalDate.now() : ate;
        LocalDate inicio = de == null ? fim.minusDays(30) : de;
        boolean soCliente = usuarioId != null && usuarioId == -1;
        return repo.buscar(inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay(), area,
                        soCliente ? null : usuarioId, soCliente, PageRequest.of(0, LIMITE_LISTA)).stream()
                .map(HistoricoService::paraResp).toList();
    }

    // ---------- Apoio para montar as frases ----------

    /** "campo: antes → depois", ou null se não mudou. */
    public static String mudanca(String campo, Object antes, Object depois) {
        if (iguais(antes, depois)) {
            return null;
        }
        return campo + ": " + texto(antes) + " → " + texto(depois);
    }

    private static boolean iguais(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        return Objects.equals(a, b);
    }

    private static String texto(Object v) {
        if (v == null || (v instanceof String s && s.isBlank())) {
            return "(vazio)";
        }
        if (v instanceof BigDecimal b) {
            return pdfMoeda(b);
        }
        if (v instanceof LocalDate d) {
            return "%02d/%02d/%d".formatted(d.getDayOfMonth(), d.getMonthValue(), d.getYear());
        }
        String s = v.toString().replaceAll("\\s+", " ").trim();
        return s.length() > 60 ? s.substring(0, 57) + "…" : s;
    }

    public static String pdfMoeda(BigDecimal v) {
        return br.com.joeljunior.atelie.servico.pdf.PdfAtelie.moeda(v);
    }

    private static HistoricoResp paraResp(Historico h) {
        Encomenda e = h.getEncomenda();
        return new HistoricoResp(h.getId(), h.getArea(), e == null ? null : e.getId(), e == null ? null : e.getDescricao(),
                h.getAutor(), h.getUsuarioId() == null && h.getAutor().endsWith("(pelo link)"), h.getAcao(),
                h.getDescricao(), h.getCriadoEm());
    }

    private static UsuarioLogado usuarioAtual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof UsuarioLogado u ? u : null;
    }

    private static boolean ajudante() {
        UsuarioLogado u = usuarioAtual();
        return u != null && u.ajudante();
    }

    /** Quem está logada (para não contar as visitas da própria Jô ao link). */
    public static boolean temLogin() {
        return usuarioAtual() != null;
    }

    private static String cortar(String s, int max) {
        String t = s == null ? "" : s.trim();
        return t.length() > max ? t.substring(0, max - 1) + "…" : t;
    }
}
