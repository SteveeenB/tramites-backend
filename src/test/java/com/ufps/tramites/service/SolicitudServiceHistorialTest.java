package com.ufps.tramites.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ufps.tramites.model.CambioEstadoSolicitud;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.repository.CambioEstadoSolicitudRepository;
import com.ufps.tramites.repository.SolicitudRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// FIX TP-201 (Johan Bueno, 07/10/2026): valida que cambiarEstado deja la
// transición en historial, actualiza el estado y marca fechaCierre
// cuando el nuevo estado es terminal.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudServiceHistorialTest {

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private CambioEstadoSolicitudRepository cambioEstadoRepository;
    @InjectMocks private SolicitudService solicitudService;

    private Solicitud solicitudEnRevision() {
        Solicitud s = new Solicitud();
        s.setCedula("1111");
        s.setTipo("TERMINACION_MATERIAS");
        s.setEstado("EN_REVISION");
        return s;
    }

    @Test
    void cambiarEstado_persisteFilaDeHistorialConLosCamposCorrectos() {
        Solicitud s = solicitudEnRevision();

        solicitudService.cambiarEstado(s, "APROBADA_DIRECTOR", "DIR1", "USUARIO");

        ArgumentCaptor<CambioEstadoSolicitud> captor =
                ArgumentCaptor.forClass(CambioEstadoSolicitud.class);
        verify(cambioEstadoRepository).save(captor.capture());
        CambioEstadoSolicitud h = captor.getValue();
        assertThat(h.getEstadoAnterior()).isEqualTo("EN_REVISION");
        assertThat(h.getEstadoNuevo()).isEqualTo("APROBADA_DIRECTOR");
        assertThat(h.getActor()).isEqualTo("DIR1");
        assertThat(h.getActorTipo()).isEqualTo("USUARIO");
        assertThat(h.getFecha()).isNotNull();
        assertThat(s.getEstado()).isEqualTo("APROBADA_DIRECTOR");
    }

    @Test
    void cambiarEstado_cuandoEstadoNuevoEsFinal_marcaFechaCierre() {
        Solicitud s = solicitudEnRevision();

        solicitudService.cambiarEstado(s, "APROBADA", "DIR1", "USUARIO");

        assertThat(s.getFechaCierre()).isNotNull();
    }

    @Test
    void cambiarEstado_estadoIntermedio_noMarcaFechaCierre() {
        Solicitud s = solicitudEnRevision();

        solicitudService.cambiarEstado(s, "APROBADA_DIRECTOR", "DIR1", "USUARIO");

        assertThat(s.getFechaCierre()).isNull();
    }

    @Test
    void obtenerHistorial_delegaAlRepositorioOrdenadoCronologicamente() {
        when(cambioEstadoRepository.findBySolicitudIdOrderByFechaAsc(99L))
                .thenReturn(java.util.List.of());

        assertThat(solicitudService.obtenerHistorial(99L)).isEmpty();
        verify(cambioEstadoRepository).findBySolicitudIdOrderByFechaAsc(99L);
    }
}
