package br.com.mssolucoes.atelie.servico.pdf;

import br.com.mssolucoes.atelie.dominio.Configuracao;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Estilo dos PDFs do ateliê, com a identidade visual: logo no cabeçalho, Montserrat,
 * preto #231F20, laranja da marca (escurecido para texto branco ler bem) e o roxo da blusa da mascote.
 */
public final class PdfAtelie {

    public static final Color MARROM = new Color(0x23, 0x1F, 0x20);
    public static final Color MARROM_2 = new Color(0x4A, 0x42, 0x45);
    /** Laranja da marca, um tom mais escuro (o #F2591A não dá contraste com texto branco). */
    public static final Color TERRA = new Color(0xC9, 0x46, 0x0E);
    public static final Color TERRA_CLARO = new Color(0xFD, 0xE4, 0xD7);
    /** Roxo da mascote (cor de apoio). */
    public static final Color OLIVA = new Color(0x5B, 0x3F, 0x96);
    public static final Color OLIVA_CLARO = new Color(0xEE, 0xE8, 0xF7);
    public static final Color CRU = new Color(0xFF, 0xF8, 0xF3);
    /** Pêssego claro. */
    public static final Color BEGE = new Color(0xF7, 0xC9, 0xB0);
    public static final Color AREIA = new Color(0xF1, 0xE4, 0xDA);
    public static final Color LINHA = new Color(0xED, 0xE3, 0xDC);

    public static final BaseFont REGULAR = fonte("Montserrat-Regular.ttf");
    public static final BaseFont MEDIA = fonte("Montserrat-Medium.ttf");
    public static final BaseFont SEMINEGRITO = fonte("Montserrat-SemiBold.ttf");
    public static final BaseFont NEGRITO = fonte("Montserrat-Bold.ttf");

    public static final Font TITULO = new Font(NEGRITO, 18, Font.NORMAL, MARROM);
    public static final Font SUBTITULO = new Font(REGULAR, 9, Font.NORMAL, MARROM_2);
    public static final Font SECAO = new Font(NEGRITO, 11, Font.NORMAL, TERRA);
    public static final Font TEXTO = new Font(REGULAR, 9.5f, Font.NORMAL, MARROM);
    public static final Font TEXTO_FORTE = new Font(SEMINEGRITO, 9.5f, Font.NORMAL, MARROM);
    public static final Font PEQUENO = new Font(REGULAR, 7.5f, Font.NORMAL, MARROM_2);
    public static final Font TABELA_CAB = new Font(SEMINEGRITO, 7.5f, Font.NORMAL, MARROM_2);
    public static final Font TABELA = new Font(REGULAR, 8.5f, Font.NORMAL, MARROM);
    public static final Font TABELA_FORTE = new Font(SEMINEGRITO, 8.5f, Font.NORMAL, MARROM);
    public static final Font DESTAQUE = new Font(NEGRITO, 15, Font.NORMAL, TERRA);

