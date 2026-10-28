package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.dominio.Usuario;
import br.com.mssolucoes.atelie.dto.Dtos.LoginReq;
import br.com.mssolucoes.atelie.dto.Dtos.LoginResp;
import br.com.mssolucoes.atelie.dto.Dtos.TrocarSenhaReq;
import br.com.mssolucoes.atelie.repositorio.UsuarioRepository;
import br.com.mssolucoes.atelie.seguranca.JwtService;
import br.com.mssolucoes.atelie.seguranca.LimitadorDeRequisicoes;
import br.com.mssolucoes.atelie.seguranca.UsuarioLogado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LimitadorDeRequisicoes limitador;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt,
                          LimitadorDeRequisicoes limitador) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.limitador = limitador;
    }

    @PostMapping("/login")
    public LoginResp login(@Valid @RequestBody LoginReq r, HttpServletRequest req) {
        limitador.verificar(req, "login", 10, 15 * 60);
        Usuario u = usuarios.findByEmailIgnoreCase(r.email().trim())
                .filter(Usuario::isAtivo)
                .filter(x -> encoder.matches(r.senha(), x.getSenhaHash()))
                .orElseThrow(() -> new BadCredentialsException("credenciais"));
        return new LoginResp(jwt.gerar(u.getId(), u.getEmail()), u.getNome(), u.getEmail(), u.getPerfil());
    }

    @GetMapping("/eu")
    public UsuarioLogado eu(@AuthenticationPrincipal UsuarioLogado usuario) {
        return usuario;
    }

    @PostMapping("/trocar-senha")
    @Transactional
    public Map<String, String> trocarSenha(@AuthenticationPrincipal UsuarioLogado usuario,
                                           @Valid @RequestBody TrocarSenhaReq r) {
        Usuario u = usuarios.findById(usuario.id()).orElseThrow(() -> new NaoEncontradoException("Usuária"));
        if (!encoder.matches(r.senhaAtual(), u.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual não confere.");
        }
        u.setSenhaHash(encoder.encode(r.novaSenha()));
        usuarios.save(u);
        return Map.of("mensagem", "Senha alterada.");
    }
}
