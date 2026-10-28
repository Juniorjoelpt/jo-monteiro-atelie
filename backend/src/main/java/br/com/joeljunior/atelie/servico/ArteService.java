package br.com.joeljunior.atelie.servico;

import br.com.joeljunior.atelie.dominio.ArteVersao;
import br.com.joeljunior.atelie.dominio.Encomenda;
import br.com.joeljunior.atelie.dominio.Enums.RespostaArte;
import br.com.joeljunior.atelie.dominio.Enums.StatusArte;
import br.com.joeljunior.atelie.dominio.Enums.StatusEncomenda;
import br.com.joeljunior.atelie.dto.Dtos.*;
import br.com.joeljunior.atelie.repositorio.ArteVersaoRepository;
import br.com.joeljunior.atelie.repositorio.EncomendaRepository;
import br.com.joeljunior.atelie.web.NaoEncontradoException;
import br.com.joeljunior.atelie.web.RegraNegocioException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Aprovação de arte: a Jô envia o mockup, a cliente abre o link da encomenda (sem senha),
 * aprova ou pede ajuste. Cada ajuste vira uma nova versão, e o histórico fica guardado.
 */
@Service
public class ArteService {

    private static final int LIMITE_VERSOES = 15;

    private final ArteVersaoRepository repo;
    private final EncomendaRepository encomendas;
    private final ArmazenamentoService armazenamento;
    private final ConfiguracaoService configuracao;
    private final UrlPublica url;
    private final HistoricoService historico;
    private final br.com.joeljunior.atelie.repositorio.LancamentoRepository lancamentos;

    /** Aberturas com menos de 30 minutos entre elas contam como a mesma visita. */
    private static final long MINUTOS_VISITA = 30;

    public ArteService(ArteVersaoRepository repo, EncomendaRepository encomendas, ArmazenamentoService armazenamento,
                       ConfiguracaoService configuracao, UrlPublica url, HistoricoService historico,
                       br.com.joeljunior.atelie.repositorio.LancamentoRepository lancamentos) {
        this.historico = historico;
        this.lancamentos = lancamentos;
        this.repo = repo;
        this.encomendas = encomendas;
        this.armazenamento = armazenamento;
        this.configuracao = configuracao;
        this.url = url;
    }

    // ---------- Sistema ----------

    @Transactional(readOnly = true)
    public ArtesEncomenda daEncomenda(Long encomendaId) {
        Encomenda e = buscarEncomenda(encomendaId);
        List<ArteVersaoResp> versoes = repo.findByEncomendaIdOrderByNumeroDesc(encomendaId).stream()
                .map(ArteService::paraResp).toList();
        return new ArtesEncomenda(e.getStatusArte(), e.getCodigoArte() == null ? null : url.pedido(e.getCodigoArte()),
                versoes);
    }

    /** Nova versão da arte: vai para a cliente aprovar pelo link. */
    @Transactional
    public ArtesEncomenda enviar(Long encomendaId, String observacao, MultipartFile arquivo) {
        Encomenda e = buscarEncomenda(encomendaId);
        if (e.getStatus() == StatusEncomenda.CANCELADA || e.getStatus() == StatusEncomenda.ENTREGUE) {
            throw new RegraNegocioException("Esta encomenda já foi " + (e.getStatus() == StatusEncomenda.CANCELADA
                    ? "cancelada." : "entregue."));
        }
        long total = repo.countByEncomendaId(encomendaId);
        if (total >= LIMITE_VERSOES) {
            throw new RegraNegocioException("Limite de " + LIMITE_VERSOES + " versões de arte por encomenda.");
        }
        ArteVersao v = new ArteVersao();
        v.setEncomenda(e);
        v.setNumero(repo.findFirstByEncomendaIdOrderByNumeroDesc(encomendaId).map(ArteVersao::getNumero).orElse(0) + 1);
        v.setObservacao(cortar(Texto.limpar(observacao), 500));
        v.setArquivo(armazenamento.salvarImagemPrivada(arquivo));
        repo.save(v);
        if (e.getCodigoArte() == null) {
            e.setCodigoArte(UUID.randomUUID().toString());
        }
        e.setStatusArte(StatusArte.AGUARDANDO_CLIENTE);
        encomendas.save(e);
        historico.encomenda(e, "ARTE", "Enviou a versão " + v.getNumero() + " da arte para a cliente aprovar"
                + (v.getObservacao() == null ? "." : ", com o recado: “" + v.getObservacao() + "”."));
        return daEncomenda(encomendaId);
    }

