package com.ufps.tramites.service;

import com.ufps.tramites.dto.NotificacionDTO;
import com.ufps.tramites.model.Notificacion;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.NotificacionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private NotificacionSseService sseService;

    /** Crea y persiste una notificacion, luego la emite por SSE si el usuario está conectado. */
    public Notificacion crear(String destinatarioCedula, String tipo,
                              String titulo, String mensaje, String enlace) {
        Notificacion n = new Notificacion();
        n.setDestinatarioCedula(destinatarioCedula);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setEnlace(enlace);
        n.setLeida(false);
        n.setFechaCreacion(LocalDateTime.now());
        Notificacion guardada = notificacionRepository.save(n);
        sseService.emitirNotificacion(destinatarioCedula, toDTO(guardada));
        return guardada;
    }

    public List<NotificacionDTO> listarPorDestinatario(String destinatarioCedula) {
        return notificacionRepository
                .findByDestinatarioCedulaOrderByFechaCreacionDesc(destinatarioCedula)
                .stream().map(this::toDTO).toList();
    }

    public void marcarLeida(Long id, String destinatarioCedula) {
        Notificacion n = notificacionRepository
                .findByIdAndDestinatarioCedula(id, destinatarioCedula)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        n.setLeida(true);
        notificacionRepository.save(n);
    }

    public long contarNoLeidas(String destinatarioCedula) {
        return notificacionRepository.countByDestinatarioCedulaAndLeidaFalse(destinatarioCedula);
    }

    /** Llamado por AlertaDirectorService cuando un director excede el plazo. */
    public void notificarDirectorPlazoVencido(Solicitud solicitud) {
        String cedula = solicitud.getCedulaDirector();
        if (cedula == null || cedula.isBlank()) return;
        crear(cedula, "PLAZO_VENCIDO",
                "Solicitud pendiente de decisión",
                "La solicitud #" + solicitud.getId() + " lleva más de 48 horas sin decisión. Tipo: "
                        + solicitud.getTipo().replace("_", " "),
                "/tramites/bandeja-director");
    }

    /** Llamado por NotificacionEmailService en el @EventListener de SolicitudEstadoCambiadoEvent. */
    public void notificarEstudianteCambioEstado(Solicitud solicitud, Usuario estudiante) {
        String estado = solicitud.getEstado();
        String tipo   = solicitud.getTipo().replace("_", " ");
        String cedula = estudiante.getCedula();
        if (cedula == null) return;

        String titulo, mensaje;
        if ("APROBADA".equals(estado)) {
            titulo  = "Solicitud aprobada";
            mensaje = "Tu solicitud de " + tipo + " ha sido APROBADA.";
        } else if ("RECHAZADA".equals(estado)) {
            String motivo = solicitud.getObservaciones();
            titulo  = "Solicitud rechazada";
            mensaje = "Tu solicitud de " + tipo + " fue RECHAZADA."
                    + (motivo != null && !motivo.isBlank() ? " Motivo: " + motivo : "");
        } else {
            return;
        }

        crear(cedula, "ESTADO_CAMBIADO", titulo, mensaje, "/proceso-de-grado");
    }

    private NotificacionDTO toDTO(Notificacion n) {
        return new NotificacionDTO(n.getId(), n.getTipo(), n.getTitulo(),
                n.getMensaje(), n.getEnlace(), n.isLeida(), n.getFechaCreacion());
    }
}
