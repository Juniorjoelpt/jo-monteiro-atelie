package br.com.joeljunior.atelie.servico;

import br.com.joeljunior.atelie.dominio.Cliente;
import br.com.joeljunior.atelie.dominio.Configuracao;
import br.com.joeljunior.atelie.dominio.Encomenda;
import br.com.joeljunior.atelie.dominio.Orcamento;
import br.com.joeljunior.atelie.dto.Dtos.DocumentosConfig;
import br.com.joeljunior.atelie.repositorio.EncomendaRepository;
import br.com.joeljunior.atelie.repositorio.LancamentoRepository;
import br.com.joeljunior.atelie.repositorio.OrcamentoRepository;
import br.com.joeljunior.atelie.web.NaoEncontradoException;
import br.com.joeljunior.atelie.web.RegraNegocioException;
import com.lowagie.text.Paragraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static br.com.joeljunior.atelie.servico.pdf.PdfAtelie.*;

/** Proposta (a partir do orçamento) e contrato (a partir da encomenda) em PDF, com a marca do ateliê. */
@Service
public class DocumentoService {

    private static final String EM_BRANCO = "______________________________";

    private final OrcamentoRepository orcamentos;
    private final EncomendaRepository encomendas;
    private final LancamentoRepository lancamentos;
    private final ConfiguracaoService configuracao;

    public DocumentoService(OrcamentoRepository orcamentos, EncomendaRepository encomendas,
                            LancamentoRepository lancamentos, ConfiguracaoService configuracao) {
        this.orcamentos = orcamentos;
        this.encomendas = encomendas;
        this.lancamentos = lancamentos;
        this.configuracao = configuracao;
    }

    public record Arquivo(String nome, byte[] bytes) {
    }

