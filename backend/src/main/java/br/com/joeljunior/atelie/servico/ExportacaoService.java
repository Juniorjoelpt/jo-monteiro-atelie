package br.com.joeljunior.atelie.servico;

import br.com.joeljunior.atelie.dominio.Configuracao;
import br.com.joeljunior.atelie.dto.Dtos.*;
import br.com.joeljunior.atelie.servico.pdf.PdfAtelie;
import com.lowagie.text.pdf.PdfPTable;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import static br.com.joeljunior.atelie.servico.pdf.PdfAtelie.*;

/** O mesmo relatório da tela, em Excel (para mexer nos números) ou PDF (para guardar e mostrar). */
@Service
public class ExportacaoService {

    private final ConfiguracaoService configuracao;

    public ExportacaoService(ConfiguracaoService configuracao) {
        this.configuracao = configuracao;
    }

    public static String nomeBase(Relatorio r) {
        return "Relatorio-" + r.de() + "-a-" + r.ate();
    }

    // ===================== Excel =====================

    public byte[] excel(Relatorio r) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos e = new Estilos(wb);

            Sheet s = wb.createSheet("Resumo");
            larguras(s, 34, 18, 18, 14);
            int l = 0;
            xTexto(s, l++, 0, configuracao.atual().getNomeAtelie() + " — Relatório", e.titulo);
            xTexto(s, l++, 0, "Período: " + PdfAtelie.data(r.de()) + " a " + PdfAtelie.data(r.ate())
                    + "   (comparado com " + PdfAtelie.data(r.deAnterior()) + " a " + PdfAtelie.data(r.ateAnterior()) + ")", e.texto);
            l++;
            xCabecalho(s, l++, e, "Indicador", "Período", "Anterior", "Variação");
            for (Object[] linha : new Object[][]{
                    {"Entradas", r.entradas()}, {"Saídas", r.saidas()}, {"Resultado (entradas − saídas)", r.resultado()}}) {
                Comparativo c = (Comparativo) linha[1];
                Row row = s.createRow(l++);
                row.createCell(0).setCellValue((String) linha[0]);
                xMoeda(row, 1, c.atual(), e);
                xMoeda(row, 2, c.anterior(), e);
                if (c.variacaoPercentual() != null) {
                    Cell cv = row.createCell(3);
                    cv.setCellValue(c.variacaoPercentual() / 100.0);
                    cv.setCellStyle(e.percentual);
                }
            }
            l++;
            Row r1 = s.createRow(l++);
            r1.createCell(0).setCellValue("Encomendas entregues");
            r1.createCell(1).setCellValue(r.encomendasEntregues());
            Row r2 = s.createRow(l++);
            r2.createCell(0).setCellValue("Valor médio por encomenda");
            xMoeda(r2, 1, r.ticketMedio(), e);

            Sheet mes = wb.createSheet("Por mês");
            larguras(mes, 12, 16, 16, 16);
            xCabecalho(mes, 0, e, "Mês", "Entradas", "Saídas", "Resultado");
            int i = 1;
            for (MesValores m : r.evolucao()) {
                Row row = mes.createRow(i++);
                row.createCell(0).setCellValue(m.mes());
                xMoeda(row, 1, m.entradas(), e);
                xMoeda(row, 2, m.saidas(), e);
                xMoeda(row, 3, m.entradas().subtract(m.saidas()), e);
            }

            Sheet fr = wb.createSheet("Por frente");
            larguras(fr, 22, 16, 10);
            xCabecalho(fr, 0, e, "Frente", "Entradas", "% do total");
            i = 1;
            for (ValorPorNome v : r.porFrente()) {
                Row row = fr.createRow(i++);
                row.createCell(0).setCellValue(v.nome());
                xMoeda(row, 1, v.valor(), e);
                Cell c = row.createCell(2);
                c.setCellValue(v.percentual() / 100.0);
                c.setCellStyle(e.percentual);
            }

