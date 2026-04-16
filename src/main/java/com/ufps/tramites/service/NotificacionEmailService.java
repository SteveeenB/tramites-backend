package com.ufps.tramites.service;

import com.ufps.tramites.event.SolicitudEstadoCambiadoEvent;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificacionEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @EventListener
    public void onEstadoCambiado(SolicitudEstadoCambiadoEvent evento) {
        Solicitud solicitud = evento.getSolicitud();
        Usuario estudiante = evento.getEstudiante();

        if (!"APROBADA".equals(solicitud.getEstado()) && !"RECHAZADA".equals(solicitud.getEstado())) {
            return;
        }

        String asunto = construirAsunto(solicitud);
        String cuerpo = construirCuerpo(solicitud, estudiante);
        String correo = estudiante.getCorreo();

        if (correo == null || correo.isBlank()) {
            log.warn("[CORREO SIN DESTINATARIO] El estudiante {} no tiene correo registrado.\n--- Contenido ---\nAsunto: {}\n{}\n---",
                    estudiante.getNombre(), asunto, cuerpo);
            return;
        }

        if (mailSender != null) {
            try {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setTo(correo);
                mensaje.setSubject(asunto);
                mensaje.setText(cuerpo);
                mailSender.send(mensaje);
                log.info("Correo enviado a {} para solicitud #{} ({})", correo, solicitud.getId(), solicitud.getEstado());
            } catch (Exception e) {
                log.error("[FALLO DE ENVÍO] No se pudo enviar correo a {}. Contenido:\nAsunto: {}\n{}", correo, asunto, cuerpo, e);
            }
        } else {
            log.info("=== [SIMULACIÓN DE CORREO] ===\nPara: {}\nAsunto: {}\n{}\n==============================",
                    correo, asunto, cuerpo);
        }
    }

    private String construirAsunto(Solicitud solicitud) {
        String tipo = solicitud.getTipo().replace("_", " ");
        return "APROBADA".equals(solicitud.getEstado())
                ? "[UFPS] Solicitud APROBADA - " + tipo
                : "[UFPS] Solicitud RECHAZADA - " + tipo;
    }

    private String construirCuerpo(Solicitud solicitud, Usuario estudiante) {
        StringBuilder sb = new StringBuilder();
        sb.append("Estimado/a ").append(estudiante.getNombre()).append(",\n\n");

        if ("APROBADA".equals(solicitud.getEstado())) {
            sb.append("Nos complace informarle que su solicitud ha sido APROBADA.\n\n");
            sb.append("Tipo de trámite: ").append(solicitud.getTipo()).append("\n");
            sb.append("Fecha: ").append(solicitud.getFechaSolicitud()).append("\n");
            if ("TERMINACION_MATERIAS".equals(solicitud.getTipo())) {
                sb.append("\nSu certificado de terminación de materias ya está disponible ")
                  .append("en el panel de estudiante. Ingrese al sistema para descargarlo.\n");
            }
        } else {
            sb.append("Le informamos que su solicitud ha sido RECHAZADA.\n\n");
            sb.append("Tipo de trámite: ").append(solicitud.getTipo()).append("\n");
            sb.append("Fecha: ").append(solicitud.getFechaSolicitud()).append("\n");
            String motivo = solicitud.getObservaciones();
            if (motivo != null && !motivo.isBlank()) {
                sb.append("\nMotivo del rechazo:\n").append(motivo).append("\n");
            }
            sb.append("\nPuede presentar una nueva solicitud una vez subsane la situación indicada.\n");
        }

        sb.append("\nAtentamente,\nUniversidad Francisco de Paula Santander (UFPS)\n");
        sb.append("Sistema de Trámites de Posgrado");
        return sb.toString();
    }
}
