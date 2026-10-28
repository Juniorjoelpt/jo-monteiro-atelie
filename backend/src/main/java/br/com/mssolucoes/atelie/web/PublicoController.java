package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.seguranca.LimitadorDeRequisicoes;
import br.com.mssolucoes.atelie.servico.ConfiguracaoService;
import br.com.mssolucoes.atelie.servico.OrcamentoService;
import br.com.mssolucoes.atelie.servico.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Rotas usadas pela landing page, sem login. */
@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private final ConfiguracaoService configuracao;
    private final PortfolioService portfolio;
    private final OrcamentoService orcamentos;
    private final LimitadorDeRequisicoes limitador;
    private final br.com.mssolucoes.atelie.servico.DepoimentoService depoimentos;
    private final br.com.mssolucoes.atelie.servico.CatalogoService catalogo;

    public PublicoController(ConfiguracaoService configuracao, PortfolioService portfolio,
                             OrcamentoService orcamentos, LimitadorDeRequisicoes limitador,
                             br.com.mssolucoes.atelie.servico.DepoimentoService depoimentos,
                             br.com.mssolucoes.atelie.servico.CatalogoService catalogo) {
        this.catalogo = catalogo;
        this.depoimentos = depoimentos;
        this.configuracao = configuracao;
        this.portfolio = portfolio;
        this.orcamentos = orcamentos;
        this.limitador = limitador;
    }

    @GetMapping("/site")
    public SitePublico site() {
        return new SitePublico(configuracao.dto(), portfolio.publicados(),
                depoimentos.publicados(), catalogo.publico(), catalogo.maisPedidos());
    }

    @GetMapping("/catalogo")
    public java.util.List<ItemCatalogo> catalogo() {
        return catalogo.publico();
    }

    /** Pedido de orçamento: os dados vão em "dados" (JSON) e até 3 fotos de referência em "fotos". */
    @PostMapping(value = "/orcamentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> pedirOrcamento(@Valid @RequestPart("dados") OrcamentoPublicoReq r,
                                              @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos,
                                              HttpServletRequest req) {
        limitador.verificar(req, "orcamento", 5, 60 * 60);
        orcamentos.receberDoSite(r, fotos);
        return Map.of("mensagem", "Pedido recebido!");
    }
}
