package com.ufps.tramites.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;

@Service
public class AlertaDirectorService {

    private static final int PLAZO_HORAS = 48;

    private final SolicitudRepository solicitudRepository;
    private final NotificacionService notificacionService;
    private final NotificacionEmailService notificacionEmailService;
    private final UsuarioRepository usuarioRepository;

    public AlertaDirectorService(SolicitudRepository solicitudRepository,
                                  NotificacionService notificacionService,
                                  NotificacionEmailService notificacionEmailService,
                                  UsuarioRepository usuarioRepository) {
        this.solicitudRepository     = solicitudRepository;
        this.notificacionService     = notificacionService;
        this.notificacionEmailService = notificacionEmailService;
        this.usuarioRepository       = usuarioRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void verificarPlazosVencidos() {
        List<Solicitud> enRevision = solicitudRepository.findByEstado("EN_REVISION");
        LocalDateTime ahora = LocalDateTime.now();

        for (Solicitud solicitud : enRevision) {
            if (solicitud.getFechaDecision() != null) continue;
            if (solicitud.getFechaEnRevision() == null) continue;

            LocalDateTime limite = solicitud.getFechaEnRevision().plusHours(PLAZO_HORAS);
            if (!ahora.isAfter(limite)) continue;

            notificacionService.notificarDirectorPlazoVencido(solicitud);

            String cedulaDirector = solicitud.getCedulaDirector();
            if (cedulaDirector != null && !cedulaDirector.isBlank()) {
                usuarioRepository.findByCedula(cedulaDirector).ifPresent(director ->
                    notificacionEmailService.enviarEmailPlazoVencido(solicitud, director, PLAZO_HORAS)
                );
            }
        }
    }
}
