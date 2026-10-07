package com.ufps.tramites.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.TipoCertificado;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.SolicitudCertificadoRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// FIX TP-189 (Johan Bueno, 07/10/2026): la solicitud del certificado
// TERMINACION_MATERIAS depende de tener la solicitud de terminación en
// APROBADA; las otras constancias no.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertificadoServiceTerminacionTest {

    @Mock private SolicitudCertificadoRepository certificadoRepository;
    @Mock private TipoCertificadoRepository tipoCertificadoRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @InjectMocks private CertificadoService certificadoService;

    private Usuario estudiante() {
        Usuario u = new Usuario();
        u.setCedula("1111");
        return u;
    }

    private Solicitud terminacion(String estado) {
        Solicitud s = new Solicitud();
        s.setCedula("1111");
        s.setTipo("TERMINACION_MATERIAS");
        s.setEstado(estado);
        return s;
    }

    private TipoCertificado tipo(String codigo) {
        TipoCertificado t = new TipoCertificado();
        t.setCodigo(codigo);
        t.setActivo(true);
        return t;
    }

    @Test
    void solicitarTerminacion_sinSolicitudAprobada_fallaCon422() {
        when(tipoCertificadoRepository.findByCodigo("TERMINACION_MATERIAS"))
                .thenReturn(Optional.of(tipo("TERMINACION_MATERIAS")));
        when(certificadoRepository.findByCedulaAndTipoCertificado("1111", "TERMINACION_MATERIAS"))
                .thenReturn(List.of());
        when(solicitudRepository.findByCedula("1111"))
                .thenReturn(List.of(terminacion("EN_REVISION")));

        assertThatThrownBy(() -> certificadoService
                    .solicitarCertificado(estudiante(), "TERMINACION_MATERIAS", "DIGITAL", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Terminación de Materias");
    }

    @Test
    void solicitarMatricula_sinTerminacionAprobada_funciona() {
        when(tipoCertificadoRepository.findByCodigo("MATRICULA"))
                .thenReturn(Optional.of(tipo("MATRICULA")));
        when(certificadoRepository.findByCedulaAndTipoCertificado("1111", "MATRICULA"))
                .thenReturn(List.of());

        assertThatCode(() -> certificadoService
                .solicitarCertificado(estudiante(), "MATRICULA", "DIGITAL", null))
                .doesNotThrowAnyException();
    }
}
