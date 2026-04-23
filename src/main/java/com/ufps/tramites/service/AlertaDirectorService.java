package com.ufps.tramites.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.repository.SolicitudRepository;

@Service
public class AlertaDirectorService {

    // Plazo máximo en horas que tiene el director para decidir.
    // Cuando implementes HU-15 (configuración de parámetros), este valor
    // vendrá de la base de datos en lugar de estar hardcodeado aquí.
    private static final int PLAZO_HORAS = 48;

    private final SolicitudRepository solicitudRepository;
    private final NotificacionService notificacionService;

    public AlertaDirectorService(SolicitudRepository solicitudRepository,
                                  NotificacionService notificacionService) {
        this.solicitudRepository = solicitudRepository;
        this.notificacionService = notificacionService;
    }

    // Se ejecuta cada hora — revisa si algún director venció el plazo
    // Formato cron: segundo minuto hora * * *
    @Scheduled(cron = "0 0 * * * *")
    public void verificarPlazosVencidos() {

        // Traemos solo las solicitudes que están esperando decisión del director
        List<Solicitud> enRevision = solicitudRepository.findByEstado("EN_REVISION");

        LocalDateTime ahora = LocalDateTime.now();

        for (Solicitud solicitud : enRevision) {

            // fechaDecision es null mientras el director no decide.
            // Si ya decidió, este campo tiene valor y no debería estar EN_REVISION,
            // pero lo validamos por seguridad.
            if (solicitud.getFechaDecision() != null) continue;

            // Usamos fechaSolicitud como punto de partida del plazo.
            // Cuando el estado cambia a EN_REVISION en TP-43, ese momento
            // queda registrado en fechaSolicitud o puedes agregar un campo
            // fechaEnRevision — ver nota al final.
            LocalDateTime limite = solicitud.getFechaEnRevision().plusHours(PLAZO_HORAS);

            if (ahora.isAfter(limite)) {
                notificacionService.notificarDirectorPlazoVencido(solicitud);
            }
        }
    }
}