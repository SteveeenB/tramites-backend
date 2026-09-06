package com.ufps.tramites.service;

import com.ufps.tramites.event.SolicitudEstadoCambiadoEvent;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class NotificacionEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private NotificacionService notificacionService;

    @EventListener
    public void onEstadoCambiado(SolicitudEstadoCambiadoEvent evento) {
        Solicitud solicitud = evento.getSolicitud();
        Usuario estudiante = evento.getEstudiante();

        notificacionService.notificarEstudianteCambioEstado(solicitud, estudiante);

        if (!"APROBADA".equals(solicitud.getEstado()) && !"RECHAZADA".equals(solicitud.getEstado())) {
            return;
        }

        String correo = estudiante.getCorreo();
        enviarEmail(correo, construirAsunto(solicitud), construirCuerpo(solicitud, estudiante));
    }

    /** Correo al director cuando un estudiante registra una nueva solicitud (evento 1). */
    public void enviarCorreoNuevaSolicitud(Solicitud solicitud, Usuario director, String nombreEstudiante) {
        if (director == null) return;
        String correo = director.getCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("[NUEVA_SOLICITUD] Director {} sin email registrado, solicitud #{}",
                    director.getCedula(), solicitud.getId());
            return;
        }
        String tipo     = solicitud.getTipo().replace("_", " ");
        String radicado = solicitud.getRadicado() != null ? solicitud.getRadicado() : "#" + solicitud.getId();
        String nombreDir = director.getNombreCompleto() != null ? director.getNombreCompleto() : director.getCedula();
        String asunto   = "[UFPS] Nueva solicitud para revisión - " + tipo;

        String cuerpo = String.format(cargarPlantilla("nueva-solicitud-director.txt"),
                nombreDir,
                tipo,
                radicado,
                nombreEstudiante != null ? nombreEstudiante : solicitud.getCedula(),
                solicitud.getFechaSolicitud());

        enviarEmail(correo, asunto, cuerpo);
    }

    /** Correo al estudiante cuando se confirma el pago de su trámite (evento 4). */
    public void enviarCorreoPagoConfirmado(Solicitud solicitud, Usuario estudiante) {
        if (estudiante == null) return;
        String correo = estudiante.getCorreo();
        String tipo     = solicitud.getTipo().replace("_", " ");
        String radicado = solicitud.getRadicado() != null ? solicitud.getRadicado() : "#" + solicitud.getId();
        String nombre   = estudiante.getNombreCompleto() != null ? estudiante.getNombreCompleto() : estudiante.getCedula();
        String asunto   = "[UFPS] Pago confirmado - " + tipo;

        String cuerpo = String.format(cargarPlantilla("pago-confirmado.txt"),
                nombre,
                tipo,
                radicado,
                java.time.LocalDate.now());

        enviarEmail(correo, asunto, cuerpo);
    }

    /** Envía correo al director cuando una solicitud excede el plazo sin decisión. */
    public void enviarEmailPlazoVencido(Solicitud solicitud, Usuario director, int plazoHoras) {
        if (director == null) return;
        String correo = director.getCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("[PLAZO_VENCIDO] Director {} sin email registrado, solicitud #{}",
                    director.getCedula(), solicitud.getId());
            return;
        }
        String tipo    = solicitud.getTipo().replace("_", " ");
        String asunto  = "[UFPS] Solicitud pendiente de decisión - " + tipo;
        String nombre  = director.getNombreCompleto() != null ? director.getNombreCompleto() : director.getCedula();
        String radicado = solicitud.getRadicado() != null ? solicitud.getRadicado() : "#" + solicitud.getId();

        String plantilla = cargarPlantilla("plazo-vencido.txt");
        String cuerpo = String.format(plantilla,
                nombre,
                radicado,
                tipo,
                plazoHoras,
                solicitud.getFechaEnRevision());

        enviarEmail(correo, asunto, cuerpo);
    }

    private String construirAsunto(Solicitud solicitud) {
        String tipo = solicitud.getTipo().replace("_", " ");
        return "APROBADA".equals(solicitud.getEstado())
                ? "[UFPS] Solicitud APROBADA - " + tipo
                : "[UFPS] Solicitud RECHAZADA - " + tipo;
    }

    private String construirCuerpo(Solicitud solicitud, Usuario estudiante) {
        String nombre   = estudiante.getNombreCompleto() != null ? estudiante.getNombreCompleto() : estudiante.getCedula();
        String radicado = solicitud.getRadicado() != null ? solicitud.getRadicado() : "#" + solicitud.getId();
        String tipo     = solicitud.getTipo().replace("_", " ");

        String mensajeEstado;
        String extra = "";
        if ("APROBADA".equals(solicitud.getEstado())) {
            mensajeEstado = "Nos complace informarle que su solicitud ha sido APROBADA.";
            if ("TERMINACION_MATERIAS".equals(solicitud.getTipo())) {
                extra = "Su certificado de terminación de materias ya está disponible en el panel de estudiante.\n";
            }
        } else {
            mensajeEstado = "Le informamos que su solicitud ha sido RECHAZADA.";
            String motivo = solicitud.getObservaciones();
            if (motivo != null && !motivo.isBlank()) {
                extra = "Motivo del rechazo: " + motivo + "\n";
            }
        }

        String plantilla = cargarPlantilla("estado-cambiado.txt");
        return String.format(plantilla,
                nombre,
                mensajeEstado,
                tipo,
                radicado,
                solicitud.getFechaSolicitud(),
                extra);
    }

    private void enviarEmail(String correo, String asunto, String cuerpo) {
        if (correo == null || correo.isBlank()) {
            log.warn("[CORREO SIN DESTINATARIO] No hay email registrado.\nAsunto: {}\n{}", asunto, cuerpo);
            return;
        }
        if (mailSender != null) {
            try {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setTo(correo);
                mensaje.setSubject(asunto);
                mensaje.setText(cuerpo);
                mailSender.send(mensaje);
                log.info("Correo enviado a {} — Asunto: {}", correo, asunto);
            } catch (Exception e) {
                log.error("[FALLO DE ENVÍO] No se pudo enviar correo a {}. Asunto: {}", correo, asunto, e);
            }
        } else {
            log.info("=== [SIMULACIÓN DE CORREO] ===\nPara: {}\nAsunto: {}\n{}\n==============================",
                    correo, asunto, cuerpo);
        }
    }

    private String cargarPlantilla(String nombre) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/" + nombre);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("No se pudo cargar la plantilla de correo: {}", nombre, e);
            return "%s\n%s\nTipo: %s\nRadicado: %s\nFecha: %s\n%s";
        }
    }
}
