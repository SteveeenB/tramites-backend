package com.ufps.tramites.service;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.SolicitudCertificadoRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// FIX TP-190 (Johan Bueno, 07/10/2026): verifica que verificarCertificado
// reconoce tanto UFPS-TM-... como UFPS-CERT-...
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerificarCertificadoTest {

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private SolicitudCertificadoRepository solicitudCertificadoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private SolicitudService solicitudService;

    @Test
    void codigoUfpsTm_deSolicitudAprobada_esValido() {
        Solicitud s = new Solicitud();
        s.setTipo("TERMINACION_MATERIAS");
        s.setEstado("APROBADA");
        s.setCedula("1234567890");
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(s));
        when(usuarioRepository.findByCedula("1234567890")).thenReturn(Optional.empty());

        Map<String, Object> r = solicitudService.verificarCertificado("UFPS-TM-10-7890");

        assertThat(r).containsEntry("valido", true);
        assertThat(r).containsEntry("tipo", "Certificado de Terminación de Materias");
    }

    @Test
    void codigoUfpsCert_deConstanciaGenerada_esValido() {
        SolicitudCertificado c = new SolicitudCertificado();
        c.setId(5L);
        c.setEstado("GENERADO");
        c.setCedula("1098765432");
        when(solicitudCertificadoRepository.findById(5L)).thenReturn(Optional.of(c));
        when(usuarioRepository.findByCedula("1098765432")).thenReturn(Optional.empty());

        Map<String, Object> r = solicitudService.verificarCertificado("UFPS-CERT-5-5432");

        assertThat(r).containsEntry("valido", true);
        assertThat(r).containsEntry("tipo", "Constancia académica");
    }

    @Test
    void codigoUfpsCert_cuandoLast4NoCoincide_esInvalido() {
        SolicitudCertificado c = new SolicitudCertificado();
        c.setId(5L);
        c.setEstado("GENERADO");
        c.setCedula("1098765432");
        when(solicitudCertificadoRepository.findById(5L)).thenReturn(Optional.of(c));

        Map<String, Object> r = solicitudService.verificarCertificado("UFPS-CERT-5-0000");

        assertThat(r).containsEntry("valido", false);
    }

    @Test
    void prefijoDesconocido_esInvalido() {
        Map<String, Object> r = solicitudService.verificarCertificado("UFPS-XYZ-1-1234");

        assertThat(r).containsEntry("valido", false);
    }
}
