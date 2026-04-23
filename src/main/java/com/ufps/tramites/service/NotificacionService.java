package com.ufps.tramites.service;

import com.ufps.tramites.model.Solicitud;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    // Por ahora imprime en consola — cuando implementes HU-17
    // (motor de notificaciones) reemplazas el System.out por
    // JavaMailSender y el envío a la plataforma, sin tocar AlertaDirectorService.
    public void notificarDirectorPlazoVencido(Solicitud solicitud) {
    System.out.println("[ALERTA] El director " +
            solicitud.getCedulaDirector() +
            " no ha decidido la solicitud ID " +
            solicitud.getId() +
            " en el plazo establecido. Tipo: " + solicitud.getTipo());
}
}