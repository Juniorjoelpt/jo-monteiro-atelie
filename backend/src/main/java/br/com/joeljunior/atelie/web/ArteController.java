package br.com.joeljunior.atelie.web;

import br.com.joeljunior.atelie.dto.Dtos.*;
import br.com.joeljunior.atelie.seguranca.LimitadorDeRequisicoes;
import br.com.joeljunior.atelie.servico.ArmazenamentoService;
import br.com.joeljunior.atelie.servico.ArteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Aprovação de arte: rotas do sistema (com login) e da página da cliente (pelo link, sem login). */
@RestController
@RequestMapping("/api")
public class ArteController {

    private final ArteService artes;
    private final LimitadorDeRequisicoes limitador;

    public ArteController(ArteService artes, LimitadorDeRequisicoes limitador) {
        this.artes = artes;
        this.limitador = limitador;
    }

    // ---------- Sistema ----------

    @GetMapping("/artes/resumo")
    public ResumoArtes resumo() {
        return artes.resumo();
    }

    @GetMapping("/encomendas/{id}/artes")
    public ArtesEncomenda daEncomenda(@PathVariable Long id) {
        return artes.daEncomenda(id);
    }

    @PostMapping(value = "/encomendas/{id}/artes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArtesEncomenda enviar(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo,
                                 @RequestParam(value = "observacao", required = false) String observacao) {
        return artes.enviar(id, observacao, arquivo);
    }

    @PatchMapping("/encomendas/{id}/arte")
    public ArtesEncomenda marcar(@PathVariable Long id, @Valid @RequestBody StatusArteReq r) {
        return artes.marcar(id, r);
    }

    @DeleteMapping("/artes/{id}")
    public ArtesEncomenda excluir(@PathVariable Long id) {
        return artes.excluir(id);
    }

    @GetMapping("/artes/{id}/arquivo")
    public ResponseEntity<Resource> arquivo(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(artes.tipoMime(id)))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(artes.arquivo(id));
    }

    // ---------- Página da cliente ----------

    @GetMapping("/publico/artes/{codigo}")
    public ArtePublica publica(@PathVariable String codigo, HttpServletRequest req) {
        limitador.verificar(req, "arte", 120, 60 * 60);
        return artes.publica(codigo);
    }

    @GetMapping("/publico/artes/{codigo}/versoes/{id}/arquivo")
    public ResponseEntity<Resource> arquivoPublico(@PathVariable String codigo, @PathVariable Long id,
                                                   HttpServletRequest req) {
        limitador.verificar(req, "arte-arquivo", 300, 60 * 60);
        var v = artes.versaoPublica(codigo, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ArmazenamentoService.tipoMime(v.getArquivo())))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(artes.abrir(v));
    }

    /** Mesmo conteúdo, pelo nome novo (página "Acompanhar meu pedido"). */
    @GetMapping("/publico/pedidos/{codigo}")
    public ArtePublica pedido(@PathVariable String codigo, HttpServletRequest req) {
        return publica(codigo, req);
    }

    @PostMapping("/publico/artes/{codigo}/visto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void visto(@PathVariable String codigo, @RequestBody(required = false) VistoReq r, HttpServletRequest req) {
        limitador.verificar(req, "arte-visto", 120, 60 * 60);
        artes.visto(codigo, r == null ? null : r.versaoId());
    }

    @PostMapping("/publico/artes/{codigo}/resposta")
    public ArtePublica responder(@PathVariable String codigo, @Valid @RequestBody RespostaArteReq r,
                                 HttpServletRequest req) {
        limitador.verificar(req, "arte-resposta", 20, 60 * 60);
        return artes.responder(codigo, r);
    }
}
