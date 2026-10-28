package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dominio.Enums.TipoFoto;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.servico.*;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/** Agenda, lembretes, fotos das encomendas, relatórios e documentos em PDF (exigem login). */
@RestController
@RequestMapping("/api")
public class AgendaDocumentosController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final AgendaService agenda;
    private final LembreteService lembretes;
    private final FotoService fotos;
    private final RelatorioService relatorios;
    private final ExportacaoService exportacao;
    private final DocumentoService documentos;
    private final ConfiguracaoService configuracao;

    public AgendaDocumentosController(AgendaService agenda, LembreteService lembretes, FotoService fotos,
                                      RelatorioService relatorios, ExportacaoService exportacao,
                                      DocumentoService documentos, ConfiguracaoService configuracao) {
        this.agenda = agenda;
        this.lembretes = lembretes;
        this.fotos = fotos;
        this.relatorios = relatorios;
        this.exportacao = exportacao;
        this.documentos = documentos;
        this.configuracao = configuracao;
    }

    // ---------- Agenda ----------

    @GetMapping("/agenda")
    public List<EventoAgenda> agenda(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                     @org.springframework.security.core.annotation.AuthenticationPrincipal
                                     br.com.mssolucoes.atelie.seguranca.UsuarioLogado eu) {
        return agenda.eventos(de, ate, eu == null || !eu.ajudante());
    }

    @GetMapping("/compromissos/{id}")
    public CompromissoResp compromisso(@PathVariable Long id) {
        return agenda.detalhar(id);
    }

    @PostMapping("/compromissos")
    @ResponseStatus(HttpStatus.CREATED)
    public CompromissoResp criarCompromisso(@Valid @RequestBody CompromissoReq r) {
        return agenda.criar(r);
    }

    @PutMapping("/compromissos/{id}")
    public CompromissoResp atualizarCompromisso(@PathVariable Long id, @Valid @RequestBody CompromissoReq r) {
        return agenda.atualizar(id, r);
    }

    @DeleteMapping("/compromissos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirCompromisso(@PathVariable Long id) {
        agenda.excluir(id);
    }

    // ---------- Lembretes ----------

    @GetMapping("/lembretes")
    public List<LembreteResp> lembretes() {
        return lembretes.pendentes();
    }

    @PostMapping("/lembretes/marcar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLembrete(@Valid @RequestBody MarcarLembreteReq r) {
        lembretes.marcar(r.chave());
    }

    // ---------- Fotos da encomenda ----------

    @GetMapping("/encomendas/{id}/fotos")
    public List<FotoResp> fotos(@PathVariable Long id) {
        return fotos.daEncomenda(id);
    }

    @PostMapping(value = "/encomendas/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FotoResp enviarFoto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto,
                               @RequestParam(value = "tipo", required = false) TipoFoto tipo,
                               @RequestParam(value = "legenda", required = false) String legenda) {
        return fotos.enviar(id, tipo, legenda, foto);
    }

    /** A imagem só sai daqui com login; o navegador não guarda em cache compartilhado. */
    @GetMapping("/fotos/{id}/arquivo")
    public ResponseEntity<Resource> arquivoFoto(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fotos.tipoMime(id)))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(fotos.arquivo(id));
    }

    @DeleteMapping("/fotos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirFoto(@PathVariable Long id) {
        fotos.excluir(id);
    }

    @PostMapping("/fotos/{id}/publicar")
    public FotoResp publicarFoto(@PathVariable Long id, @RequestBody(required = false) PublicarFotoReq r) {
        return fotos.publicar(id, r);
    }

    // ---------- Relatórios ----------

    @GetMapping("/relatorios")
    public Relatorio relatorio(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return relatorios.gerar(de, ate);
    }

    @GetMapping("/relatorios/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                           @RequestParam(defaultValue = "xlsx") String formato) {
        Relatorio r = relatorios.gerar(de, ate);
        if ("pdf".equalsIgnoreCase(formato)) {
            return arquivo(ExportacaoService.nomeBase(r) + ".pdf", MediaType.APPLICATION_PDF, exportacao.pdf(r));
        }
        if (!"xlsx".equalsIgnoreCase(formato)) {
            throw new RegraNegocioException("Formato inválido. Use xlsx ou pdf.");
        }
        return arquivo(ExportacaoService.nomeBase(r) + ".xlsx", XLSX, exportacao.excel(r));
    }

    // ---------- Documentos ----------

    @GetMapping("/orcamentos/{id}/proposta")
    public ResponseEntity<byte[]> proposta(@PathVariable Long id) {
        var a = documentos.proposta(id);
        return arquivo(a.nome(), MediaType.APPLICATION_PDF, a.bytes());
    }

    @GetMapping("/encomendas/{id}/contrato")
    public ResponseEntity<byte[]> contrato(@PathVariable Long id) {
        var a = documentos.contrato(id);
        return arquivo(a.nome(), MediaType.APPLICATION_PDF, a.bytes());
    }

    @GetMapping("/configuracoes/documentos")
    public DocumentosConfig configDocumentos() {
        return configuracao.documentos();
    }

    @PutMapping("/configuracoes/documentos")
    public DocumentosConfig salvarConfigDocumentos(@Valid @RequestBody DocumentosConfig r) {
        return configuracao.salvarDocumentos(r);
    }

    private static ResponseEntity<byte[]> arquivo(String nome, MediaType tipo, byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nome, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(bytes);
    }
}
