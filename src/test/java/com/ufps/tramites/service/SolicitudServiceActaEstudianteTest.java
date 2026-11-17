package com.ufps.tramites.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.SolicitudRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// TP-156: el estudiante solo descarga el acta de grado que Posgrados ya
// generó; nunca la genera ni cambia su estado a GRADUADO.
@ExtendWith(MockitoExtension.class)
class SolicitudServiceActaEstudianteTest {

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private DocumentoService documentoService;
    @Mock private EstudianteRepository estudianteRepository;
    @InjectMocks private SolicitudService solicitudService;

    private Solicitud solicitud(String tipo, String estado) {
        Solicitud s = new Solicitud();
        s.setTipo(tipo);
        s.setEstado(estado);
        s.setCedula("1127339958");
        return s;
    }

    @Test
    void conActaYaGenerada_laDevuelveSinGenerarNada() throws Exception {
        byte[] pdf = "%PDF-acta".getBytes();
        when(solicitudRepository.findById(13L)).thenReturn(Optional.of(solicitud("GRADO", "APROBADA")));
        when(documentoService.obtenerActa(13L)).thenReturn(Optional.of(pdf));

        assertThat(solicitudService.obtenerActaParaEstudiante(13L)).isEqualTo(pdf);
        verify(documentoService, never()).guardarActaComoDocumento(any(), any());
        verifyNoInteractions(estudianteRepository);
    }

    @Test
    void sinActaGenerada_respondeQuePosgradosAunNoLaGenera() throws Exception {
        when(solicitudRepository.findById(13L)).thenReturn(Optional.of(solicitud("GRADO", "APROBADA")));
        when(documentoService.obtenerActa(13L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudService.obtenerActaParaEstudiante(13L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Oficina de Posgrados");
        verify(documentoService, never()).guardarActaComoDocumento(any(), any());
        verifyNoInteractions(estudianteRepository);
    }

    @Test
    void solicitudDeTerminacion_noTieneActaDeGrado() {
        when(solicitudRepository.findById(5L)).thenReturn(Optional.of(solicitud("TERMINACION_MATERIAS", "APROBADA")));

        assertThatThrownBy(() -> solicitudService.obtenerActaParaEstudiante(5L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(documentoService);
    }

    @Test
    void solicitudDeGradoNoAprobada_noTieneActa() {
        when(solicitudRepository.findById(13L)).thenReturn(Optional.of(solicitud("GRADO", "PENDIENTE_PAGO")));

        assertThatThrownBy(() -> solicitudService.obtenerActaParaEstudiante(13L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(documentoService);
    }

    @Test
    void solicitudInexistente_lanzaNoEncontrada() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudService.obtenerActaParaEstudiante(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
