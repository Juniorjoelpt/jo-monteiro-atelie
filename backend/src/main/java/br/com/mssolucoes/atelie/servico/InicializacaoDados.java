package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Configuracao;
import br.com.mssolucoes.atelie.dominio.Usuario;
import br.com.mssolucoes.atelie.repositorio.ConfiguracaoRepository;
import br.com.mssolucoes.atelie.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Na primeira execução cria a usuária administradora e as configurações do ateliê. */
@Component
public class InicializacaoDados implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializacaoDados.class);

    private final UsuarioRepository usuarios;
    private final ConfiguracaoRepository configuracoes;
    private final PasswordEncoder encoder;
    private final String nome;
    private final String email;
    private final String senha;

    public InicializacaoDados(UsuarioRepository usuarios, ConfiguracaoRepository configuracoes, PasswordEncoder encoder,
                              @Value("${atelie.admin.nome}") String nome,
                              @Value("${atelie.admin.email}") String email,
                              @Value("${atelie.admin.senha}") String senha) {
        this.usuarios = usuarios;
        this.configuracoes = configuracoes;
        this.encoder = encoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() == 0) {
            Usuario u = new Usuario();
            u.setNome(nome);
            u.setEmail(email.trim().toLowerCase());
            u.setSenhaHash(encoder.encode(senha));
            usuarios.save(u);
            log.info("Usuária administradora criada: {} (troque a senha no primeiro acesso)", u.getEmail());
        }
        if (!configuracoes.existsById(Configuracao.ID_UNICO)) {
            configuracoes.save(ConfiguracaoService.padrao());
        }
    }
}
