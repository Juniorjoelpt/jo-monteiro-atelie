package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.seguranca.UsuarioLogado;
import br.com.mssolucoes.atelie.servico.DepoimentoService;
import br.com.mssolucoes.atelie.servico.EquipeService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Equipe (acessos) e depoimentos do site. */
@RestController
@RequestMapping("/api")
public class EquipeSiteController {

    private final EquipeService equipe;
    private final DepoimentoService depoimentos;

    public EquipeSiteController(EquipeService equipe, DepoimentoService depoimentos) {
        this.equipe = equipe;
        this.depoimentos = depoimentos;
    }

    // ---------- Equipe ----------

    @GetMapping("/equipe")
    public List<UsuarioResp> equipe(@AuthenticationPrincipal UsuarioLogado eu) {
        return equipe.listar(eu.id());
    }

    @PostMapping("/equipe")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResp criarAcesso(@Valid @RequestBody UsuarioReq r, @AuthenticationPrincipal UsuarioLogado eu) {
        return equipe.criar(r, eu.id());
    }

    @PutMapping("/equipe/{id}")
    public UsuarioResp atualizarAcesso(@PathVariable Long id, @Valid @RequestBody UsuarioReq r,
                                       @AuthenticationPrincipal UsuarioLogado eu) {
        return equipe.atualizar(id, r, eu.id());
    }

    @DeleteMapping("/equipe/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirAcesso(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado eu) {
        equipe.excluir(id, eu.id());
    }

    // ---------- Depoimentos ----------

    @GetMapping("/depoimentos")
    public List<DepoimentoResp> depoimentos() {
        return depoimentos.listar();
    }

    @PostMapping(value = "/depoimentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DepoimentoResp criarDepoimento(@Valid @RequestPart("dados") DepoimentoReq dados,
                                          @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return depoimentos.criar(dados, foto);
    }

    @PostMapping(value = "/depoimentos/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DepoimentoResp atualizarDepoimento(@PathVariable Long id, @Valid @RequestPart("dados") DepoimentoReq dados,
                                              @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return depoimentos.atualizar(id, dados, foto);
    }

    @DeleteMapping("/depoimentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirDepoimento(@PathVariable Long id) {
        depoimentos.excluir(id);
    }

    static ResponseEntity<byte[]> pdf(br.com.mssolucoes.atelie.servico.DocumentoService.Arquivo a) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.nome(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(a.bytes());
    }
}
