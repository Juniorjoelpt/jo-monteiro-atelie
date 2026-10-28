package br.com.joeljunior.atelie.servico;

import br.com.joeljunior.atelie.dominio.*;
import br.com.joeljunior.atelie.dominio.Enums.*;
import br.com.joeljunior.atelie.dto.Dtos.*;
import br.com.joeljunior.atelie.repositorio.*;
import br.com.joeljunior.atelie.web.NaoEncontradoException;
import br.com.joeljunior.atelie.web.RegraNegocioException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class OrcamentoService {

    /** Fotos de referência que a cliente pode mandar junto com o pedido. */
    public static final int MAX_FOTOS_SITE = 3;

    private final OrcamentoRepository repo;
    private final OrcamentoFotoRepository fotos;
    private final EncomendaRepository encomendas;
    private final EncomendaFotoRepository fotosEncomenda;
    private final EncomendaService encomendaService;
    private final ClienteService clientes;
    private final ProdutoRepository produtos;
    private final ArmazenamentoService armazenamento;
    private final OrcamentoItemRepository itens;
    private final PrecificacaoService precificacao;
    private final HistoricoService historico;

    public OrcamentoService(OrcamentoRepository repo, OrcamentoFotoRepository fotos, EncomendaRepository encomendas,
                            EncomendaFotoRepository fotosEncomenda, EncomendaService encomendaService,
                            ClienteService clientes, ProdutoRepository produtos, ArmazenamentoService armazenamento,
                            OrcamentoItemRepository itens, PrecificacaoService precificacao, HistoricoService historico) {
        this.historico = historico;
        this.itens = itens;
        this.precificacao = precificacao;
        this.repo = repo;
        this.fotos = fotos;
        this.encomendas = encomendas;
        this.fotosEncomenda = fotosEncomenda;
        this.encomendaService = encomendaService;
        this.clientes = clientes;
        this.produtos = produtos;
        this.armazenamento = armazenamento;
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResp> listar(StatusOrcamento status) {
        List<Orcamento> lista = status == null
                ? repo.findAllByOrderByCriadoEmDesc()
                : repo.findByStatusInOrderByCriadoEmDesc(EnumSet.of(status));
        return lista.stream().map(this::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResp> doCliente(Long clienteId) {
        return repo.findByClienteIdOrderByCriadoEmDesc(clienteId).stream().map(this::paraResp).toList();
    }

    /** Pedido feito pelo formulário do site, com até 3 fotos de referência. */
    @Transactional
    public void receberDoSite(OrcamentoPublicoReq r, List<MultipartFile> anexos) {
        if (Texto.limpar(r.site()) != null) {
            return; // robô preencheu o campo escondido: ignora em silêncio
        }
        if (r.dataDesejada() != null && r.dataDesejada().isBefore(LocalDate.now())) {
            throw new RegraNegocioException("A data desejada já passou.");
        }
        List<MultipartFile> validos = anexos == null ? List.of()
                : anexos.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (validos.size() > MAX_FOTOS_SITE) {
            throw new RegraNegocioException("Envie no máximo " + MAX_FOTOS_SITE + " fotos de referência.");
        }
        // Sacola: só peças que estão no catálogo do site; a mesma peça repetida soma as quantidades
        java.util.LinkedHashMap<Long, Integer> sacola = new java.util.LinkedHashMap<>();
        if (r.itens() != null) {
            r.itens().forEach(i -> sacola.merge(i.produtoId(), i.quantidade(), Integer::sum));
        }
        java.util.Map<Long, Produto> daSacola = new java.util.HashMap<>();
        produtos.findAllById(sacola.keySet()).stream().filter(p -> p.isAtivo() && p.isNoCatalogo())
                .forEach(p -> daSacola.put(p.getId(), p));
        sacola.keySet().retainAll(daSacola.keySet());

        Orcamento o = new Orcamento();
        o.setNome(r.nome().trim());
        o.setWhatsapp(Texto.whatsapp(r.whatsapp()));
        o.setTipoPeca(r.tipoPeca().trim());
        o.setQuantidade(r.quantidade() == null ? 1 : r.quantidade());
        // Só aceita peça que está no catálogo do site
        if (r.produtoId() != null) {
            produtos.findById(r.produtoId()).filter(p -> p.isAtivo() && p.isNoCatalogo()).ifPresent(o::setProduto);
        }
        if (sacola.size() == 1) {
            // Uma peça só: fica como a peça do pedido, igual ao botão "Pedir orçamento desta peça"
            var unico = sacola.entrySet().iterator().next();
            o.setProduto(daSacola.get(unico.getKey()));
            o.setQuantidade(unico.getValue());
        } else if (sacola.size() > 1) {
            o.setTipoPeca(("Sacola: " + sacola.size() + " peças do catálogo"));
            o.setQuantidade(sacola.values().stream().mapToInt(Integer::intValue).sum());
        }
        o.setPersonalizacao(Texto.limpar(r.personalizacao()));
        o.setMedidas(Texto.limpar(r.medidas()));
        o.setCores(Texto.limpar(r.cores()));
        o.setDataDesejada(r.dataDesejada());
        o.setDetalhes(Texto.limpar(r.detalhes()));
        o.setOrigem(Origem.SITE);
        o.setStatus(StatusOrcamento.NOVO);
        repo.save(o);
        if (sacola.size() > 1) {
            sacola.forEach((id, qtd) -> {
                OrcamentoItem item = new OrcamentoItem();
                item.setOrcamento(o);
                item.setProduto(daSacola.get(id));
                item.setQuantidade(qtd);
                itens.save(item);
            });
        }

        // Salva as fotos; se alguma falhar, apaga as já gravadas (a transação desfaz o pedido)
        List<String> salvos = new ArrayList<>();
        try {
            for (MultipartFile f : validos) {
                String nome = armazenamento.salvarImagemPrivada(f);
                salvos.add(nome);
                OrcamentoFoto foto = new OrcamentoFoto();
                foto.setOrcamento(o);
                foto.setArquivo(nome);
                fotos.save(foto);
            }
        } catch (RuntimeException e) {
            salvos.forEach(armazenamento::excluirPrivada);
            throw e;
        }
    }

    @Transactional
    public OrcamentoResp criar(OrcamentoReq r) {
        Orcamento o = new Orcamento();
        preencher(o, r);
        o.setOrigem(Origem.MANUAL);
        return paraResp(repo.save(o));
    }

    @Transactional
    public OrcamentoResp atualizar(Long id, OrcamentoReq r) {
        Orcamento o = buscar(id);
        preencher(o, r);
        return paraResp(repo.save(o));
    }

    @Transactional
    public OrcamentoResp mudarStatus(Long id, StatusOrcamento status) {
        Orcamento o = buscar(id);
        if (status == StatusOrcamento.APROVADO) {
            throw new RegraNegocioException("Use \"Aprovar e criar encomenda\" para aprovar.");
        }
        if (o.getStatus() == StatusOrcamento.APROVADO) {
            throw new RegraNegocioException("Este orçamento já virou encomenda.");
        }
        o.setStatus(status);
        return paraResp(repo.save(o));
    }

    /**
     * Aprova o orçamento: cadastra (ou reaproveita) a cliente e cria a encomenda.
     * A personalização e as fotos de referência vão junto; a arte entra como "a criar".
     */
    @Transactional
    public EncomendaResp aprovar(Long id, AprovarOrcamentoReq r) {
        Orcamento o = buscar(id);
        if (o.getStatus() == StatusOrcamento.APROVADO || encomendas.existsByOrcamentoId(id)) {
            throw new RegraNegocioException("Este orçamento já virou encomenda.");
        }
        BigDecimal sinal = r.valorSinal() == null ? BigDecimal.ZERO : r.valorSinal();
        if (sinal.compareTo(r.valorTotal()) > 0) {
            throw new RegraNegocioException("O sinal não pode ser maior que o valor total.");
        }
        Cliente c = o.getCliente() != null ? o.getCliente()
                : clientes.encontrarOuCriar(o.getNome(), o.getWhatsapp(), TipoCliente.CLIENTE);
        o.setCliente(c);
        o.setStatus(StatusOrcamento.APROVADO);
        if (o.getValorProposto() == null) {
            o.setValorProposto(r.valorTotal());
        }
        repo.save(o);

        Encomenda e = new Encomenda();
        e.setCliente(c);
        e.setOrcamento(o);
        e.setDescricao(r.descricao().trim());
        e.setFrente(r.frente());
        Long produtoId = r.produtoId() != null ? r.produtoId() : o.getProduto() != null ? o.getProduto().getId() : null;
        if (produtoId != null) {
            e.setProduto(produtos.findById(produtoId).orElseThrow(() -> new NaoEncontradoException("Peça")));
        }
        e.setQuantidade(r.quantidade() != null ? r.quantidade() : o.getQuantidade());
        e.setValorTotal(r.valorTotal());
        e.setValorSinal(sinal);
        e.setPrazoEntrega(r.prazoEntrega() != null ? r.prazoEntrega() : o.getDataDesejada());
        e.setStatus(sinal.signum() == 0 ? StatusEncomenda.EM_PRODUCAO : StatusEncomenda.AGUARDANDO_SINAL);
        List<OrcamentoItem> pecas = itens.findByOrcamentoIdOrderByIdAsc(id);
        String listaPecas = pecas.isEmpty() ? null : "Peças do pedido: " + String.join(", ",
                pecas.stream().map(i -> i.getQuantidade() + "× " + i.getProduto().getNome()).toList()) + ".";
        e.setObservacoes(listaPecas == null ? o.getDetalhes()
                : o.getDetalhes() == null ? listaPecas : listaPecas + "\n" + o.getDetalhes());
        String pers = Texto.limpar(r.personalizacao());
        e.setPersonalizacao(pers != null ? pers : o.getPersonalizacao());
        EncomendaService.definirSeTemArte(e, r.precisaArte() == null || r.precisaArte());
        EncomendaResp resp = encomendaService.salvarEProcessar(e);
        historico.encomenda(e, "CRIOU", "Aprovou o orçamento nº " + o.getId() + " e criou a encomenda para " + c.getNome() + ".");
        historico.encomendaValor(e, "VALORES", "Valor total " + HistoricoService.pdfMoeda(e.getValorTotal())
                + ", sinal " + HistoricoService.pdfMoeda(e.getValorSinal()) + ".");

        // As fotos de referência do pedido passam para a encomenda (cópias: cada uma tem a sua)
        for (OrcamentoFoto f : fotos.findByOrcamentoIdOrderByIdAsc(id)) {
            EncomendaFoto ef = new EncomendaFoto();
            ef.setEncomenda(e);
            ef.setTipo(TipoFoto.REFERENCIA);
            ef.setLegenda("Referência do pedido de orçamento");
            ef.setArquivo(armazenamento.copiarPrivada(f.getArquivo()));
            fotosEncomenda.save(ef);
        }
        return resp;
    }

    @Transactional
    public void excluir(Long id) {
        Orcamento o = buscar(id);
        if (encomendas.existsByOrcamentoId(id)) {
            throw new RegraNegocioException("Este orçamento já virou encomenda e não pode ser excluído.");
        }
        List<OrcamentoFoto> lista = fotos.findByOrcamentoIdOrderByIdAsc(id);
        fotos.deleteAll(lista);
        itens.deleteByOrcamentoId(id);
        repo.delete(o);
        lista.forEach(f -> armazenamento.excluirPrivada(f.getArquivo()));
    }

    @Transactional(readOnly = true)
    public Resource arquivoFoto(Long fotoId) {
        return armazenamento.abrirPrivada(buscarFoto(fotoId).getArquivo());
    }

    @Transactional(readOnly = true)
    public String tipoMimeFoto(Long fotoId) {
        return ArmazenamentoService.tipoMime(buscarFoto(fotoId).getArquivo());
    }

    private OrcamentoFoto buscarFoto(Long id) {
        return fotos.findById(id).orElseThrow(() -> new NaoEncontradoException("Foto"));
    }

    private Orcamento buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Orçamento"));
    }

    private void preencher(Orcamento o, OrcamentoReq r) {
        o.setCliente(r.clienteId() == null ? null : clientes.buscar(r.clienteId()));
        o.setNome(r.nome().trim());
        o.setWhatsapp(Texto.whatsapp(r.whatsapp()));
        o.setTipoPeca(r.tipoPeca().trim());
        o.setProduto(r.produtoId() == null ? null
                : produtos.findById(r.produtoId()).orElseThrow(() -> new NaoEncontradoException("Peça")));
        o.setQuantidade(r.quantidade() == null ? 1 : r.quantidade());
        o.setPersonalizacao(Texto.limpar(r.personalizacao()));
        o.setMedidas(Texto.limpar(r.medidas()));
        o.setCores(Texto.limpar(r.cores()));
        o.setDataDesejada(r.dataDesejada());
        o.setDetalhes(Texto.limpar(r.detalhes()));
        o.setValorProposto(r.valorProposto());
    }

    private OrcamentoResp paraResp(Orcamento o) {
        Long encomendaId = null;
        if (o.getStatus() == StatusOrcamento.APROVADO) {
            encomendaId = encomendas.findFirstByOrcamentoId(o.getId()).map(Encomenda::getId).orElse(null);
        }
        List<FotoOrcamentoResp> lista = o.getId() == null ? List.of()
                : fotos.findByOrcamentoIdOrderByIdAsc(o.getId()).stream()
                .map(f -> new FotoOrcamentoResp(f.getId(), "/api/orcamentos/fotos/" + f.getId() + "/arquivo")).toList();
        Produto p = o.getProduto();
        List<ItemOrcamentoResp> pecas = List.of();
        if (o.getId() != null) {
            var daSacola = itens.findByOrcamentoIdOrderByIdAsc(o.getId());
            if (!daSacola.isEmpty()) {
                var par = precificacao.parametros();
                pecas = daSacola.stream().map(i -> new ItemOrcamentoResp(i.getProduto().getId(), i.getProduto().getNome(),
                        i.getQuantidade(), i.getProduto().isMostrarPreco()
                        ? precificacao.calcular(i.getProduto(), par).precoAtual() : null)).toList();
            }
        }
        return new OrcamentoResp(o.getId(), o.getCliente() == null ? null : o.getCliente().getId(), o.getNome(),
                o.getWhatsapp(), o.getTipoPeca(), o.getMedidas(), o.getCores(), o.getDataDesejada(), o.getDetalhes(),
                o.getValorProposto(), o.getStatus(), o.getOrigem(), o.getCriadoEm(), encomendaId,
                p == null ? null : p.getId(), p == null ? null : p.getNome(), o.getQuantidade(), o.getPersonalizacao(),
                lista, pecas);
    }
}
