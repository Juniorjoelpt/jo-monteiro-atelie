package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Cliente;
import br.com.mssolucoes.atelie.dominio.Enums.TipoCliente;
import br.com.mssolucoes.atelie.dto.Dtos.ClienteReq;
import br.com.mssolucoes.atelie.dto.Dtos.ClienteResp;
import br.com.mssolucoes.atelie.repositorio.ClienteRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ClienteResp> listar(String busca) {
        return repo.buscar(Texto.limpar(busca)).stream().map(ClienteService::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente"));
    }

    @Transactional
    public ClienteResp criar(ClienteReq r) {
        Cliente c = new Cliente();
        preencher(c, r);
        return paraResp(repo.save(c));
    }

    @Transactional
    public ClienteResp atualizar(Long id, ClienteReq r) {
        Cliente c = buscar(id);
        preencher(c, r);
        return paraResp(repo.save(c));
    }

    @Transactional
    public void excluir(Long id) {
        Cliente c = buscar(id);
        try {
            repo.delete(c);
            repo.flush();
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Esta cliente tem orçamentos, encomendas ou inscrições e não pode ser excluída.");
        }
    }

    /** Encontra a cliente pelo WhatsApp ou cria uma nova. */
    @Transactional
    public Cliente encontrarOuCriar(String nome, String whatsapp, TipoCliente tipo) {
        String fone = Texto.whatsapp(whatsapp);
        if (fone != null) {
            var existente = repo.findFirstByWhatsapp(fone);
            if (existente.isPresent()) {
                return existente.get();
            }
        }
        Cliente c = new Cliente();
        c.setNome(nome.trim());
        c.setWhatsapp(fone);
        c.setTipo(tipo);
        return repo.save(c);
    }

    private void preencher(Cliente c, ClienteReq r) {
        c.setNome(r.nome().trim());
        c.setWhatsapp(Texto.whatsapp(r.whatsapp()));
        c.setEmail(Texto.limpar(r.email()));
        c.setCidade(Texto.limpar(r.cidade()));
        c.setAniversario(r.aniversario());
        c.setTipo(r.tipo() == null ? TipoCliente.CLIENTE : r.tipo());
        c.setObservacoes(Texto.limpar(r.observacoes()));
        c.setDocumento(Texto.limpar(r.documento()));
        c.setEndereco(Texto.limpar(r.endereco()));
    }

    public static ClienteResp paraResp(Cliente c) {
        return new ClienteResp(c.getId(), c.getNome(), c.getWhatsapp(), c.getEmail(), c.getCidade(),
                c.getAniversario(), c.getTipo(), c.getObservacoes(), c.getCriadoEm(), c.getDocumento(), c.getEndereco());
    }
}
