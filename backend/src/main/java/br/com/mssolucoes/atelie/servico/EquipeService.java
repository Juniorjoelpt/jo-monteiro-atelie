package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Enums.Perfil;
import br.com.mssolucoes.atelie.dominio.Usuario;
import br.com.mssolucoes.atelie.dto.Dtos.UsuarioReq;
import br.com.mssolucoes.atelie.dto.Dtos.UsuarioResp;
import br.com.mssolucoes.atelie.repositorio.UsuarioRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Acessos ao sistema: a dona do ateliê e as ajudantes. */
@Service
public class EquipeService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public EquipeService(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResp> listar(Long eu) {
        return repo.findAllByOrderByNomeAsc().stream().map(u -> paraResp(u, eu)).toList();
    }

    @Transactional
    public UsuarioResp criar(UsuarioReq r, Long eu) {
        if (r.senha() == null || r.senha().isBlank()) {
            throw new RegraNegocioException("Defina uma senha provisória com pelo menos 8 caracteres.");
        }
        Usuario u = new Usuario();
        preencher(u, r);
        u.setSenhaHash(encoder.encode(r.senha()));
        return paraResp(repo.save(u), eu);
    }

    @Transactional
    public UsuarioResp atualizar(Long id, UsuarioReq r, Long eu) {
        Usuario u = buscar(id);
        Perfil perfil = r.perfil() == null ? Perfil.AJUDANTE : r.perfil();
        boolean ativo = r.ativo() == null || r.ativo();
        if (id.equals(eu) && (perfil != Perfil.ADMIN || !ativo)) {
            throw new RegraNegocioException("Você não pode tirar o seu próprio acesso de administradora.");
        }
        if (u.getPerfil() == Perfil.ADMIN && u.isAtivo() && (perfil != Perfil.ADMIN || !ativo)) {
            garantirOutraAdmin();
        }
        preencher(u, r);
        if (r.senha() != null && !r.senha().isBlank()) {
            u.setSenhaHash(encoder.encode(r.senha()));
        }
        return paraResp(repo.save(u), eu);
    }

    @Transactional
    public void excluir(Long id, Long eu) {
        Usuario u = buscar(id);
        if (id.equals(eu)) {
            throw new RegraNegocioException("Você não pode excluir o seu próprio acesso.");
        }
        if (u.getPerfil() == Perfil.ADMIN && u.isAtivo()) {
            garantirOutraAdmin();
        }
        repo.delete(u);
    }

    private void garantirOutraAdmin() {
        if (repo.countByPerfilAndAtivoTrue(Perfil.ADMIN) <= 1) {
            throw new RegraNegocioException("O sistema precisa de pelo menos uma administradora ativa.");
        }
    }

    private void preencher(Usuario u, UsuarioReq r) {
        String email = r.email().trim().toLowerCase();
        repo.findByEmailIgnoreCase(email).filter(outro -> !outro.getId().equals(u.getId())).ifPresent(outro -> {
            throw new RegraNegocioException("Já existe um acesso com este e-mail.");
        });
        u.setNome(r.nome().trim());
        u.setEmail(email);
        u.setPerfil(r.perfil() == null ? Perfil.AJUDANTE : r.perfil());
        u.setAtivo(r.ativo() == null || r.ativo());
    }

    private Usuario buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Acesso"));
    }

    private static UsuarioResp paraResp(Usuario u, Long eu) {
        return new UsuarioResp(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(), u.getCriadoEm(),
                u.getId().equals(eu));
    }
}