    @Transactional(readOnly = true)
    public Arquivo proposta(Long orcamentoId) {
        Orcamento o = orcamentos.findById(orcamentoId).orElseThrow(() -> new NaoEncontradoException("Orçamento"));
        if (o.getValorProposto() == null || o.getValorProposto().signum() <= 0) {
            throw new RegraNegocioException("Informe o valor proposto no orçamento antes de gerar a proposta.");
        }
        Configuracao cfg = configuracao.atual();
        DocumentosConfig docs = configuracao.documentos();
        LocalDate hoje = LocalDate.now();
        BigDecimal total = o.getValorProposto();
        BigDecimal sinal = total.multiply(docs.sinalPercentual()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String num = String.format("%04d", o.getId());

        byte[] pdf = gerar(cfg, "Proposta " + num, doc -> {
            cabecalho(doc, cfg, "Proposta", "Nº " + num + "  ·  " + data(hoje));
            adicionar(doc, ficha(new String[][]{
                    {"Para", o.getNome()},
                    {"WhatsApp", o.getWhatsapp() != null ? telefone(o.getWhatsapp()) : null}}));

            secao(doc, "A peça");
            adicionar(doc, ficha(new String[][]{
                    {"Tipo de peça", o.getTipoPeca()},
                    {"Medidas ou quantidade", o.getMedidas()},
                    {"Cores", o.getCores()},
                    {"Para quando", o.getDataDesejada() != null ? data(o.getDataDesejada()) : null},
                    {"Detalhes", o.getDetalhes()}}));

            secao(doc, "Investimento");
            adicionar(doc, caixas(OLIVA_CLARO, new String[][]{
                    {"VALOR TOTAL", moeda(total), null},
                    {"SINAL PARA CONFIRMAR (" + numero(docs.sinalPercentual()) + "%)", moeda(sinal), null},
                    {"RESTANTE NA ENTREGA", moeda(total.subtract(sinal)), null}}));

            secao(doc, "Condições");
            paragrafo(doc, docs.condicoesProposta());
            paragrafo(doc, "Esta proposta é válida até " + data(hoje.plusDays(docs.validadePropostaDias()))
                    + " (" + docs.validadePropostaDias() + " dias).");

            secao(doc, "Como confirmar");
            StringBuilder confirmar = new StringBuilder("Para confirmar, é só responder pelo WhatsApp");
            if (cfg.getChavePix() != null) {
                confirmar.append(" e fazer o Pix do sinal de ").append(moeda(sinal)).append(" para a chave ")
                        .append(cfg.getChavePix());
                if (cfg.getTitularPix() != null) {
                    confirmar.append(" (").append(cfg.getTitularPix()).append(")");
                }
            }
            confirmar.append(". Assim que o sinal for confirmado, a sua peça entra na fila de produção.");
            paragrafo(doc, confirmar.toString());

            Paragraph despedida = new Paragraph("Com carinho,\n" + (cfg.getResponsavel() != null ? cfg.getResponsavel()
                    : cfg.getNomeAtelie()), TEXTO_FORTE);
            despedida.setSpacingBefore(18);
            adicionar(doc, despedida);
        });
        return new Arquivo("Proposta-" + num + "-" + nomeArquivo(o.getNome()) + ".pdf", pdf);
    }

    @Transactional(readOnly = true)
    public Arquivo contrato(Long encomendaId) {
        Encomenda e = encomendas.findById(encomendaId).orElseThrow(() -> new NaoEncontradoException("Encomenda"));
        Configuracao cfg = configuracao.atual();
        DocumentosConfig docs = configuracao.documentos();
        Cliente c = e.getCliente();
        LocalDate hoje = LocalDate.now();
        BigDecimal pago = lancamentos.totalPagoEncomenda(e.getId());
        BigDecimal saldo = e.getValorTotal().subtract(pago).max(BigDecimal.ZERO);
        String num = String.format("%04d", e.getId());
        String cidade = cfg.getCidade() != null ? cfg.getCidade() : "";
        String comarca = cidade.split("[—\\-/,]")[0].trim();

        byte[] pdf = gerar(cfg, "Contrato " + num, doc -> {
            cabecalho(doc, cfg, "Contrato de encomenda", "Nº " + num + "  ·  " + data(hoje));

            secao(doc, "As partes");
            adicionar(doc, ficha(new String[][]{
                    {"Contratada", cfg.getNomeAtelie() + (cfg.getResponsavel() != null ? ", representada por " + cfg.getResponsavel() : "")},
                    {"CPF/CNPJ", cfg.getDocumento() != null ? cfg.getDocumento() : EM_BRANCO},
                    {"Endereço", cfg.getEndereco() != null ? cfg.getEndereco() : (cidade.isBlank() ? EM_BRANCO : cidade)},
                    {"Contratante", c.getNome()},
                    {"CPF/CNPJ", c.getDocumento() != null ? c.getDocumento() : EM_BRANCO},
                    {"Endereço", c.getEndereco() != null ? c.getEndereco() : EM_BRANCO},
                    {"WhatsApp", c.getWhatsapp() != null ? telefone(c.getWhatsapp()) : EM_BRANCO}}));

            secao(doc, "1. Objeto");
            String objeto = "Confecção artesanal e personalizada de: " + e.getDescricao()
                    + (e.getProduto() != null ? " (" + e.getQuantidade() + " × " + e.getProduto().getNome() + ")" : "")
                    + "." + (e.getPersonalizacao() != null ? " Personalização: " + e.getPersonalizacao() + "." : "");
            paragrafo(doc, objeto + (e.getObservacoes() != null ? " Detalhes combinados: " + e.getObservacoes() : ""));

            secao(doc, "2. Valor e pagamento");
            adicionar(doc, caixas(OLIVA_CLARO, new String[][]{
                    {"VALOR TOTAL", moeda(e.getValorTotal()), null},
                    {"SINAL", moeda(e.getValorSinal()), "para confirmar a encomenda"},
                    {"JÁ RECEBIDO", moeda(pago), saldo.signum() > 0 ? "falta " + moeda(saldo) : "quitado"}}));
            paragrafo(doc, "O sinal confirma a encomenda e o início da produção. O saldo restante deve ser pago até a "
                    + "entrega da peça, por Pix, dinheiro ou cartão"
                    + (cfg.getChavePix() != null ? " (chave Pix: " + cfg.getChavePix() + ")" : "") + ".");

            secao(doc, "3. Prazo de entrega");
            paragrafo(doc, e.getPrazoEntrega() != null
                    ? "A entrega está prevista para " + dataExtenso(e.getPrazoEntrega()) + ", contada a partir do "
                    + "pagamento do sinal. Atrasos no pagamento do sinal ou mudanças no pedido podem alterar esse prazo."
                    : "A data de entrega será combinada entre as partes após o pagamento do sinal.");

            secao(doc, "4. Cancelamento e alterações");
            paragrafo(doc, docs.politicaCancelamento());

            secao(doc, "5. Peça artesanal");
            paragrafo(doc, "A peça é feita à mão, por isso pode ter pequenas variações de cor, medida e acabamento em "
                    + "relação às fotos de referência, sem que isso seja considerado defeito.");

            secao(doc, "6. Disposições gerais");
            paragrafo(doc, "As partes buscarão resolver de forma amigável qualquer divergência sobre este contrato."
                    + (comarca.isBlank() ? "" : " Persistindo, fica eleito o foro da comarca de " + comarca + "."));

            Paragraph local = new Paragraph((cidade.isBlank() ? "" : cidade + ", ") + dataExtenso(hoje) + ".", TEXTO);
            local.setSpacingBefore(14);
            adicionar(doc, local);
            assinaturas(doc, cfg.getNomeAtelie() + (cfg.getResponsavel() != null ? "\n" + cfg.getResponsavel() : ""),
                    c.getNome());
        });
        return new Arquivo("Contrato-" + num + "-" + nomeArquivo(c.getNome()) + ".pdf", pdf);
    }

    static String nomeArquivo(String s) {
        String semAcento = java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpo = semAcento.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return limpo.isEmpty() ? "cliente" : limpo.substring(0, Math.min(40, limpo.length()));
    }
}
