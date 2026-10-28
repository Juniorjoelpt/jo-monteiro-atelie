package br.com.joeljunior.atelie.servico;

import br.com.joeljunior.atelie.dominio.Encomenda;
import br.com.joeljunior.atelie.dominio.EncomendaFoto;
import br.com.joeljunior.atelie.dominio.Enums.CategoriaPortfolio;
import br.com.joeljunior.atelie.dominio.Enums.Frente;
import br.com.joeljunior.atelie.dominio.Enums.TipoFoto;
import br.com.joeljunior.atelie.dominio.PortfolioItem;
import br.com.joeljunior.atelie.dto.Dtos.FotoResp;
import br.com.joeljunior.atelie.dto.Dtos.PublicarFotoReq;
import br.com.joeljunior.atelie.repositorio.EncomendaFotoRepository;
import br.com.joeljunior.atelie.repositorio.EncomendaRepository;
import br.com.joeljunior.atelie.web.NaoEncontradoException;
import br.com.joeljunior.atelie.web.RegraNegocioException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Fotos de cada encomenda: referência da cliente e peça pronta, com publicação no portfólio. */
@Service
public class FotoService {

    private static final int LIMITE_POR_ENCOMENDA = 20;

    private final EncomendaFotoRepository repo;
    private final EncomendaRepository encomendas;
    private final ArmazenamentoService armazenamento;
    private final PortfolioService portfolio;
    private final HistoricoService historico;

    public FotoService(EncomendaFotoRepository repo, EncomendaRepository encomendas,
                       ArmazenamentoService armazenamento, PortfolioService portfolio, HistoricoService historico) {
        this.historico = historico;
        this.repo = repo;
        this.encomendas = encomendas;
        this.armazenamento = armazenamento;
        this.portfolio = portfolio;
    }

    @Transactional(readOnly = true)
    public List<FotoResp> daEncomenda(Long encomendaId) {
        buscarEncomenda(encomendaId);
        return repo.findByEncomendaIdOrderByIdAsc(encomendaId).stream().map(FotoService::paraResp).toList();
    }

    @Transactional
    public FotoResp enviar(Long encomendaId, TipoFoto tipo, String legenda, MultipartFile arquivo) {
        Encomenda e = buscarEncomenda(encomendaId);
        if (repo.countByEncomendaId(encomendaId) >= LIMITE_POR_ENCOMENDA) {
            throw new RegraNegocioException("Limite de " + LIMITE_POR_ENCOMENDA + " fotos por encomenda.");
        }
        EncomendaFoto f = new EncomendaFoto();
        f.setEncomenda(e);
        f.setTipo(tipo == null ? TipoFoto.REFERENCIA : tipo);
        String leg = Texto.limpar(legenda);
        f.setLegenda(leg == null ? null : leg.substring(0, Math.min(160, leg.length())));
        f.setArquivo(armazenamento.salvarImagemPrivada(arquivo));
        historico.encomenda(e, "FOTO", "Enviou uma foto " + (f.getTipo() == TipoFoto.PRONTA ? "da peça pronta." : "de referência."));
        return paraResp(repo.save(f));
    }

    @Transactional(readOnly = true)
    public Resource arquivo(Long fotoId) {
        return armazenamento.abrirPrivada(buscar(fotoId).getArquivo());
    }

    @Transactional(readOnly = true)
    public String tipoMime(Long fotoId) {
        return ArmazenamentoService.tipoMime(buscar(fotoId).getArquivo());
    }

    /** Remove a foto da encomenda. Se ela já foi para o portfólio, a cópia pública continua lá. */
    @Transactional
    public void excluir(Long fotoId) {
        EncomendaFoto f = buscar(fotoId);
        historico.encomenda(f.getEncomenda(), "FOTO", "Excluiu uma foto " + (f.getTipo() == TipoFoto.PRONTA
                ? "da peça pronta." : "de referência."));
        repo.delete(f);
        armazenamento.excluirPrivada(f.getArquivo());
    }

    /** Só a foto da peça pronta pode ir para o site: a referência costuma ser de outra pessoa. */
    @Transactional
    public FotoResp publicar(Long fotoId, PublicarFotoReq r) {
        EncomendaFoto f = buscar(fotoId);
        if (f.getTipo() != TipoFoto.PRONTA) {
            throw new RegraNegocioException("Só fotos da peça pronta podem ir para o portfólio.");
        }
        if (portfolio.existe(f.getPortfolioItemId())) {
            throw new RegraNegocioException("Esta foto já está no portfólio.");
        }
        Encomenda e = f.getEncomenda();
        String titulo = Texto.limpar(r == null ? null : r.titulo());
        if (titulo == null) {
            titulo = e.getProduto() != null ? e.getProduto().getNome() : e.getDescricao();
        }
        CategoriaPortfolio cat = r != null && r.categoria() != null ? r.categoria() : categoria(e.getFrente());
        String publico = armazenamento.publicarCopia(f.getArquivo());
        PortfolioItem item = portfolio.criarComArquivo(titulo.substring(0, Math.min(140, titulo.length())), cat, publico);
        f.setPortfolioItemId(item.getId());
        historico.encomenda(e, "FOTO", "Publicou a foto da peça pronta no portfólio do site.");
        return paraResp(repo.save(f));
    }

    private static CategoriaPortfolio categoria(Frente f) {
        return switch (f) {
            case PAPELARIA -> CategoriaPortfolio.PAPELARIA;
            case PORCELANA -> CategoriaPortfolio.CANECAS;
            case PRESENTE -> CategoriaPortfolio.PRESENTES;
        };
    }

    private EncomendaFoto buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Foto"));
    }

    private Encomenda buscarEncomenda(Long id) {
        return encomendas.findById(id).orElseThrow(() -> new NaoEncontradoException("Encomenda"));
    }

    private static FotoResp paraResp(EncomendaFoto f) {
        return new FotoResp(f.getId(), f.getEncomenda().getId(), f.getTipo(), f.getLegenda(),
                "/api/fotos/" + f.getId() + "/arquivo", f.getPortfolioItemId(), f.getCriadoEm());
    }
}