    /**
     * Marca pelo sistema o que a cliente respondeu fora do link (no WhatsApp, por exemplo),
     * ou liga/desliga a etapa de arte enquanto nenhuma versão foi enviada.
     */
    @Transactional
    public ArtesEncomenda marcar(Long encomendaId, StatusArteReq r) {
        Encomenda e = buscarEncomenda(encomendaId);
        ArteVersao ultima = repo.findFirstByEncomendaIdOrderByNumeroDesc(encomendaId).orElse(null);
        String comentario = cortar(Texto.limpar(r.comentario()), 1000);
        switch (r.status()) {
            case APROVADA -> {
                if (ultima != null && ultima.getResposta() == null) {
                    responderVersao(ultima, RespostaArte.APROVADA, comentario, true);
                    historico.encomenda(e, "ARTE", "Marcou a versão " + ultima.getNumero()
                            + " da arte como aprovada (a cliente respondeu fora do link).");
                } else if (e.getStatusArte() != StatusArte.APROVADA) {
                    historico.encomenda(e, "ARTE", "Marcou a arte como aprovada fora do sistema.");
                }
                e.setStatusArte(StatusArte.APROVADA);
            }
            case AJUSTE_PEDIDO -> {
                if (ultima == null) {
                    throw new RegraNegocioException("Envie uma versão da arte antes de registrar um ajuste.");
                }
                if (ultima.getResposta() != null) {
                    throw new RegraNegocioException("A última versão já foi respondida. Envie uma nova versão.");
                }
                if (comentario == null) {
                    throw new RegraNegocioException("Escreva o que a cliente pediu para mudar.");
                }
                responderVersao(ultima, RespostaArte.AJUSTE, comentario, true);
                e.setStatusArte(StatusArte.AJUSTE_PEDIDO);
                historico.encomenda(e, "ARTE", "Registrou ajuste pedido pela cliente na versão " + ultima.getNumero()
                        + ": “" + comentario + "”.");
            }
            case A_CRIAR, SEM_ARTE -> {
                if (ultima != null) {
                    throw new RegraNegocioException("Esta encomenda já tem arte enviada. Exclua as versões para mudar.");
                }
                if (e.getStatusArte() != r.status()) {
                    historico.encomenda(e, "ARTE", r.status() == StatusArte.SEM_ARTE
                            ? "Marcou que a encomenda não precisa de arte." : "Marcou que a encomenda precisa de arte.");
                }
                e.setStatusArte(r.status());
            }
            case AGUARDANDO_CLIENTE ->
                    throw new RegraNegocioException("Para mandar para a cliente, envie uma versão da arte.");
        }
        encomendas.save(e);
        return daEncomenda(encomendaId);
    }

    /** Só a última versão, e só enquanto a cliente não respondeu. */
    @Transactional
    public ArtesEncomenda excluir(Long versaoId) {
        ArteVersao v = buscar(versaoId);
        Long encomendaId = v.getEncomenda().getId();
        ArteVersao ultima = repo.findFirstByEncomendaIdOrderByNumeroDesc(encomendaId).orElseThrow();
        if (!ultima.getId().equals(v.getId())) {
            throw new RegraNegocioException("Só a versão mais recente pode ser excluída.");
        }
        if (v.getResposta() != null) {
            throw new RegraNegocioException("Esta versão já foi respondida e fica no histórico.");
        }
        repo.delete(v);
        repo.flush();
        armazenamento.excluirPrivada(v.getArquivo());
        Encomenda e = v.getEncomenda();
        historico.encomenda(e, "ARTE", "Excluiu a versão " + v.getNumero() + " da arte (antes da resposta da cliente).");
        e.setStatusArte(repo.findFirstByEncomendaIdOrderByNumeroDesc(encomendaId)
                .map(ArteService::situacao).orElse(StatusArte.A_CRIAR));
        encomendas.save(e);
        return daEncomenda(encomendaId);
    }

    @Transactional(readOnly = true)
    public Resource arquivo(Long versaoId) {
        return armazenamento.abrirPrivada(buscar(versaoId).getArquivo());
    }

    @Transactional(readOnly = true)
    public String tipoMime(Long versaoId) {
        return ArmazenamentoService.tipoMime(buscar(versaoId).getArquivo());
    }

