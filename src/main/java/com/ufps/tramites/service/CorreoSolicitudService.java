package com.ufps.tramites.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;

/**
 * Envía correos transaccionales para los eventos del flujo de solicitudes:
 * nueva solicitud, aprobación, rechazo y confirmación de pago.
 * Todos los métodos son seguros ante fallos: si el envío falla, solo loguean
 * el error sin interrumpir el flujo del trámite.
 */
@Service
public class CorreoSolicitudService {

    private static final Logger log = LoggerFactory.getLogger(CorreoSolicitudService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@ufps.edu.co}")
    private String remitente;

    // ── Correos al director ───────────────────────────────────────────────────

    public void notificarDirectorNuevaSolicitud(Solicitud s, String correoDirector) {
        if (correoDirector == null || correoDirector.isBlank()) return;
        String tipo = labelTipo(s);
        String asunto = "[UFPS Posgrados] Nueva solicitud de " + tipo + " — " + s.getRadicado();
        String cuerpo = String.format(
            "Estimado/a Director/a de Programa,%n%n" +
            "Se ha registrado una nueva solicitud de %s que requiere su revisión.%n%n" +
            "  Radicado    : %s%n" +
            "  Estudiante  : %s%n" +
            "  Fecha       : %s%n%n" +
            "Ingrese al sistema para aprobar o rechazar la solicitud.%n%n" +
            "Atentamente,%n" +
            "Sistema de Trámites de Posgrado — UFPS",
            tipo, s.getRadicado(), s.getCedula(), s.getFechaSolicitud()
        );
        enviar(correoDirector, asunto, cuerpo);
    }

    // ── Correos al estudiante ─────────────────────────────────────────────────

    public void notificarEstudianteAprobacion(Solicitud s, Usuario estudiante) {
        if (estudiante == null || estudiante.getCorreo() == null) return;
        String tipo = labelTipo(s);
        boolean esTerminacion = "TERMINACION_MATERIAS".equals(s.getTipo());
        String siguientePaso = esTerminacion
            ? "Procede al pago de la liquidación desde el sistema para continuar con el proceso."
            : "Tu solicitud de grado fue aprobada. Revisa el estado de los paz y salvos en el sistema.";
        String asunto = "[UFPS Posgrados] Solicitud aprobada — " + s.getRadicado();
        String cuerpo = String.format(
            "Estimado/a %s,%n%n" +
            "Tu solicitud de %s ha sido APROBADA por el director de programa.%n%n" +
            "  Radicado    : %s%n" +
            "  Fecha       : %s%n%n" +
            "%s%n%n" +
            "Atentamente,%n" +
            "Sistema de Trámites de Posgrado — UFPS",
            estudiante.getNombre(), tipo, s.getRadicado(), s.getFechaSolicitud(), siguientePaso
        );
        enviar(estudiante.getCorreo(), asunto, cuerpo);
    }

    public void notificarEstudianteRechazo(Solicitud s, Usuario estudiante) {
        if (estudiante == null || estudiante.getCorreo() == null) return;
        String tipo = labelTipo(s);
        String motivo = s.getObservaciones() != null && !s.getObservaciones().isBlank()
            ? s.getObservaciones() : "Sin motivo especificado";
        String asunto = "[UFPS Posgrados] Solicitud rechazada — " + s.getRadicado();
        String cuerpo = String.format(
            "Estimado/a %s,%n%n" +
            "Tu solicitud de %s ha sido RECHAZADA.%n%n" +
            "  Radicado    : %s%n" +
            "  Motivo      : %s%n%n" +
            "Puedes presentar una nueva solicitud una vez hayas atendido las observaciones.%n%n" +
            "Atentamente,%n" +
            "Sistema de Trámites de Posgrado — UFPS",
            estudiante.getNombre(), tipo, s.getRadicado(), motivo
        );
        enviar(estudiante.getCorreo(), asunto, cuerpo);
    }

    public void notificarEstudiantePagoConfirmado(Solicitud s, Usuario estudiante) {
        if (estudiante == null || estudiante.getCorreo() == null) return;
        String tipo = "TERMINACION_MATERIAS".equals(s.getTipo())
            ? "Terminación de Materias" : "Derechos de Grado";
        String asunto = "[UFPS Posgrados] Pago recibido — " + s.getRadicado();
        String cuerpo = String.format(
            "Estimado/a %s,%n%n" +
            "Hemos recibido tu pago de %s correctamente.%n%n" +
            "  Radicado    : %s%n%n" +
            "Tu solicitud está en revisión. Recibirás una notificación cuando haya una decisión.%n%n" +
            "Atentamente,%n" +
            "Sistema de Trámites de Posgrado — UFPS",
            estudiante.getNombre(), tipo, s.getRadicado()
        );
        enviar(estudiante.getCorreo(), asunto, cuerpo);
    }

    // ── Helper privado ────────────────────────────────────────────────────────

    private String labelTipo(Solicitud s) {
        return "GRADO".equals(s.getTipo()) ? "Grado Académico" : "Terminación de Materias";
    }

    private void enviar(String destino, String asunto, String cuerpo) {
        if (mailSender == null) {
            log.info("[CORREO] Simulado → {} | {}", destino, asunto);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remitente);
            msg.setTo(destino);
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            mailSender.send(msg);
            log.info("[CORREO] Enviado → {}", destino);
        } catch (Exception e) {
            log.error("[CORREO] Error enviando a {}: {}", destino, e.getMessage());
        }
    }
}
