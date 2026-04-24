package com.ufps.tramites.service;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaDirectorServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private AlertaDirectorService alertaDirectorService;

    private Solicitud solicitudVencida;
    private Solicitud solicitudEnPlazo;
    private Solicitud solicitudYaDecidida;

    @BeforeEach
    void setUp() {
        // Solicitud que lleva más de 48h sin decisión — debe generar alerta
        solicitudVencida = new Solicitud();
        solicitudVencida.setEstado("EN_REVISION");
        solicitudVencida.setFechaEnRevision(LocalDateTime.now().minusHours(50));

        // Solicitud dentro del plazo — NO debe generar alerta
        solicitudEnPlazo = new Solicitud();
        solicitudEnPlazo.setEstado("EN_REVISION");
        solicitudEnPlazo.setFechaEnRevision(LocalDateTime.now().minusHours(10));

        // Solicitud ya decidida — NO debe generar alerta aunque esté vencida
        solicitudYaDecidida = new Solicitud();
        solicitudYaDecidida.setEstado("EN_REVISION");
        solicitudYaDecidida.setFechaEnRevision(LocalDateTime.now().minusHours(50));
        solicitudYaDecidida.setFechaDecision(LocalDateTime.now());
    }

    @Test
    void cuandoSolicitudVencida_debeNotificar() {
        when(solicitudRepository.findByEstado("EN_REVISION"))
                .thenReturn(List.of(solicitudVencida));

        alertaDirectorService.verificarPlazosVencidos();

        verify(notificacionService, times(1))
                .notificarDirectorPlazoVencido(solicitudVencida);
    }

    @Test
    void cuandoSolicitudDentroDePlazo_noDebeNotificar() {
        when(solicitudRepository.findByEstado("EN_REVISION"))
                .thenReturn(List.of(solicitudEnPlazo));

        alertaDirectorService.verificarPlazosVencidos();

        verify(notificacionService, never())
                .notificarDirectorPlazoVencido(any());
    }

    @Test
    void cuandoDirectorYaDecidio_noDebeNotificarAunqueVencida() {
        when(solicitudRepository.findByEstado("EN_REVISION"))
                .thenReturn(List.of(solicitudYaDecidida));

        alertaDirectorService.verificarPlazosVencidos();

        verify(notificacionService, never())
                .notificarDirectorPlazoVencido(any());
    }
}