    /** Contadores para o menu: artes a fazer (inclui ajustes), ajustes pedidos e artes com a cliente. */
    @Transactional(readOnly = true)
    public ResumoArtes resumo() {
        return new ResumoArtes(
                encomendas.countByStatusInAndStatusArteIn(EncomendaService.ATIVAS,
                        java.util.EnumSet.of(StatusArte.A_CRIAR, StatusArte.AJUSTE_PEDIDO)),
                encomendas.countByStatusInAndStatusArteIn(EncomendaService.ATIVAS, java.util.EnumSet.of(StatusArte.AJUSTE_PEDIDO)),
                encomendas.countByStatusInAndStatusArteIn(EncomendaService.ATIVAS,
                        java.util.EnumSet.of(StatusArte.AGUARDANDO_CLIENTE)));
    }

    // ---------- Página da cliente (link sem senha) ----------

    @Transactional(readOnly = true)
    public ArtePublica publica(String codigo) {
        Encomenda e = porCodigo(codigo);
        var cfg = configuracao.atual();
        List<ArteVersao> versoes = repo.findByEncomendaIdOrderByNumeroDesc(e.getId());
        Long atual = versoes.isEmpty() ? null : versoes.get(0).getId();
        java.math.BigDecimal pago = lancamentos.totalPagoEncomenda(e.getId());
        java.math.BigDecimal saldo = e.getValorTotal().subtract(pago).max(java.math.BigDecimal.ZERO);
        boolean comSaldo = saldo.signum() > 0;
        var andamento = new AndamentoPublico(e.getStatus(), e.getQuantidade(), e.getPrazoEntrega(), e.getEntregueEm(),
                e.getValorTotal(), pago, saldo, e.getValorSinal(), comSaldo ? cfg.getChavePix() : null,
                comSaldo ? cfg.getTitularPix() : null, e.getCriadoEm());
        // Escola e empresa: "Oi, Escola Pequeno Príncipe!" (o primeiro nome seria só "Escola")
        var c = e.getCliente();
        String saudacao = c.getTipo() == br.com.joeljunior.atelie.dominio.Enums.TipoCliente.ESCOLA
                || c.getTipo() == br.com.joeljunior.atelie.dominio.Enums.TipoCliente.EMPRESA ? c.getNome() : primeiroNome(c.getNome());
        return new ArtePublica(cfg.getNomeAtelie(), cfg.getWhatsapp(), saudacao,
                e.getDescricao(), e.getPersonalizacao(), e.getStatusArte(),
                versoes.stream().map(v -> new VersaoPublica(v.getId(), v.getNumero(),
                        "/api/publico/artes/" + codigo + "/versoes/" + v.getId() + "/arquivo", v.getObservacao(),
                        v.getResposta(), v.getComentarioCliente(), v.getRespondidoEm(), v.getCriadoEm(),
                        v.getId().equals(atual))).toList(), andamento);
    }

    /**
     * A página do pedido avisa que abriu (e qual versão da arte apareceu na tela).
     * Quem está logada no sistema não conta: assim a Jô pode conferir o link sem marcar como visto.
     */
    @Transactional
    public void visto(String codigo, Long versaoId) {
        if (HistoricoService.temLogin()) {
            return;
        }
        Encomenda e = porCodigo(codigo);
        LocalDateTime agora = LocalDateTime.now();
        if (e.getAcompanhadoEm() == null) {
            historico.daCliente(e, "ABRIU", "Abriu o link do pedido pela primeira vez.");
        }
        e.setAcompanhadoEm(agora);
        encomendas.save(e);
        if (versaoId == null) {
            return;
        }
        ArteVersao ultima = repo.findFirstByEncomendaIdOrderByNumeroDesc(e.getId()).orElse(null);
        if (ultima == null || !ultima.getId().equals(versaoId)) {
            return;
        }
        if (ultima.getVistaEm() == null) {
            ultima.setVistaEm(agora);
            historico.daCliente(e, "VIU_ARTE", "Viu a versão " + ultima.getNumero() + " da arte pela primeira vez.");
        }
        if (ultima.getVistaUltimaEm() == null || ultima.getVistaUltimaEm().plusMinutes(MINUTOS_VISITA).isBefore(agora)) {
            ultima.setVisualizacoes(ultima.getVisualizacoes() + 1);
        }
        ultima.setVistaUltimaEm(agora);
        repo.save(ultima);
    }

