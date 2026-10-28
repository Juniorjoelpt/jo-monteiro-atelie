package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Depoimento;
import br.com.mssolucoes.atelie.dto.Dtos.DepoimentoReq;
import br.com.mssolucoes.atelie.dto.Dtos.DepoimentoResp;
import br.com.mssolucoes.atelie.repositorio.DepoimentoRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Depoimentos do site. A foto (opcional) fica na pasta pública, como as do portfólio. */
@Service
public class DepoimentoService {

    private final DepoimentoRepository repo;
    private final ArmazenamentoService armazenamento;

    public DepoimentoService(DepoimentoRepository repo, ArmazenamentoService armazenamento) {
        this.repo = repo;
        this.armazenamento = armazenamento;
    }

    @Transactional(readOnly = true)
    public List<DepoimentoResp> listar() {
        return repo.findAllByOrderByOrdemAscIdDesc().stream().map(DepoimentoService::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<DepoimentoResp> publicados() {
        return repo.findByPublicadoTrueOrderByOrdemAscIdDesc().stream().map(DepoimentoService::paraResp).toList();
    }

    @Transactional
    public DepoimentoResp criar(DepoimentoReq r, MultipartFile foto) {
        Depoimento d = new Depoimento();
        preencher(d, r);
        if (foto != null && !foto.isEmpty()) {
            d.setFoto(armazenamento.salvarImagem(foto));
        }
        return paraResp(repo.save(d));
    }

    @Transactional
    public DepoimentoResp atualizar(Long id, DepoimentoReq r, MultipartFile foto) {
        Depoimento d = buscar(id);
        preencher(d, r);
        String antiga = d.getFoto();
        if (foto != null && !foto.isEmpty()) {
            d.setFoto(armazenamento.salvarImagem(foto));
        } else if (Boolean.TRUE.equals(r.removerFoto())) {
            d.setFoto(null);
        }
        repo.save(d);
        if (antiga != null && !antiga.equals(d.getFoto())) {
            armazenamento.excluir(antiga);
        }
        return paraResp(d);
    }

    @Transactional
    public void excluir(Long id) {
        Depoimento d = buscar(id);
        repo.delete(d);
        if (d.getFoto() != null) {
            armazenamento.excluir(d.getFoto());
        }
    }

    private Depoimento buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Depoimento"));
    }

    private static void preencher(Depoimento d, DepoimentoReq r) {
        d.setNome(r.nome().trim());
        d.setContexto(Texto.limpar(r.contexto()));
        // Tira aspas que vieram coladas do WhatsApp: o site já desenha as aspas
        d.setTexto(r.texto().trim().replaceAll("^[\"“”']+|[\"“”']+$", "").trim());
        d.setEstrelas(r.estrelas() == null ? 5 : r.estrelas());
        d.setPublicado(r.publicado() == null || r.publicado());
        d.setOrdem(r.ordem() == null ? 0 : r.ordem());
    }

    public static DepoimentoResp paraResp(Depoimento d) {
        return new DepoimentoResp(d.getId(), d.getNome(), d.getContexto(), d.getTexto(), d.getEstrelas(),
                d.getFoto() == null ? null : "/uploads/" + d.getFoto(), d.isPublicado(), d.getOrdem());
    }
}
