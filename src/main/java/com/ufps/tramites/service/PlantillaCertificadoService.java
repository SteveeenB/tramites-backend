package com.ufps.tramites.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class PlantillaCertificadoService {

    public static final List<String> VARIABLES_DISPONIBLES = List.of(
        "nombre_completo",
        "cedula",
        "codigo_estudiantil",
        "programa",
        "tipo_certificado",
        "fecha_expedicion",
        "fecha_aprobacion",
        "numero_solicitud",
        "codigo_verificacion",
        "dependencia"
    );

    public String aplicarVariables(String plantillaHtml, Map<String, String> variables) {
        String resultado = plantillaHtml;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String valor = entry.getValue() != null ? entry.getValue() : "";
            resultado = resultado.replace("{{" + entry.getKey() + "}}", valor);
        }
        return resultado;
    }

    public byte[] renderizarPdf(String htmlProcesado) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlProcesado, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error renderizando plantilla HTML a PDF: " + e.getMessage(), e);
        }
    }
}
