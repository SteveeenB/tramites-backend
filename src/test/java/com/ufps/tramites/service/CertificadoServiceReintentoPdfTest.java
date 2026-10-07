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

import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.model.TipoCertificado;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.SolicitudCertificadoRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// FIX TP-199 (Johan Bueno, 07/10/2026): cubre el flujo de fallo total en
// la generación del PDF: tras 3 intentos la solicitud queda en
// GENERACION_FALLIDA y se dispara una notificación a POS001.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertificadoServiceReintentoPdfTest {

    @Mock private SolicitudCertificadoRepository certificadoRepository;
    @Mock private TipoCertificadoRepository tipoCertificadoRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private CertificadoConstanciaPdfService pdfService;
    @Mock private PlantillaCertificadoService plantillaService;
    @Mock private CorreoConstanciaService correoService;
    @Mock private SupabaseStorageService storage;
    @Mock private NotificacionService notificacionService;
    @InjectMocks private CertificadoService certificadoService;

    private SolicitudCertificado solicitudPagada() {
        SolicitudCertificado s = new SolicitudCertificado();
        s.setCedula("1111");
        s.setTipoCertificado("MATRICULA");
        s.setEstado("PAGADO");
        s.setModalidadEnvio("DIGITAL");
        return s;
    }

    private TipoCertificado tipoSinPlantilla() {
        TipoCertificado t = new TipoCertificado();
        t.setCodigo("MATRICULA");
        t.setLabel("Constancia de matrícula");
        t.setActivo(true);
        // sin plantillaHtml → se delega a pdfService.generar
        return t;
    }

    @Test
    void generarYNotificar_tresIntentosFallidos_marcaFallidaYAvisaAPosgrados() throws Exception {
        SolicitudCertificado s = solicitudPagada();
        when(certificadoRepository.findById(5L)).thenReturn(Optional.of(s));
        when(tipoCertificadoRepository.findByCodigo("MATRICULA")).thenReturn(Optional.of(tipoSinPlantilla()));
        when(pdfService.generar(any(), any(), any()))
                .thenThrow(new RuntimeException("plantilla caída"));

        certificadoService.generarYNotificar(5L);

        // 3 intentos reales
        verify(pdfService, atLeast(3)).generar(any(), any(), any());
        // Estado terminal
        assertThat(s.getEstado()).isEqualTo("GENERACION_FALLIDA");
        // Aviso al administrador POS001
        verify(notificacionService).crear(
                org.mockito.ArgumentMatchers.eq("POS001"),
                org.mockito.ArgumentMatchers.eq("FALLO_GENERACION"),
                anyString(), anyString(), anyString());
    }

    @Test
    void reintentarGeneracionPdf_solicitudEnOtroEstado_lanza422() {
        SolicitudCertificado s = solicitudPagada();
        s.setEstado("GENERADO");
        when(certificadoRepository.findById(5L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> certificadoService.reintentarGeneracionPdf(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GENERACION_FALLIDA");
    }
}