    @Transactional(readOnly = true)
    public ArteVersao versaoPublica(String codigo, Long versaoId) {
        Encomenda e = porCodigo(codigo);
        ArteVersao v = buscar(versaoId);
        if (!v.getEncomenda().getId().equals(e.getId())) {
            throw new NaoEncontradoException("Arte");
        }
        return v;
    }

    public Resource abrir(ArteVersao v) {
        return armazenamento.abrirPrivada(v.getArquivo());
    }

    @Transactional
    public ArtePublica responder(String codigo, RespostaArteReq r) {
        Encomenda e = porCodigo(codigo);
        if (Texto.limpar(r.site()) != null) {
            return publica(codigo); // robô: ignora em silêncio
        }
        ArteVersao ultima = repo.findFirstByEncomendaIdOrderByNumeroDesc(e.getId())
                .orElseThrow(() -> new RegraNegocioException("Ainda não há arte para aprovar."));
        if (!ultima.getId().equals(r.versaoId())) {
            throw new RegraNegocioException("Já existe uma versão mais nova desta arte. Atualize a página.");
        }
        if (ultima.getResposta() != null || e.getStatusArte() != StatusArte.AGUARDANDO_CLIENTE) {
            throw new RegraNegocioException("Esta versão já foi respondida. Obrigada!");
        }
        String comentario = cortar(Texto.limpar(r.comentario()), 1000);
        if (Boolean.TRUE.equals(r.aprovar())) {
            responderVersao(ultima, RespostaArte.APROVADA, comentario, false);
            e.setStatusArte(StatusArte.APROVADA);
            historico.daCliente(e, "ARTE", "Aprovou a versão " + ultima.getNumero() + " da arte"
                    + (comentario == null ? "." : ", com o recado: “" + comentario + "”."));
        } else {
            if (comentario == null || comentario.length() < 3) {
                throw new RegraNegocioException("Conte o que você gostaria de mudar na arte.");
            }
            responderVersao(ultima, RespostaArte.AJUSTE, comentario, false);
            e.setStatusArte(StatusArte.AJUSTE_PEDIDO);
            historico.daCliente(e, "ARTE", "Pediu ajuste na versão " + ultima.getNumero() + ": “" + comentario + "”.");
        }
        encomendas.save(e);
        return publica(codigo);
    }

    // ---------- Apoio ----------

    private void responderVersao(ArteVersao v, RespostaArte resposta, String comentario, boolean peloSistema) {
        v.setResposta(resposta);
        v.setComentarioCliente(comentario);
        v.setRespondidaNoSistema(peloSistema);
        v.setRespondidoEm(LocalDateTime.now());
        repo.save(v);
    }

    private static StatusArte situacao(ArteVersao v) {
        if (v.getResposta() == null) {
            return StatusArte.AGUARDANDO_CLIENTE;
        }
        return v.getResposta() == RespostaArte.APROVADA ? StatusArte.APROVADA : StatusArte.AJUSTE_PEDIDO;
    }

    private Encomenda porCodigo(String codigo) {
        if (codigo == null || !codigo.matches("[0-9a-fA-F-]{36}")) {
            throw new NaoEncontradoException("Arte");
        }
        Encomenda e = encomendas.findByCodigoArte(codigo).orElseThrow(() -> new NaoEncontradoException("Arte"));
        if (e.getStatus() == StatusEncomenda.CANCELADA) {
            throw new NaoEncontradoException("Arte");
        }
        return e;
    }

    private ArteVersao buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Arte"));
    }

    private Encomenda buscarEncomenda(Long id) {
        return encomendas.findById(id).orElseThrow(() -> new NaoEncontradoException("Encomenda"));
    }

    private static ArteVersaoResp paraResp(ArteVersao v) {
        return new ArteVersaoResp(v.getId(), v.getNumero(), "/api/artes/" + v.getId() + "/arquivo", v.getObservacao(),
                v.getResposta(), v.getComentarioCliente(), v.isRespondidaNoSistema(), v.getRespondidoEm(),
                v.getCriadoEm(), v.getVistaEm(), v.getVistaUltimaEm(), v.getVisualizacoes());
    }

    private static String primeiroNome(String nome) {
        String n = nome == null ? "" : nome.trim();
        int espaco = n.indexOf(' ');
        return espaco > 0 ? n.substring(0, espaco) : n;
    }

    private static String cortar(String s, int max) {
        return s == null ? null : s.substring(0, Math.min(max, s.length()));
    }
}