    private static final byte[] LOGO_HORIZONTAL = recurso("marca/horizontal.png");
    private static final byte[] LOGO_SIMBOLO = recurso("marca/simbolo.png");
    private static final byte[] LOGO_EMPILHADA = recurso("marca/empilhada.png");

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_EXTENSO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT_BR);

    private PdfAtelie() {
    }

    /** Cria o documento A4, escreve o conteúdo e devolve os bytes do PDF. */
    public static byte[] gerar(Configuracao cfg, String titulo, Consumer<Document> conteudo) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 48, 48, 48, 64);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new Rodape(cfg));
        doc.addTitle(titulo);
        doc.addAuthor(cfg.getNomeAtelie());
        doc.addCreator("Sistema do ateliê — MS Soluções Inteligentes");
        doc.open();
        conteudo.accept(doc);
        doc.close();
        return out.toByteArray();
    }

    /** Logo do ateliê à esquerda, título do documento à direita, linha marrom embaixo. */
    public static void cabecalho(Document doc, Configuracao cfg, String titulo, String subtitulo) {
        PdfPTable t = new PdfPTable(new float[]{1f, 1.25f});
        t.setWidthPercentage(100);
        PdfPCell marca = semBorda();
        Image logo = imagem(LOGO_HORIZONTAL);
        logo.scaleToFit(170, 54);
        logo.setSpacingAfter(4);
        marca.addElement(logo);
        StringBuilder contato = new StringBuilder();
        if (cfg.getWhatsapp() != null && !cfg.getWhatsapp().isBlank()) {
            contato.append("WhatsApp ").append(telefone(cfg.getWhatsapp()));
        }
        if (cfg.getInstagram() != null) {
            contato.append(contato.isEmpty() ? "" : "  ·  ").append("@").append(cfg.getInstagram());
        }
        marca.addElement(new Paragraph(contato.toString(), PEQUENO));
        if (cfg.getCidade() != null) {
            marca.addElement(new Paragraph(cfg.getCidade(), PEQUENO));
        }
        t.addCell(marca);

        PdfPCell direita = semBorda();
        Font fonteTitulo = titulo.length() > 14 ? new Font(NEGRITO, 15, Font.NORMAL, MARROM) : TITULO;
        Paragraph pt = new Paragraph(titulo, fonteTitulo);
        pt.setAlignment(Element.ALIGN_RIGHT);
        direita.addElement(pt);
        if (subtitulo != null) {
            Paragraph ps = new Paragraph(subtitulo, SUBTITULO);
            ps.setAlignment(Element.ALIGN_RIGHT);
            direita.addElement(ps);
        }
        t.addCell(direita);
        adicionar(doc, t);

        PdfPTable linha = new PdfPTable(1);
        linha.setWidthPercentage(100);
        linha.setSpacingBefore(8);
        linha.setSpacingAfter(14);
        PdfPCell c = new PdfPCell();
        c.setFixedHeight(3);
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(TERRA);
        linha.addCell(c);
        adicionar(doc, linha);
    }

    public static void secao(Document doc, String texto) {
        Paragraph p = new Paragraph(texto, SECAO);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        adicionar(doc, p);
    }

    /** Título da seção e tabela juntos: o título nunca fica sozinho no pé da página. */
    public static void secaoComTabela(Document doc, String titulo, PdfPTable tabela) {
        PdfPTable envoltorio = new PdfPTable(1);
        envoltorio.setWidthPercentage(100);
        envoltorio.setSpacingBefore(10);
        envoltorio.setKeepTogether(tabela.getRows().size() <= 30);
        PdfPCell cab = semBorda();
        cab.setPadding(0);
        cab.setPaddingBottom(6);
        cab.addElement(new Paragraph(titulo, SECAO));
        envoltorio.addCell(cab);
        PdfPCell corpo = semBorda();
        corpo.setPadding(0);
        corpo.addElement(tabela);
        envoltorio.addCell(corpo);
        adicionar(doc, envoltorio);
    }

    public static void paragrafo(Document doc, String texto) {
        Paragraph p = new Paragraph(texto, TEXTO);
        p.setLeading(14);
        p.setSpacingAfter(6);
        // à esquerda: o justificado estica as letras quando há e-mail ou chave Pix longa
        p.setAlignment(Element.ALIGN_LEFT);
        adicionar(doc, p);
    }

    /** Tabela de duas colunas "rótulo: valor", sem bordas. */
    public static PdfPTable ficha(String[][] linhas) {
        PdfPTable t = new PdfPTable(new float[]{1f, 2.6f});
        t.setWidthPercentage(100);
        for (String[] l : linhas) {
            if (l[1] == null || l[1].isBlank()) {
                continue;
            }
            PdfPCell r = semBorda();
            r.setPadding(4);
            r.setPhrase(new Phrase(l[0], TABELA_CAB));
            t.addCell(r);
            PdfPCell v = semBorda();
            v.setPadding(4);
            v.setPhrase(new Phrase(l[1], TABELA));
            t.addCell(v);
        }
        return t;
    }

    /** Tabela com cabeçalho; colunas com true em "numericas" ficam alinhadas à direita. */
    public static PdfPTable tabela(String[] cabecalho, float[] larguras, boolean[] numericas) {
        PdfPTable t = new PdfPTable(larguras);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        for (int i = 0; i < cabecalho.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(cabecalho[i].toUpperCase(PT_BR), TABELA_CAB));
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(AREIA);
            c.setBorderWidth(1.2f);
            c.setPaddingBottom(5);
            c.setHorizontalAlignment(numericas[i] ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            t.addCell(c);
        }
        return t;
    }

    public static void linha(PdfPTable t, boolean[] numericas, boolean negrito, String... valores) {
        for (int i = 0; i < valores.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(valores[i] == null ? "—" : valores[i], negrito ? TABELA_FORTE : TABELA));
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(LINHA);
            c.setPaddingTop(4);
            c.setPaddingBottom(5);
            c.setHorizontalAlignment(numericas[i] ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            t.addCell(c);
        }
    }

    /** Caixa de destaque com fundo colorido (valor total, sinal…). */
    public static PdfPTable caixas(Color fundo, String[][] itens) {
        PdfPTable t = new PdfPTable(itens.length);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6);
        t.setSpacingAfter(6);
        for (String[] it : itens) {
            PdfPCell c = new PdfPCell();
            c.setBorder(Rectangle.NO_BORDER);
            c.setBackgroundColor(fundo);
            c.setPadding(10);
            c.addElement(new Paragraph(it[0], TABELA_CAB));
            c.addElement(new Paragraph(it[1], DESTAQUE));
            if (it.length > 2 && it[2] != null) {
                c.addElement(new Paragraph(it[2], PEQUENO));
            }
            t.addCell(c);
        }
        // espaço branco entre as caixas
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        return t;
    }

    public static void assinaturas(Document doc, String esquerda, String direita) {
        PdfPTable t = new PdfPTable(new float[]{1f, 0.2f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(46);
        t.addCell(assinatura(esquerda));
        t.addCell(semBorda());
        t.addCell(assinatura(direita));
        t.setKeepTogether(true);
        adicionar(doc, t);
    }

    private static PdfPCell assinatura(String nome) {
        PdfPCell c = new PdfPCell(new Phrase(nome, TABELA));
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(MARROM);
        c.setPaddingTop(6);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    public static PdfPCell semBorda() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    public static void adicionar(Document doc, Element e) {
        try {
            doc.add(e);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Erro ao montar o PDF", ex);
        }
    }

    // ---------- Marca ----------

    public static Image logoHorizontal() {
        return imagem(LOGO_HORIZONTAL);
    }

    public static Image logoEmpilhada() {
        return imagem(LOGO_EMPILHADA);
    }

    public static Image simbolo() {
        return imagem(LOGO_SIMBOLO);
    }

    private static Image imagem(byte[] bytes) {
        try {
            return Image.getInstance(bytes);
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Logo do ateliê inválida", e);
        }
    }

    private static byte[] recurso(String caminho) {
        try (InputStream in = PdfAtelie.class.getClassLoader().getResourceAsStream(caminho)) {
            if (in == null) {
                throw new IllegalStateException("Arquivo não encontrado: " + caminho);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + caminho, e);
        }
    }

    private static BaseFont fonte(String arquivo) {
        try {
            return BaseFont.createFont(arquivo, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, recurso("fontes/" + arquivo), null);
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Fonte inválida: " + arquivo, e);
        }
    }

    // ---------- Formatação ----------

    public static String moeda(BigDecimal v) {
        return v == null ? "—" : NumberFormat.getCurrencyInstance(PT_BR).format(v);
    }

    public static String numero(BigDecimal v) {
        if (v == null) {
            return "—";
        }
        NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
        f.setMaximumFractionDigits(3);
        return f.format(v);
    }

    /** "144 metros", "1 unidade", "300 g" (abreviações ficam como estão). */
    public static String quantidade(BigDecimal q, String unidade) {
        String u = unidade == null ? "" : unidade;
        boolean plural = q != null && q.abs().compareTo(BigDecimal.ONE) > 0 && u.length() > 2 && u.matches(".*[aeiouAEIOU]$");
        return numero(q) + " " + (plural ? u + "s" : u);
    }

    public static String data(LocalDate d) {
        return d == null ? "—" : DATA.format(d);
    }

    public static String dataExtenso(LocalDate d) {
        return DATA_EXTENSO.format(d);
    }

    public static String telefone(String w) {
        if (w == null) {
            return "—";
        }
        String d = w.replaceAll("\\D", "").replaceFirst("^55", "");
        if (d.length() == 11) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        }
        if (d.length() == 10) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        }
        return w;
    }

    /** Rodapé de todas as páginas: contato à esquerda, número da página à direita. */
    private static final class Rodape extends PdfPageEventHelper {
        private final String texto;

        Rodape(Configuracao cfg) {
            this.texto = cfg.getNomeAtelie() + (cfg.getWhatsapp() != null && !cfg.getWhatsapp().isBlank() ? "  ·  WhatsApp " + telefone(cfg.getWhatsapp()) : "")
                    + (cfg.getInstagram() != null ? "  ·  @" + cfg.getInstagram() : "");
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float y = document.bottom() - 26;
            cb.setColorStroke(LINHA);
            cb.setLineWidth(0.8f);
            cb.moveTo(document.left(), y + 14);
            cb.lineTo(document.right(), y + 14);
            cb.stroke();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(texto, PEQUENO), document.left(), y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase("Página " + writer.getPageNumber(), PEQUENO),
                    document.right(), y, 0);
        }
    }
}
