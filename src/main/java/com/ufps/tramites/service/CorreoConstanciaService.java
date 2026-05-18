package com.ufps.tramites.service;

import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.model.TipoCertificado;
import com.ufps.tramites.model.Usuario;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envío del PDF de la constancia al correo del estudiante.
 *
 * Independiente de CorreoCertificadoService (Terminación de Materias) para que
 * el copy y los adjuntos respondan al dominio de HU11, sin acoplar dominios.
 */
@Service
public class CorreoConstanciaService {

    private static final Logger log = LoggerFactory.getLogger(CorreoConstanciaService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void enviarConstancia(Usuario estudiante, TipoCertificado tipo, SolicitudCertificado solicitud, byte[] pdfBytes) {
        String correo = estudiante != null ? estudiante.getCorreo() : null;
        String nombre = estudiante != null ? estudiante.getNombre() : "Estudiante";
        String labelTipo = tipo != null ? tipo.getLabel() : solicitud.getTipoCertificado();

        if (correo == null || correo.isBlank()) {
            log.warn("[CONSTANCIA] Estudiante {} no tiene correo registrado; no se envía PDF de solicitud {}.",
                    nombre, solicitud.getId());
            return;
        }

        boolean esFisica = "FISICA".equals(solicitud.getModalidadEnvio());

        String asunto = "[UFPS Posgrados] Tu certificado: " + labelTipo;
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Estimado/a ").append(nombre).append(",\n\n");
        cuerpo.append("Confirmamos la generación de tu certificado: ").append(labelTipo).append(".\n");
        cuerpo.append("Número de solicitud: ").append(solicitud.getId()).append("\n\n");
        cuerpo.append("Encontrarás el documento adjunto a este correo. El PDF tiene un código QR y un código ")
              .append("legible para que cualquier entidad pueda verificar su autenticidad.\n\n");
        if (esFisica && tipo != null) {
            cuerpo.append("Como solicitaste la modalidad FÍSICA, además del PDF digital adjunto, "
                       + "puedes pasar a recoger la copia impresa con sello institucional en:\n");
            cuerpo.append("  • ").append(tipo.getDireccionOficina() != null ? tipo.getDireccionOficina() : "Oficina de la dependencia encargada").append("\n");
            if (tipo.getTiempoEntregaDias() != null && tipo.getTiempoEntregaDias() > 0) {
                cuerpo.append("  • Tiempo estimado de impresión: ").append(tipo.getTiempoEntregaDias()).append(" día(s) hábil(es)\n");
            }
            cuerpo.append("Recibirás un correo adicional cuando el documento físico esté listo para retiro.\n\n");
        }
        cuerpo.append("Atentamente,\n")
              .append("Sección de Posgrados\n")
              .append("Universidad Francisco de Paula Santander");

        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                if (fromEmail != null && !fromEmail.isBlank()) helper.setFrom(fromEmail);
                helper.setTo(correo);
                helper.setSubject(asunto);
                helper.setText(cuerpo.toString());
                helper.addAttachment(
                    "constancia-" + solicitud.getId() + ".pdf",
                    new ByteArrayResource(pdfBytes),
                    "application/pdf"
                );
                mailSender.send(message);
                log.info("[CONSTANCIA] PDF enviado a {} <{}> — solicitud {}", nombre, correo, solicitud.getId());
            } catch (Exception e) {
                log.error("[CONSTANCIA] Error enviando correo a {} <{}>: {}", nombre, correo, e.getMessage());
            }
        } else {
            log.info("=== [SIMULACIÓN CONSTANCIA] ===\nPara: {} <{}>\nAsunto: {}\n{}\n[Adjunto: constancia-{}.pdf ({} bytes)]",
                    nombre, correo, asunto, cuerpo, solicitud.getId(), pdfBytes.length);
        }
    }

    public void enviarAvisoListoRetiro(Usuario estudiante, TipoCertificado tipo, SolicitudCertificado solicitud) {
        String correo = estudiante != null ? estudiante.getCorreo() : null;
        String nombre = estudiante != null ? estudiante.getNombre() : "Estudiante";
        if (correo == null || correo.isBlank()) return;

        String labelTipo = tipo != null ? tipo.getLabel() : solicitud.getTipoCertificado();
        String oficina   = tipo != null && tipo.getDireccionOficina() != null
                ? tipo.getDireccionOficina() : "la oficina de la dependencia encargada";

        String asunto = "[UFPS Posgrados] Tu certificado físico está listo para retiro";
        String cuerpo = "Estimado/a " + nombre + ",\n\n"
                + "Tu certificado " + labelTipo + " (solicitud " + solicitud.getId() + ") "
                + "está listo para que pases a recogerlo en:\n  • " + oficina + "\n\n"
                + "Recuerda llevar tu documento de identidad.\n\n"
                + "Atentamente,\nSección de Posgrados — UFPS";

        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                if (fromEmail != null && !fromEmail.isBlank()) helper.setFrom(fromEmail);
                helper.setTo(correo);
                helper.setSubject(asunto);
                helper.setText(cuerpo);
                mailSender.send(message);
                log.info("[CONSTANCIA] Aviso de retiro enviado a {} <{}>", nombre, correo);
            } catch (Exception e) {
                log.error("[CONSTANCIA] Error enviando aviso de retiro: {}", e.getMessage());
            }
        } else {
            log.info("=== [SIMULACIÓN AVISO RETIRO] === Para: {} <{}>\n{}", nombre, correo, cuerpo);
        }
    }
}
