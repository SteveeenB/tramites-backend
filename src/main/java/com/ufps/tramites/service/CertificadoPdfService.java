package com.ufps.tramites.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.image.ImageDataFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CertificadoPdfService {

    private static final DeviceRgb COLOR_UFPS  = new DeviceRgb(0, 71, 133);
    private static final DeviceRgb COLOR_FILA  = new DeviceRgb(235, 242, 252);
    private static final DeviceRgb COLOR_BORDE = new DeviceRgb(180, 200, 225);

    /**
     * Genera el PDF del certificado de terminación de materias con QR de verificación.
     *
     * @param nombre           Nombre completo del estudiante
     * @param cedula           Cédula
     * @param codigo           Código estudiantil
     * @param programa         Programa académico
     * @param fechaAprobacion  Fecha de aprobación (texto)
     * @param fechaExpedicion  Fecha de expedición (texto)
     * @param solicitudId      ID de la solicitud (para el QR de verificación)
     */
    public byte[] generar(String nombre, String cedula, String codigo,
                          String programa, String fechaAprobacion,
                          String fechaExpedicion, Long solicitudId) throws IOException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.LETTER);
            doc.setMargins(60, 72, 60, 72);

            agregarEncabezado(doc);
            agregarTitulo(doc);
            agregarCuerpo(doc, fechaAprobacion);
            agregarTabla(doc, nombre, cedula, codigo, programa);
            agregarSeccionVerificacion(doc, solicitudId, cedula, fechaExpedicion);
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

        // Barra dorada
        Table sep = new Table(UnitValue.createPercentArray(new float[]{100}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(0);
        sep.addCell(new Cell().setHeight(3)
                .setBackgroundColor(new DeviceRgb(200, 160, 0))
                .setBorder(Border.NO_BORDER));
        doc.add(sep);
    }

    private void agregarTitulo(Document doc) {
        doc.add(new Paragraph("CERTIFICADO DE TERMINACIÓN DE MATERIAS")
                .setBold().setFontSize(20)
                .setFontColor(COLOR_UFPS)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(24).setMarginBottom(20));
    }

    private void agregarCuerpo(Document doc, String fechaAprobacion) {
        doc.add(new Paragraph(
                "Se certifica que el/la estudiante identificado(a) con los datos relacionados en "
                + "el presente documento ha cumplido satisfactoriamente con todos los requisitos "
                + "académicos establecidos por la institución para la Terminación de Materias del "
                + "programa de posgrado, según resolución aprobada el " + fechaAprobacion
                + ". Este certificado lo habilita para continuar con las siguientes etapas del "
                + "proceso de grado ante la Sección de Posgrados.")
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
                .setMarginBottom(22);

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
                .setBackgroundColor(COLOR_FILA).setBorder(borde).setPadding(7));
        tabla.addCell(new Cell()
                .add(new Paragraph(valor != null ? valor : "—").setFontSize(10))
                .setBorder(borde).setPadding(7));
    }

    private void agregarSeccionVerificacion(Document doc, Long solicitudId,
                                             String cedula, String fechaExpedicion) {
        // Código de verificación legible
        String codigoVerif = "UFPS-TM-" + solicitudId + "-" + cedula.substring(Math.max(0, cedula.length() - 4));
        String urlVerif    = "https://tramites.ufps.edu.co/verificar?codigo=" + codigoVerif;

        // ── Tabla con QR a la izquierda y texto a la derecha ──────────────
        float[] widths = {28f, 72f};
        Table verif = new Table(UnitValue.createPercentArray(widths))
                .setWidth(UnitValue.createPercentValue(88))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginBottom(28);

        // Celda QR
        Cell celdaQr = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
        try {
            byte[] qrBytes = generarQr(urlVerif, 120);
            Image qrImg = new Image(ImageDataFactory.create(qrBytes)).setWidth(80).setHeight(80);
            celdaQr.add(qrImg);
        } catch (Exception e) {
            celdaQr.add(new Paragraph("[QR]").setFontSize(8).setFontColor(ColorConstants.GRAY));
        }
        verif.addCell(celdaQr);

        // Celda texto de verificación
        Cell celdaTexto = new Cell()
                .add(new Paragraph("Verificación de autenticidad").setBold().setFontSize(10)
                        .setFontColor(COLOR_UFPS).setMarginBottom(3))
                .add(new Paragraph("Escanea el código QR o ingresa el siguiente código en el portal "
                        + "de trámites de la UFPS para verificar la autenticidad de este certificado.")
                        .setFontSize(9).setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(5))
                .add(new Paragraph("Código: " + codigoVerif)
                        .setBold().setFontSize(10).setFontColor(COLOR_UFPS))
                .add(new Paragraph("Expedido el: " + fechaExpedicion)
                        .setFontSize(9).setFontColor(ColorConstants.GRAY).setMarginTop(3))
                .setBorder(Border.NO_BORDER)
                .setPadding(4);
        verif.addCell(celdaTexto);
        doc.add(verif);
    }

    private void agregarFirmas(Document doc) {
        float[] widths = {50f, 50f};
        Table firmas = new Table(UnitValue.createPercentArray(widths))
                .setWidth(UnitValue.createPercentValue(88))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        firmas.addCell(celdaFirma("Director(a) de Programa"));
        firmas.addCell(celdaFirma("Coordinador(a) de Posgrados"));
        doc.add(firmas);
    }

    private Cell celdaFirma(String cargo) {
        return new Cell()
                .add(new Paragraph("_______________________________")
                        .setFontSize(10).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph(cargo).setFontSize(9).setItalic()
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Universidad Francisco de Paula Santander")
                        .setFontSize(8).setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.GRAY))
                .setBorder(Border.NO_BORDER).setPaddingTop(6);
    }

    /** Genera los bytes PNG del QR para el texto dado. */
    private byte[] generarQr(String contenido, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new MultiFormatWriter()
                .encode(contenido, BarcodeFormat.QR_CODE, size, size, hints);
        BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", out);
        return out.toByteArray();
    }
}