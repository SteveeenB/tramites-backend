package com.ufps.tramites.repository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ufps.tramites.model.Solicitud;

@ExtendWith(MockitoExtension.class)
class SolicitudRepositoryTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Test
    void cuandoBuscaPorEstado_soloRetornaSolicitudesEnRevision() {
        Solicitud enRevision = new Solicitud();
        enRevision.setCedula("111");
        enRevision.setTipo("TERMINACION_MATERIAS");
        enRevision.setEstado("EN_REVISION");
        enRevision.setFechaSolicitud(LocalDate.now());

        when(solicitudRepository.findByEstado("EN_REVISION"))
                .thenReturn(List.of(enRevision));

        List<Solicitud> resultado = solicitudRepository.findByEstado("EN_REVISION");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCedula()).isEqualTo("111");
    }

    @Test
    void cuandoNoHaySolicitudesEnRevision_retornaListaVacia() {
        when(solicitudRepository.findByEstado("EN_REVISION"))
                .thenReturn(List.of());

        List<Solicitud> resultado = solicitudRepository.findByEstado("EN_REVISION");

        assertThat(resultado).isEmpty();
    }
}