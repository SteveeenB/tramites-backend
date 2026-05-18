package com.ufps.tramites.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.io.image.ImageDataFactory;
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
import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.model.TipoCertificado;
import com.ufps.tramites.model.Usuario;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Generador de PDF para constancias académicas (HU11).
 *
 * Es independiente del CertificadoPdfService de Terminación de Materias para no
 * mezclar dominios. Se parametriza con TipoCertificado: la diferencia entre un
 * tipo y otro vive en las filas de tipo_certificado, no en código.
 *
 * Punto de extensión para firma digital: el byte[] resultante puede ser firmado
 * antes de almacenarlo. No envolver este servicio: la firma debe aplicarse en
 * el flujo de CertificadoService.generarYNotificar() para mantener este
 * generador puro.
 */
@Service
public class CertificadoConstanciaPdfService {

    private static final DeviceRgb COLOR_UFPS  = new DeviceRgb(0, 71, 133);
    private static final DeviceRgb COLOR_FILA  = new DeviceRgb(235, 242, 252);
    private static final DeviceRgb COLOR_BORDE = new DeviceRgb(180, 200, 225);

    public byte[] generar(TipoCertificado tipo, SolicitudCertificado solicitud, Usuario estudiante) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.LETTER);
            doc.setMargins(60, 72, 60, 72);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "CO"));
            String fechaExpedicion = LocalDate.now().format(fmt);

            agregarEncabezado(doc);
            agregarTitulo(doc, tipo.getLabel());
            agregarCuerpo(doc, tipo, estudiante, fechaExpedicion);
            agregarTablaDatos(doc, estudiante);
            agregarSeccionVerificacion(doc, solicitud, estudiante, fechaExpedicion);
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

        Table sep = new Table(UnitValue.createPercentArray(new float[]{100}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(0);
        sep.addCell(new Cell().setHeight(3)
                .setBackgroundColor(new DeviceRgb(200, 160, 0))
                .setBorder(Border.NO_BORDER));
        doc.add(sep);
    }

    private void agregarTitulo(Document doc, String label) {
        doc.add(new Paragraph(label != null ? label.toUpperCase() : "CONSTANCIA ACADÉMICA")
                .setBold().setFontSize(18)
                .setFontColor(COLOR_UFPS)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(24).setMarginBottom(20));
    }

    private void agregarCuerpo(Document doc, TipoCertificado tipo, Usuario estudiante, String fechaExpedicion) {
        String descripcion = tipo.getDescripcion() != null && !tipo.getDescripcion().isBlank()
                ? tipo.getDescripcion()
                : "Documento oficial emitido por la Universidad Francisco de Paula Santander.";

        String programa = estudiante != null && estudiante.getProgramaAcademico() != null
                ? estudiante.getProgramaAcademico().getNombre()
                : "—";

        String texto = "La Sección de Posgrados de la Universidad Francisco de Paula Santander hace constar que el/la "
                + "estudiante identificado(a) con los datos relacionados a continuación se encuentra registrado(a) "
                + "en el programa de " + programa + ". "
                + descripcion + " "
                + "El presente documento se expide a solicitud del interesado(a) el " + fechaExpedicion + ".";

        doc.add(new Paragraph(texto)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginBottom(18));
    }

    private void agregarTablaDatos(Document doc, Usuario estudiante) {
        float[] widths = {38f, 62f};
        Table tabla = new Table(UnitValue.createPercentArray(widths))
                .setWidth(UnitValue.createPercentValue(88))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginBottom(22);

        String nombre   = estudiante != null ? estudiante.getNombre()  : "—";
        String cedula   = estudiante != null ? estudiante.getCedula()  : "—";
        String codigo   = estudiante != null ? estudiante.getCodigo()  : "—";
        String programa = estudiante != null && estudiante.getProgramaAcademico() != null
                ? estudiante.getProgramaAcademico().getNombre() : "—";

        agregarFila(tabla, "Nombre completo",       nombre);
        agregarFila(tabla, "Cédula de ciudadanía",  cedula);
        agregarFila(tabla, "Código estudiantil",    codigo);
        agregarFila(tabla, "Programa académico",    programa);
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

    private void agregarSeccionVerificacion(Document doc, SolicitudCertificado s, Usuario estudiante, String fechaExpedicion) {
        String cedula = estudiante != null ? estudiante.getCedula() : "0000";
        String codigoVerif = "UFPS-CERT-" + s.getId() + "-"
                + cedula.substring(Math.max(0, cedula.length() - 4));
        String urlVerif = "https://tramites.ufps.edu.co/verificar?codigo=" + codigoVerif;

        float[] widths = {28f, 72f};
        Table verif = new Table(UnitValue.createPercentArray(widths))
                .setWidth(UnitValue.createPercentValue(88))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginBottom(28);

        Cell celdaQr = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
        try {
            byte[] qrBytes = generarQr(urlVerif, 120);
            Image qrImg = new Image(ImageDataFactory.create(qrBytes)).setWidth(80).setHeight(80);
            celdaQr.add(qrImg);
        } catch (Exception e) {
            celdaQr.add(new Paragraph("[QR]").setFontSize(8).setFontColor(ColorConstants.GRAY));
        }
        verif.addCell(celdaQr);

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
        firmas.addCell(celdaFirma("Coordinador(a) de Posgrados"));
        firmas.addCell(celdaFirma("Secretaría Académica"));
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
