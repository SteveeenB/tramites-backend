package com.ufps.tramites.service;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    // Por ahora imprime en consola — cuando implementes HU-17
    // (motor de notificaciones) reemplazas el log por JavaMailSender
    // sin tocar AlertaDirectorService ni SolicitudService.
    public void notificarDirectorPlazoVencido(Solicitud solicitud) {
        log.warn("[ALERTA] El director {} no ha decidido la solicitud ID {} en el plazo establecido. Tipo: {}",
                solicitud.getCedulaDirector(), solicitud.getId(), solicitud.getTipo());
    }

    /**
     * Notifica al estudiante que su solicitud fue aprobada o rechazada.
     * Cuando se implemente HU-17 este metodo enviara el correo real.
     */
    public void notificarEstudianteCambioEstado(Solicitud solicitud, Usuario estudiante) {
        String estado = solicitud.getEstado();
        String nombre = estudiante.getNombre();
        String tipo   = solicitud.getTipo().replace("_", " ");

        if ("APROBADA".equals(estado)) {
            log.info("=== [NOTIFICACION ESTUDIANTE] ===\nPara: {}\nAsunto: [UFPS] Solicitud APROBADA - {}\n"
                    + "Estimado/a {},\n\nSu solicitud de {} ha sido APROBADA.\n"
                    + "Su certificado de terminacion de materias ya esta disponible para descarga.\n"
                    + "\nAtentamente,\nUFPS - Sistema de Tramites de Posgrado\n=================================",
                    nombre, tipo, nombre, tipo);
        } else if ("RECHAZADA".equals(estado)) {
            String motivo = solicitud.getObservaciones();
            log.info("=== [NOTIFICACION ESTUDIANTE] ===\nPara: {}\nAsunto: [UFPS] Solicitud RECHAZADA - {}\n"
                    + "Estimado/a {},\n\nSu solicitud de {} ha sido RECHAZADA.\n"
                    + "Motivo: {}\n"
                    + "Puede presentar una nueva solicitud una vez subsane la situacion indicada.\n"
                    + "\nAtentamente,\nUFPS - Sistema de Tramites de Posgrado\n=================================",
                    nombre, tipo, nombre, tipo, motivo);
        }
    }
}