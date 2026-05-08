package com.ufps.tramites.service;

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

@Service
public class CorreoCertificadoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoCertificadoService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * Envía el certificado de terminación de materias como adjunto PDF al correo del estudiante.
     */
    public void enviarCertificadoPorCorreo(Usuario estudiante, byte[] pdfBytes, Long solicitudId) {
        String correo = estudiante.getCorreo();
        String nombre = estudiante.getNombre();

        String asunto = "[UFPS Posgrados] Tu certificado de Terminación de Materias";
        String cuerpo = "Estimado/a " + nombre + ",\n\n"
            + "Nos complace informarte que tu solicitud de Terminación de Materias ha sido "
            + "aprobada por la Sección de Posgrados de la Universidad Francisco de Paula Santander.\n\n"
            + "Adjunto a este correo encontrarás tu certificado oficial en formato PDF. "
            + "El documento incluye un código QR y un código de verificación para validar "
            + "su autenticidad ante terceros.\n\n"
            + "Número de solicitud: " + solicitudId + "\n\n"
            + "Este certificado te habilita para continuar con el proceso de grado. "
            + "Si tienes alguna inquietud, comunícate con la Sección de Posgrados.\n\n"
            + "Atentamente,\n"
            + "Sección de Posgrados\n"
            + "Universidad Francisco de Paula Santander (UFPS)\n"
            + "San José de Cúcuta, Colombia";

        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                if (fromEmail != null && !fromEmail.isBlank()) {
                    helper.setFrom(fromEmail);
                }
                helper.setTo(correo);
                helper.setSubject(asunto);
                helper.setText(cuerpo);
                helper.addAttachment(
                    "certificado-terminacion-" + solicitudId + ".pdf",
                    new ByteArrayResource(pdfBytes),
                    "application/pdf"
                );

                mailSender.send(message);
                log.info("[CERTIFICADO] Correo con PDF enviado a {} ({})", nombre, correo);

            } catch (Exception e) {
                log.error("[CERTIFICADO] Error enviando correo a {} ({}): {}", nombre, correo, e.getMessage());
            }
        } else {
            // Sin SMTP: simular en consola
            log.info("=== [SIMULACIÓN - CERTIFICADO PDF] ===\nPara: {} <{}>\nAsunto: {}\n{}\n[Adjunto: certificado-{}.pdf ({} bytes)]\n===",
                nombre, correo, asunto, cuerpo, solicitudId, pdfBytes.length);
        }
    }
}