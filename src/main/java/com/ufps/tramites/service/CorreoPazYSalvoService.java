package com.ufps.tramites.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Correos de paz y salvo. Es un bean aparte y @Async a propósito: antes
 * PazYSalvoService.iniciarProcesoPazYSalvo enviaba cada correo de forma
 * síncrona DENTRO de la transacción de aprobación del grado. Con SMTP bloqueado
 * (Render free) cada envío esperaba su timeout mientras la transacción seguía
 * abierta, reteniendo bloqueos de solicitud/estudiante y una conexión de BD:
 * la aprobación tardaba minutos, la bandeja no reflejaba el cambio hasta el
 * commit y un segundo clic provocaba deadlocks.
 */
@Service
public class CorreoPazYSalvoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoPazYSalvoService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    // Remitente configurado en application.properties (spring.mail.username)
    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Async
    public void enviar(String correo, String nombreDependencia,
                                        String nombreEstudiante, Long solicitudId) {
        String asunto = "[UFPS Posgrados] Verificación de Paz y Salvo requerida";
        String cuerpo = "Estimado/a " + nombreDependencia + ",\n\n"
            + "El/la estudiante " + nombreEstudiante + " ha solicitado su grado académico "
            + "y requiere verificación de paz y salvo con su dependencia.\n\n"
            + "Por favor ingrese al sistema y confirme si el estudiante se encuentra a paz y salvo.\n"
            + "Solicitud N°: " + solicitudId + "\n\n"
            + "Atentamente,\nUniversidad Francisco de Paula Santander (UFPS)\n"
            + "Sistema de Trámites de Posgrado";

        if (correo == null || correo.isBlank()) {
            log.warn("[PAZ Y SALVO - SIN CORREO] Destinatario '{}' sin correo registrado.\nAsunto: {}\n{}",
                    nombreDependencia, asunto, cuerpo);
            return;
        }

        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                if (fromEmail != null && !fromEmail.isBlank()) msg.setFrom(fromEmail);
                msg.setTo(correo);
                msg.setSubject(asunto);
                msg.setText(cuerpo);
                mailSender.send(msg);
                log.info("[PAZ Y SALVO] Correo enviado a {} ({})", nombreDependencia, correo);
            } catch (Exception e) {
                log.error("[PAZ Y SALVO] Error enviando correo a {} ({}): {}",
                        nombreDependencia, correo, e.getMessage());
            }
        } else {
            log.info("=== [SIMULACIÓN CORREO PAZ Y SALVO] ===\nPara: {} <{}>\nAsunto: {}\n{}\n===",
                    nombreDependencia, correo, asunto, cuerpo);
        }
    }
}