            Sheet pc = wb.createSheet("Encomendas por peça");
            larguras(pc, 30, 8, 16, 16, 16, 10, 16);
            xCabecalho(pc, 0, e, "Peça", "Peças", "Faturamento", "Materiais", "Seu ganho", "Horas", "Ganho por hora");
            i = 1;
            for (LinhaPeca p : r.porPeca()) {
                Row row = pc.createRow(i++);
                row.createCell(0).setCellValue(p.nome());
                row.createCell(1).setCellValue(p.pecas());
                xMoeda(row, 2, p.faturamento(), e);
                xMoeda(row, 3, p.materiais(), e);
                xMoeda(row, 4, p.ganho(), e);
                xNumero(row, 5, p.horas(), e);
                xMoeda(row, 6, p.ganhoPorHora(), e);
            }

            Sheet pe = wb.createSheet("Pronta-entrega");
            larguras(pe, 30, 8, 16, 16, 16);
            xCabecalho(pe, 0, e, "Peça", "Peças", "Vendido", "Custo dos materiais", "Sobra");
            i = 1;
            for (LinhaVenda v : r.prontaEntrega()) {
                Row row = pe.createRow(i++);
                row.createCell(0).setCellValue(v.nome());
                row.createCell(1).setCellValue(v.pecas());
                xMoeda(row, 2, v.faturamento(), e);
                xMoeda(row, 3, v.custo(), e);
                xMoeda(row, 4, v.sobra(), e);
            }
            if (r.descontosVendas().signum() > 0) {
                Row row = pe.createRow(++i);
                row.createCell(0).setCellValue("Descontos concedidos nas vendas");
                xMoeda(row, 2, r.descontosVendas().negate(), e);
            }

            Sheet mv = wb.createSheet("Mais vendidos");
            larguras(mv, 30, 10, 14, 12, 16);
            xCabecalho(mv, 0, e, "Peça", "Peças", "Em encomendas", "Em vendas", "Faturamento");
            i = 1;
            for (LinhaRanking v : r.maisVendidos()) {
                Row row = mv.createRow(i++);
                row.createCell(0).setCellValue(v.nome());
                row.createCell(1).setCellValue(v.pecas());
                row.createCell(2).setCellValue(v.emEncomendas());
                row.createCell(3).setCellValue(v.emVendas());
                xMoeda(row, 4, v.faturamento(), e);
            }

            Sheet mt = wb.createSheet("Materiais");
            larguras(mt, 30, 14, 12, 16);
            xCabecalho(mt, 0, e, "Material", "Quantidade", "Unidade", "Custo");
            i = 1;
            for (LinhaMaterial m : r.materiais()) {
                Row row = mt.createRow(i++);
                row.createCell(0).setCellValue(m.nome());
                xNumero(row, 1, m.quantidade(), e);
                row.createCell(2).setCellValue(m.unidade());
                xMoeda(row, 3, m.custo(), e);
            }

