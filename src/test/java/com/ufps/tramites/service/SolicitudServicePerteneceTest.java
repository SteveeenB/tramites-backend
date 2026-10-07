package com.ufps.tramites.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.repository.SolicitudRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// FIX TP-186 (Santiago Cepeda, 07/10/2026): cobertura de perteneceAEstudiante,
// la pieza que WompiController usa para prevenir IDOR al crear un pago.
@ExtendWith(MockitoExtension.class)
class SolicitudServicePerteneceTest {

    @Mock private SolicitudRepository solicitudRepository;
    @InjectMocks private SolicitudService solicitudService;

    private Solicitud solicitudCon(String cedula) {
        Solicitud s = new Solicitud();
        s.setCedula(cedula);
        return s;
    }

    @Test
    void cuandoLaSolicitudEsDelMismoEstudiante_devuelveTrue() {
        when(solicitudRepository.findById(42L)).thenReturn(Optional.of(solicitudCon("1111")));

        assertThat(solicitudService.perteneceAEstudiante(42L, "1111")).isTrue();
    }

    @Test
    void cuandoLaSolicitudEsDeOtroEstudiante_devuelveFalse() {
        when(solicitudRepository.findById(42L)).thenReturn(Optional.of(solicitudCon("1111")));

        assertThat(solicitudService.perteneceAEstudiante(42L, "9999")).isFalse();
    }

    @Test
    void cuandoLaSolicitudNoExiste_devuelveFalse() {
        when(solicitudRepository.findById(42L)).thenReturn(Optional.empty());

        assertThat(solicitudService.perteneceAEstudiante(42L, "1111")).isFalse();
    }
}
