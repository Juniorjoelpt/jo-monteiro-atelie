package br.com.joeljunior.atelie.servico;

import br.com.joeljunior.atelie.dominio.Cliente;
import br.com.joeljunior.atelie.dominio.Compromisso;
import br.com.joeljunior.atelie.dominio.Encomenda;
import br.com.joeljunior.atelie.dominio.Enums.*;
import br.com.joeljunior.atelie.dto.Dtos.CompromissoReq;
import br.com.joeljunior.atelie.dto.Dtos.CompromissoResp;
import br.com.joeljunior.atelie.dto.Dtos.EventoAgenda;
import br.com.joeljunior.atelie.repositorio.*;
import br.com.joeljunior.atelie.web.NaoEncontradoException;
import br.com.joeljunior.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Junta numa linha do tempo: entregas, compromissos, vencimentos e aniversários de clientes. */
@Service
public class AgendaService {

    private final CompromissoRepository compromissos;
    private final EncomendaRepository encomendas;
    private final ClienteRepository clientes;
    private final ContaFixaRepository contas;
    private final LancamentoRepository lancamentos;

    public AgendaService(CompromissoRepository compromissos, EncomendaRepository encomendas,
                         ClienteRepository clientes, ContaFixaRepository contas,
                         LancamentoRepository lancamentos) {
        this.contas = contas;
        this.lancamentos = lancamentos;
        this.compromissos = compromissos;
        this.encomendas = encomendas;
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public List<EventoAgenda> eventos(LocalDate de, LocalDate ate) {
        return eventos(de, ate, true);
    }

    /** comContas = false para a ajudante (vencimentos de contas são só da administradora). */
    @Transactional(readOnly = true)
    public List<EventoAgenda> eventos(LocalDate de, LocalDate ate, boolean comContas) {
        if (ate.isBefore(de)) {
            throw new RegraNegocioException("O período está invertido.");
        }
        if (ChronoUnit.DAYS.between(de, ate) > 100) {
            throw new RegraNegocioException("Escolha um período de até 100 dias.");
        }
        List<EventoAgenda> lista = new ArrayList<>();

        for (Encomenda e : encomendas.findByPrazoEntregaBetweenOrderByPrazoEntregaAsc(de, ate)) {
            if (e.getStatus() == StatusEncomenda.CANCELADA) {
                continue;
            }
            lista.add(new EventoAgenda("ENTREGA", e.getId(), e.getPrazoEntrega(), null,
                    "Entrega: " + e.getDescricao(), e.getCliente().getNome(), e.getStatus().name(),
                    e.getStatus() == StatusEncomenda.ENTREGUE));
        }

        for (Compromisso c : compromissos.findByInicioBetweenOrderByInicioAsc(de.atStartOfDay(), ate.plusDays(1).atStartOfDay())) {
            String detalhe = String.join(" · ", java.util.stream.Stream.of(
                            c.getCliente() != null ? c.getCliente().getNome() : null, c.getLocal())
                    .filter(java.util.Objects::nonNull).toList());
            lista.add(new EventoAgenda("COMPROMISSO", c.getId(), c.getInicio().toLocalDate(), c.getInicio().toLocalTime(),
                    c.getTitulo(), detalhe, c.getTipo().name(), c.isConcluido()));
        }

        for (Cliente c : clientes.findByAniversarioIsNotNull()) {
            MonthDay md = MonthDay.from(c.getAniversario());
            for (int ano = de.getYear(); ano <= ate.getYear(); ano++) {
                if (md.getMonthValue() == 2 && md.getDayOfMonth() == 29 && !java.time.Year.isLeap(ano)) {
                    continue;
                }
                LocalDate dia = md.atYear(ano);
                if (!dia.isBefore(de) && !dia.isAfter(ate)) {
                    lista.add(new EventoAgenda("ANIVERSARIO", c.getId(), dia, null,
                            "Aniversário: " + c.getNome(), null, null, false));
                }
            }
        }

        if (comContas) {
            List<java.time.YearMonth> meses = new ArrayList<>();
            for (java.time.YearMonth m = java.time.YearMonth.from(de); !m.isAfter(java.time.YearMonth.from(ate)); m = m.plusMonths(1)) {
                meses.add(m);
            }
            java.util.Set<String> pagas = new java.util.HashSet<>();
            lancamentos.findByContaFixaIsNotNullAndCompetenciaIn(meses.stream().map(m -> m.atDay(1)).toList())
                    .forEach(l -> pagas.add(l.getContaFixa().getId() + ":" + l.getCompetencia()));
            for (br.com.joeljunior.atelie.dominio.ContaFixa c : contas.findByAtivaTrueOrderByDiaVencimentoAscNomeAsc()) {
                for (java.time.YearMonth m : meses) {
                    LocalDate venc = ContaFixaService.vencimento(c, m);
                    if (venc.isBefore(de) || venc.isAfter(ate)) {
                        continue;
                    }
                    boolean paga = pagas.contains(c.getId() + ":" + m.atDay(1));
                    lista.add(new EventoAgenda("CONTA", c.getId(), venc, null, "Vence: " + c.getNome(),
                            LembreteService.moeda(c.getValor()), paga ? "PAGA" : "ABERTA", paga));
                }
            }
        }

        lista.sort(Comparator.comparing(EventoAgenda::data)
                .thenComparing(ev -> ev.hora() == null ? LocalTime.MIN : ev.hora()));
        return lista;
    }

    // ---------- Compromissos ----------

    @Transactional(readOnly = true)
    public CompromissoResp detalhar(Long id) {
        return paraResp(buscar(id));
    }

    @Transactional
    public CompromissoResp criar(CompromissoReq r) {
        Compromisso c = new Compromisso();
        preencher(c, r);
        return paraResp(compromissos.save(c));
    }

    @Transactional
    public CompromissoResp atualizar(Long id, CompromissoReq r) {
        Compromisso c = buscar(id);
        preencher(c, r);
        return paraResp(compromissos.save(c));
    }

    @Transactional
    public void excluir(Long id) {
        compromissos.delete(buscar(id));
    }

    private Compromisso buscar(Long id) {
        return compromissos.findById(id).orElseThrow(() -> new NaoEncontradoException("Compromisso"));
    }

    private void preencher(Compromisso c, CompromissoReq r) {
        if (r.fim() != null && r.fim().isBefore(r.inicio())) {
            throw new RegraNegocioException("O horário de término é antes do início.");
        }
        c.setTitulo(r.titulo().trim());
        c.setTipo(r.tipo() == null ? TipoCompromisso.OUTRO : r.tipo());
        c.setInicio(r.inicio());
        c.setFim(r.fim());
        c.setLocal(Texto.limpar(r.local()));
        c.setCliente(r.clienteId() == null ? null
                : clientes.findById(r.clienteId()).orElseThrow(() -> new NaoEncontradoException("Cliente")));
        Encomenda e = r.encomendaId() == null ? null
                : encomendas.findById(r.encomendaId()).orElseThrow(() -> new NaoEncontradoException("Encomenda"));
        c.setEncomenda(e);
        if (e != null && c.getCliente() == null) {
            c.setCliente(e.getCliente());
        }
        c.setObservacoes(Texto.limpar(r.observacoes()));
        c.setConcluido(Boolean.TRUE.equals(r.concluido()));
    }

    private static CompromissoResp paraResp(Compromisso c) {
        return new CompromissoResp(c.getId(), c.getTitulo(), c.getTipo(), c.getInicio(), c.getFim(), c.getLocal(),
                c.getCliente() == null ? null : c.getCliente().getId(),
                c.getCliente() == null ? null : c.getCliente().getNome(),
                c.getEncomenda() == null ? null : c.getEncomenda().getId(),
                c.getEncomenda() == null ? null : c.getEncomenda().getDescricao(),
                c.getObservacoes(), c.isConcluido());
    }
}
