package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda as imagens em duas pastas:
 * - pública (servida em /uploads/**): fotos do portfólio, que aparecem no site;
 * - privada (nunca servida direto): fotos das encomendas, abertas só com login.
 */
@Service
public class ArmazenamentoService {

    private static final Map<String, String> TIPOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path pasta;
    private final Path pastaPrivada;

    public ArmazenamentoService(@Value("${atelie.uploads-dir}") String pasta,
                                @Value("${atelie.uploads-privados-dir}") String pastaPrivada) throws IOException {
        this.pasta = Path.of(pasta).toAbsolutePath().normalize();
        this.pastaPrivada = Path.of(pastaPrivada).toAbsolutePath().normalize();
        if (this.pastaPrivada.startsWith(this.pasta)) {
            throw new IllegalStateException("A pasta privada não pode ficar dentro da pasta pública de uploads.");
        }
        Files.createDirectories(this.pasta);
        Files.createDirectories(this.pastaPrivada);
    }

    public Path getPasta() {
        return pasta;
    }

    public String salvarImagem(MultipartFile arquivo) {
        return salvar(arquivo, pasta);
    }

    public String salvarImagemPrivada(MultipartFile arquivo) {
        return salvar(arquivo, pastaPrivada);
    }

    public void excluir(String nome) {
        apagar(pasta, nome);
    }

    public void excluirPrivada(String nome) {
        apagar(pastaPrivada, nome);
    }

    public Resource abrirPrivada(String nome) {
        Path p = resolver(pastaPrivada, nome);
        if (p == null || !Files.isReadable(p)) {
            throw new NaoEncontradoException("Arquivo da foto");
        }
        return new PathResource(p);
    }

    /** Copia uma foto privada para a pasta pública (ao publicar no portfólio). */
    public String publicarCopia(String nomePrivado) {
        Path origem = resolver(pastaPrivada, nomePrivado);
        if (origem == null || !Files.isReadable(origem)) {
            throw new NaoEncontradoException("Arquivo da foto");
        }
        String extensao = nomePrivado.substring(nomePrivado.lastIndexOf('.'));
        String novo = UUID.randomUUID() + extensao;
        try {
            Files.copy(origem, pasta.resolve(novo));
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível publicar a foto.");
        }
        return novo;
    }

    /** Cópia de um arquivo privado (a referência do orçamento passa para a encomenda). */
    public String copiarPrivada(String nomePrivado) {
        Path origem = resolver(pastaPrivada, nomePrivado);
        if (origem == null || !Files.isReadable(origem)) {
            throw new NaoEncontradoException("Arquivo da foto");
        }
        String novo = UUID.randomUUID() + nomePrivado.substring(nomePrivado.lastIndexOf('.'));
        try {
            Files.copy(origem, pastaPrivada.resolve(novo));
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível copiar a foto.");
        }
        return novo;
    }

    public static String tipoMime(String nome) {
        if (nome.endsWith(".png")) {
            return "image/png";
        }
        if (nome.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String salvar(MultipartFile arquivo, Path destino) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Envie uma imagem.");
        }
        String extensao = TIPOS.get(arquivo.getContentType());
        if (extensao == null) {
            throw new RegraNegocioException("Formato não aceito. Use JPG, PNG ou WEBP.");
        }
        try (InputStream in = arquivo.getInputStream()) {
            byte[] cabecalho = in.readNBytes(12);
            if (!assinaturaValida(cabecalho, extensao)) {
                throw new RegraNegocioException("O arquivo enviado não é uma imagem válida.");
            }
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível ler a imagem.");
        }
        String nome = UUID.randomUUID() + extensao;
        try (InputStream in = arquivo.getInputStream()) {
            Files.copy(in, destino.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível salvar a imagem.");
        }
        return nome;
    }

    private static Path resolver(Path base, String nome) {
        if (nome == null || nome.contains("/") || nome.contains("\\") || nome.contains("..")) {
            return null;
        }
        Path p = base.resolve(nome).normalize();
        return p.startsWith(base) ? p : null;
    }

    private static void apagar(Path base, String nome) {
        Path p = resolver(base, nome);
        if (p == null) {
            return;
        }
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignorado) {
            // a foto some do sistema mesmo que o arquivo não seja apagado
        }
    }

    private boolean assinaturaValida(byte[] b, String ext) {
        if (b.length < 12) {
            return false;
        }
        return switch (ext) {
            case ".jpg" -> (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
            case ".png" -> (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case ".webp" -> b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }
}
