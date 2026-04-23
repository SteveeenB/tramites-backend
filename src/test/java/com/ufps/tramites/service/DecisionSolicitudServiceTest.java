package com.ufps.tramites.service;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.repository.SolicitudRepository;

@ExtendWith(MockitoExtension.class)
class DecisionSolicitudServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @InjectMocks
    private DecisionSolicitudService decisionSolicitudService;

    private Solicitud solicitud;

    @BeforeEach
    void setUp() {
        solicitud = new Solicitud();
        solicitud.setCedula("123456");
        solicitud.setTipo("TERMINACION_MATERIAS");
        solicitud.setEstado("EN_REVISION");
        solicitud.setFechaSolicitud(LocalDate.now());
    }

    @Test
    void cuandoDirectorAprueba_estadoCambiaAAprobada() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any())).thenReturn(solicitud);

        decisionSolicitudService.registrarDecision(1L, "APROBADA", "", "director-cedula");

        assertThat(solicitud.getEstado()).isEqualTo("APROBADA");
        assertThat(solicitud.getDecision()).isEqualTo("APROBADA");
        assertThat(solicitud.getCedulaDirector()).isEqualTo("director-cedula");
        verify(solicitudRepository, times(1)).save(solicitud);
    }

    @Test
    void cuandoDirectorRechaza_estadoCambiaYGuardaObservaciones() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any())).thenReturn(solicitud);

        decisionSolicitudService.registrarDecision(
                1L, "RECHAZADA", "Documentación incompleta", "director-cedula"
        );

        assertThat(solicitud.getEstado()).isEqualTo("RECHAZADA");
        assertThat(solicitud.getDecision()).isEqualTo("RECHAZADA");
        assertThat(solicitud.getObservacionesDirector()).isEqualTo("Documentación incompleta");
        verify(solicitudRepository, times(1)).save(solicitud);
    }
}