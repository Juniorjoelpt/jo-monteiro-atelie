package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Configuracao;
import br.com.mssolucoes.atelie.dto.Dtos.ConfiguracaoDto;
import br.com.mssolucoes.atelie.dto.Dtos.DocumentosConfig;
import br.com.mssolucoes.atelie.repositorio.ConfiguracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ConfiguracaoService {

    private final ConfiguracaoRepository repo;

    public ConfiguracaoService(ConfiguracaoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Configuracao atual() {
        return repo.findById(Configuracao.ID_UNICO).orElseGet(ConfiguracaoService::padrao);
    }

    @Transactional(readOnly = true)
    public ConfiguracaoDto dto() {
        return paraDto(atual());
    }

    @Transactional
    public ConfiguracaoDto salvar(ConfiguracaoDto d) {
        Configuracao c = repo.findById(Configuracao.ID_UNICO).orElseGet(ConfiguracaoService::padrao);
        c.setNomeAtelie(d.nomeAtelie().trim());
        c.setWhatsapp(Texto.whatsapp(d.whatsapp()));
        c.setInstagram(Texto.limpar(d.instagram()) == null ? null : d.instagram().trim().replaceFirst("^@", ""));
        c.setChavePix(Texto.limpar(d.chavePix()));
        c.setTitularPix(Texto.limpar(d.titularPix()));
        c.setEndereco(Texto.limpar(d.endereco()));
        c.setHorario(Texto.limpar(d.horario()));
        c.setCidade(Texto.limpar(d.cidade()));
        c.setSobreTexto(Texto.limpar(d.sobreTexto()));
        c.setAtualizadoEm(LocalDateTime.now());
        return paraDto(repo.save(c));
    }

    public static final String CONDICOES_PADRAO =
            "O pagamento é feito com um sinal para confirmar a encomenda e o restante na entrega, por Pix, dinheiro "
            + "ou cartão. As peças personalizadas só entram em produção depois do pagamento do sinal e da aprovação "
            + "da arte, que é enviada por um link para a cliente conferir nomes, frases, datas e cores. O prazo de "
            + "produção começa a contar a partir dessa aprovação. Por serem feitas à mão, as peças podem ter pequenas "
            + "variações de cor em relação à arte vista na tela.";

    public static final String CANCELAMENTO_PADRAO =
            "O sinal confirma a encomenda e reserva a data. Em caso de desistência, o sinal não é devolvido, pois "
            + "cobre os materiais comprados e o trabalho já iniciado. Cancelamentos com menos de 15 dias da data "
            + "de entrega podem ter cobrança proporcional ao que já foi produzido. Erros de escrita aprovados na arte "
            + "não são de responsabilidade do ateliê, e mudanças depois da aprovação podem alterar o valor e o prazo.";

    @Transactional(readOnly = true)
    public DocumentosConfig documentos() {
        Configuracao c = atual();
        return new DocumentosConfig(c.getResponsavel(), c.getDocumento(), c.getSinalPercentual(),
                c.getValidadePropostaDias(),
                c.getCondicoesProposta() != null ? c.getCondicoesProposta() : CONDICOES_PADRAO,
                c.getPoliticaCancelamento() != null ? c.getPoliticaCancelamento() : CANCELAMENTO_PADRAO);
    }

    @Transactional
    public DocumentosConfig salvarDocumentos(DocumentosConfig d) {
        Configuracao c = repo.findById(Configuracao.ID_UNICO).orElseGet(ConfiguracaoService::padrao);
        c.setResponsavel(Texto.limpar(d.responsavel()));
        c.setDocumento(Texto.limpar(d.documento()));
        c.setSinalPercentual(d.sinalPercentual());
        c.setValidadePropostaDias(d.validadePropostaDias());
        c.setCondicoesProposta(Texto.limpar(d.condicoesProposta()));
        c.setPoliticaCancelamento(Texto.limpar(d.politicaCancelamento()));
        c.setAtualizadoEm(LocalDateTime.now());
        repo.save(c);
        return documentos();
    }

    public static Configuracao padrao() {
        Configuracao c = new Configuracao();
        c.setNomeAtelie("Ateliê Jô Monteiro");
        // Preencher em Configurações: enquanto vazio, os botões do site usam o link de orçamento do Instagram
        c.setWhatsapp("");
        c.setInstagram("jomonteiroatelie");
        c.setCidade("Governador Nunes Freire — MA");
        c.setHorario("Segunda a sexta, das 8h às 17h30 · Sábado até 12h");
        c.setSobreTexto("Papelaria criativa e presentes únicos, feitos à mão em Governador Nunes Freire. "
                + "Cada caderneta, caneca e presente nasce de uma conversa: entendo a ocasião e quem vai receber, "
                + "crio a arte e só produzo depois que você aprova.");
        return c;
    }

    public static ConfiguracaoDto paraDto(Configuracao c) {
        return new ConfiguracaoDto(c.getNomeAtelie(), c.getWhatsapp(), c.getInstagram(), c.getChavePix(),
                c.getTitularPix(), c.getEndereco(), c.getHorario(), c.getCidade(), c.getSobreTexto());
    }
}