            Sheet lc = wb.createSheet("Lançamentos");
            larguras(lc, 12, 10, 18, 46, 12, 16);
            xCabecalho(lc, 0, e, "Data", "Tipo", "Categoria", "Descrição", "Forma", "Valor");
            i = 1;
            for (LancamentoResp x : r.lancamentos()) {
                Row row = lc.createRow(i++);
                Cell cd = row.createCell(0);
                cd.setCellValue(x.data());
                cd.setCellStyle(e.data);
                row.createCell(1).setCellValue(x.tipo().name().equals("ENTRADA") ? "Entrada" : "Saída");
                row.createCell(2).setCellValue(FinanceiroService.nome(x.categoria()));
                row.createCell(3).setCellValue(x.descricao());
                row.createCell(4).setCellValue(x.formaPagamento().name());
                xMoeda(row, 5, x.tipo().name().equals("ENTRADA") ? x.valor() : x.valor().negate(), e);
            }
            lc.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, i - 1), 0, 5));
            lc.createFreezePane(0, 1);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível gerar a planilha", ex);
        }
    }

    private static final class Estilos {
        final CellStyle titulo, texto, cab, moeda, percentual, numero, data;

        Estilos(XSSFWorkbook wb) {
            DataFormat df = wb.createDataFormat();
            Font fTitulo = wb.createFont();
            fTitulo.setBold(true);
            fTitulo.setFontHeightInPoints((short) 14);
            titulo = wb.createCellStyle();
            titulo.setFont(fTitulo);
            texto = wb.createCellStyle();

            Font fCab = wb.createFont();
            fCab.setBold(true);
            XSSFCellStyle c = wb.createCellStyle();
            c.setFont(fCab);
            c.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xF0, (byte) 0xDC, (byte) 0xCA}, null));
            c.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            c.setBorderBottom(BorderStyle.THIN);
            cab = c;

            moeda = wb.createCellStyle();
            moeda.setDataFormat(df.getFormat("\"R$\" #,##0.00;[Red]-\"R$\" #,##0.00"));
            percentual = wb.createCellStyle();
            percentual.setDataFormat(df.getFormat("0%"));
            numero = wb.createCellStyle();
            numero.setDataFormat(df.getFormat("#,##0.###"));
            data = wb.createCellStyle();
            data.setDataFormat(df.getFormat("dd/mm/yyyy"));
        }
    }

    private static void larguras(Sheet s, int... caracteres) {
        for (int i = 0; i < caracteres.length; i++) {
            s.setColumnWidth(i, caracteres[i] * 256);
        }
    }

    private static void xTexto(Sheet s, int linha, int col, String valor, CellStyle estilo) {
        Row r = s.getRow(linha) != null ? s.getRow(linha) : s.createRow(linha);
        Cell c = r.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private static void xCabecalho(Sheet s, int linha, Estilos e, String... titulos) {
        Row r = s.createRow(linha);
        for (int i = 0; i < titulos.length; i++) {
            Cell c = r.createCell(i);
            c.setCellValue(titulos[i]);
            c.setCellStyle(e.cab);
        }
    }

    private static void xMoeda(Row r, int col, BigDecimal v, Estilos e) {
        if (v == null) {
            return;
        }
        Cell c = r.createCell(col);
        c.setCellValue(v.doubleValue());
        c.setCellStyle(e.moeda);
    }

    private static void xNumero(Row r, int col, BigDecimal v, Estilos e) {
        if (v == null) {
            return;
        }
        Cell c = r.createCell(col);
        c.setCellValue(v.doubleValue());
        c.setCellStyle(e.numero);
    }

    // ===================== PDF =====================

    public byte[] pdf(Relatorio r) {
        Configuracao cfg = configuracao.atual();
        return PdfAtelie.gerar(cfg, "Relatório " + data(r.de()) + " a " + data(r.ate()), doc -> {
            cabecalho(doc, cfg, "Relatório", data(r.de()) + " a " + data(r.ate()));

            adicionar(doc, caixas(OLIVA_CLARO, new String[][]{
                    {"ENTRADAS", moeda(r.entradas().atual()), variacao(r.entradas())},
                    {"SAÍDAS", moeda(r.saidas().atual()), variacao(r.saidas())},
                    {"RESULTADO", moeda(r.resultado().atual()), variacao(r.resultado())}}));
            paragrafo(doc, "Comparado com " + data(r.deAnterior()) + " a " + data(r.ateAnterior()) + ". "
                    + r.encomendasEntregues() + (r.encomendasEntregues() == 1 ? " encomenda entregue" : " encomendas entregues")
                    + (r.encomendasEntregues() > 0 ? " (valor médio " + moeda(r.ticketMedio()) + ")" : "") + ".");

                        boolean[] n4 = {false, true, true, true};
            PdfPTable tm = tabela(new String[]{"Mês", "Entradas", "Saídas", "Resultado"}, new float[]{1, 1.3f, 1.3f, 1.3f}, n4);
            for (MesValores m : r.evolucao()) {
                linha(tm, n4, false, m.mes(), moeda(m.entradas()), moeda(m.saidas()), moeda(m.entradas().subtract(m.saidas())));
            }
            secaoComTabela(doc, "Entradas e saídas nos últimos 12 meses", tm);

            if (!r.porFrente().isEmpty()) {
                String tituloSecao = "Entradas por frente";
                boolean[] n3 = {false, true, true};
                PdfPTable t = tabela(new String[]{"Frente", "Entradas", "% do total"}, new float[]{2, 1.3f, 1}, n3);
                r.porFrente().forEach(v -> linha(t, n3, false, v.nome(), moeda(v.valor()), v.percentual() + "%"));
                secaoComTabela(doc, tituloSecao, t);
            }

            if (!r.porPeca().isEmpty()) {
                String tituloSecao = "Encomendas entregues por peça";
                boolean[] n6 = {false, true, true, true, true, true};
                PdfPTable t = tabela(new String[]{"Peça", "Peças", "Faturamento", "Materiais", "Seu ganho", "Por hora"},
                        new float[]{2.4f, 0.7f, 1.3f, 1.2f, 1.2f, 1.1f}, n6);
                r.porPeca().forEach(p -> linha(t, n6, false, p.nome(), String.valueOf(p.pecas()), moeda(p.faturamento()),
                        moeda(p.materiais()), moeda(p.ganho()), moeda(p.ganhoPorHora())));
                secaoComTabela(doc, tituloSecao, t);
            }

            if (!r.prontaEntrega().isEmpty()) {
                String tituloSecao = "Pronta-entrega";
                boolean[] n5 = {false, true, true, true, true};
                PdfPTable t = tabela(new String[]{"Peça", "Peças", "Vendido", "Materiais", "Sobra"},
                        new float[]{2.4f, 0.7f, 1.3f, 1.2f, 1.2f}, n5);
                r.prontaEntrega().forEach(v -> linha(t, n5, false, v.nome(), String.valueOf(v.pecas()), moeda(v.faturamento()),
                        moeda(v.custo()), moeda(v.sobra())));
                if (r.descontosVendas().signum() > 0) {
                    linha(t, n5, true, "Descontos concedidos", "", moeda(r.descontosVendas().negate()), "", "");
                }
                secaoComTabela(doc, tituloSecao, t);
            }

            if (!r.maisVendidos().isEmpty()) {
                String tituloSecao = "Mais vendidos";
                boolean[] n5 = {false, true, true, true, true};
                PdfPTable t = tabela(new String[]{"Peça", "Peças", "Encomendas", "Vendas", "Faturamento"},
                        new float[]{2.4f, 0.8f, 1, 0.8f, 1.3f}, n5);
                r.maisVendidos().forEach(v -> linha(t, n5, false, v.nome(), String.valueOf(v.pecas()),
                        String.valueOf(v.emEncomendas()), String.valueOf(v.emVendas()), moeda(v.faturamento())));
                secaoComTabela(doc, tituloSecao, t);
            }

            if (!r.materiais().isEmpty()) {
                String tituloSecao = "Materiais mais usados";
                boolean[] n3 = {false, true, true};
                PdfPTable t = tabela(new String[]{"Material", "Quantidade", "Custo"}, new float[]{2.4f, 1.3f, 1.2f}, n3);
                r.materiais().forEach(m -> linha(t, n3, false, m.nome(), PdfAtelie.quantidade(m.quantidade(), m.unidade()),
                        moeda(m.custo())));
                secaoComTabela(doc, tituloSecao, t);
            }
        });
    }

    private static String variacao(Comparativo c) {
        if (c.variacaoPercentual() == null) {
            return "sem base de comparação";
        }
        int v = c.variacaoPercentual();
        return (v > 0 ? "+" : "") + v + "% vs. período anterior";
    }
}
