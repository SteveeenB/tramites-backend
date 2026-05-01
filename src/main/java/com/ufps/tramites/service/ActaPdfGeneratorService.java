package com.ufps.tramites.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.springframework.stereotype.Service;

/**
 * Genera el PDF del acta de grado sin plantilla institucional.
 * Para añadir plantilla en el futuro: reemplazar los métodos agregarEncabezado
 * y agregarFirmas con la lógica de superposición de imagen/plantilla.
 */
@Service
public class ActaPdfGeneratorService {

    private static final DeviceRgb COLOR_UFPS    = new DeviceRgb(0, 71, 133);
    private static final DeviceRgb COLOR_FILA    = new DeviceRgb(235, 242, 252);
    private static final DeviceRgb COLOR_BORDE   = new DeviceRgb(180, 200, 225);

    public byte[] generar(String nombre, String cedula, String codigo,
                          String programa, String fechaAprobacion,
                          String fechaExpedicion) throws IOException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.LETTER);
            doc.setMargins(60, 72, 60, 72);

            agregarEncabezado(doc);
            agregarTitulo(doc);
            agregarCuerpo(doc, fechaAprobacion);
            agregarTabla(doc, nombre, cedula, codigo, programa);
            agregarLugarFecha(doc, fechaExpedicion);
            agregarFirmas(doc);

            doc.close();
            return baos.toByteArray();
        }
    }

    private void agregarEncabezado(Document doc) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{100}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell celda = new Cell()
                .add(new Paragraph("UNIVERSIDAD FRANCISCO DE PAULA SANTANDER")
                        .setFontColor(ColorConstants.WHITE).setBold().setFontSize(14)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2))
                .add(new Paragraph("Vicerrectoría Académica · Sección de Posgrados")
                        .setFontColor(ColorConstants.WHITE).setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(COLOR_UFPS)
                .setBorder(Border.NO_BORDER)
                .setPadding(14);

        header.addCell(celda);
        doc.add(header);
    }

    private void agregarTitulo(Document doc) {
        // Barra separadora delgada
        Table sep = new Table(UnitValue.createPercentArray(new float[]{100}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(0);
        sep.addCell(new Cell().setHeight(3)
                .setBackgroundColor(new DeviceRgb(200, 160, 0))
                .setBorder(Border.NO_BORDER));
        doc.add(sep);

        doc.add(new Paragraph("ACTA DE GRADO")
                .setBold().setFontSize(22)
                .setFontColor(COLOR_UFPS)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(24).setMarginBottom(20));
    }

    private void agregarCuerpo(Document doc, String fechaAprobacion) {
        doc.add(new Paragraph(
                "Se certifica que el/la candidato(a) a grado, identificado(a) con los datos "
                + "relacionados en el presente documento, ha cumplido satisfactoriamente con todos "
                + "los requisitos académicos y administrativos establecidos por la institución para "
                + "optar al título de grado, según resolución aprobada el " + fechaAprobacion + ".")
                .setFontSize(11)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginBottom(18));
    }

    private void agregarTabla(Document doc, String nombre, String cedula,
                               String codigo, String programa) {
        float[] widths = {38f, 62f};
        Table tabla = new Table(UnitValue.createPercentArray(widths))
                .setWidth(UnitValue.createPercentValue(88))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginBottom(24);

        agregarFila(tabla, "Nombre completo", nombre);
        agregarFila(tabla, "Cédula de ciudadanía", cedula);
        agregarFila(tabla, "Código estudiantil", codigo);
        agregarFila(tabla, "Programa académico", programa);

        doc.add(tabla);
    }

    private void agregarFila(Table tabla, String etiqueta, String valor) {
        SolidBorder borde = new SolidBorder(COLOR_BORDE, 0.5f);

        tabla.addCell(new Cell()
                .add(new Paragraph(etiqueta).setBold().setFontSize(10))
                .setBackgroundColor(COLOR_FILA)
                .setBorder(borde)
                .setPadding(7));

        tabla.addCell(new Cell()
                .add(new Paragraph(valor != null ? valor : "—").setFontSize(10))
                .setBorder(borde)
                .setPadding(7));
    }

    private void agregarLugarFecha(Document doc, String fechaExpedicion) {
        doc.add(new Paragraph("Expedido en San José de Cúcuta el " + fechaExpedicion + ".")
                .setFontSize(10).setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4).setMarginBottom(44));
    }

    private void agregarFirmas(Document doc) {
        float[] widths = {50f, 50f};
        Table firmas = new Table(UnitValue.createPercentArray(widths))
                .setWidth(UnitValue.createPercentValue(88))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        firmas.addCell(celdaFirma("Director(a) de Programa"));
        firmas.addCell(celdaFirma("Vicerrector(a) Académico(a)"));

        doc.add(firmas);
    }

    private Cell celdaFirma(String cargo) {
        return new Cell()
                .add(new Paragraph("_______________________________")
                        .setFontSize(10).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph(cargo)
                        .setFontSize(9).setItalic().setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Universidad Francisco de Paula Santander")
                        .setFontSize(8).setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.GRAY))
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(6);
    }
}
