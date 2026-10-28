package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.PortfolioItem;
import br.com.mssolucoes.atelie.dto.Dtos.PortfolioReq;
import br.com.mssolucoes.atelie.dto.Dtos.PortfolioResp;
import br.com.mssolucoes.atelie.repositorio.PortfolioItemRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioItemRepository repo;
    private final ArmazenamentoService armazenamento;

    public PortfolioService(PortfolioItemRepository repo, ArmazenamentoService armazenamento) {
        this.repo = repo;
        this.armazenamento = armazenamento;
    }

    @Transactional(readOnly = true)
    public List<PortfolioResp> listar() {
        return repo.findAllByOrderByOrdemAscIdDesc().stream().map(PortfolioService::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<PortfolioResp> publicados() {
        return repo.findByPublicadoTrueOrderByOrdemAscIdDesc().stream().map(PortfolioService::paraResp).toList();
    }

    @Transactional
    public PortfolioResp criar(PortfolioReq r, MultipartFile foto) {
        String arquivo = armazenamento.salvarImagem(foto);
        PortfolioItem p = new PortfolioItem();
        p.setArquivo(arquivo);
        preencher(p, r);
        return paraResp(repo.save(p));
    }

    /** Cria o item a partir de um arquivo que já está na pasta pública (foto publicada de uma encomenda). */
    @Transactional
    public PortfolioItem criarComArquivo(String titulo, br.com.mssolucoes.atelie.dominio.Enums.CategoriaPortfolio categoria,
                                         String arquivoPublico) {
        PortfolioItem p = new PortfolioItem();
        p.setArquivo(arquivoPublico);
        p.setTitulo(titulo);
        p.setCategoria(categoria);
        p.setOrdem(0);
        p.setPublicado(true);
        return repo.save(p);
    }

    @Transactional(readOnly = true)
    public boolean existe(Long id) {
        return id != null && repo.existsById(id);
    }

    @Transactional
    public PortfolioResp atualizar(Long id, PortfolioReq r) {
        PortfolioItem p = buscar(id);
        preencher(p, r);
        return paraResp(repo.save(p));
    }

    @Transactional
    public void excluir(Long id) {
        PortfolioItem p = buscar(id);
        repo.delete(p);
        armazenamento.excluir(p.getArquivo());
    }

    private PortfolioItem buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Foto"));
    }

    private void preencher(PortfolioItem p, PortfolioReq r) {
        p.setTitulo(r.titulo().trim());
        p.setCategoria(r.categoria());
        p.setOrdem(r.ordem() == null ? 0 : r.ordem());
        p.setPublicado(r.publicado() == null || r.publicado());
    }

    public static PortfolioResp paraResp(PortfolioItem p) {
        return new PortfolioResp(p.getId(), p.getTitulo(), p.getCategoria(), "/uploads/" + p.getArquivo(),
                p.getOrdem(), p.isPublicado());
    }
}
