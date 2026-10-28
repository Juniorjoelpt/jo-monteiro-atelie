package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.servico.CatalogoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/** Catálogo de peças (só a administradora). */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CatalogoService catalogo;

    public CatalogoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    // ---------- Catálogo ----------

    @PutMapping("/produtos/{id}/catalogo")
    public CatalogoInfo atualizarCatalogo(@PathVariable Long id, @Valid @RequestBody CatalogoReq r) {
        return catalogo.atualizar(id, r);
    }

    @PostMapping(value = "/produtos/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogoInfo enviarFotoProduto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto) {
        return catalogo.enviarFoto(id, foto);
    }

    @PostMapping("/fotos-produto/{id}/capa")
    public CatalogoInfo capaProduto(@PathVariable Long id) {
        return catalogo.capa(id);
    }

    @DeleteMapping("/fotos-produto/{id}")
    public CatalogoInfo excluirFotoProduto(@PathVariable Long id) {
        return catalogo.excluirFoto(id);
    }
}
