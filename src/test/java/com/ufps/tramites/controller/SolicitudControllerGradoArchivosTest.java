package com.ufps.tramites.controller;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.security.JwtService;
import com.ufps.tramites.security.PrincipalResolver;
import com.ufps.tramites.security.ResolvedPrincipal;
import com.ufps.tramites.service.SolicitudService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// TP-156: en la regresión del 17/11/2026 una foto «.exe» en la solicitud de
// grado devolvía 500 «Internal Server Error». Debe ser 400 con el motivo.
@ExtendWith(MockitoExtension.class)
class SolicitudControllerGradoArchivosTest {

    @Mock private SolicitudService solicitudService;
    @Mock private PrincipalResolver principalResolver;
    @InjectMocks private SolicitudController controller;

    private final MockMultipartFile foto =
            new MockMultipartFile("foto", "foto.exe", "application/x-msdownload", new byte[] {0x4d, 0x5a});
    private final MockMultipartFile acta =
            new MockMultipartFile("actaSustentacion", "acta.pdf", "application/pdf", "%PDF-1.4".getBytes());

    private Authentication autenticado() throws Exception {
        Authentication auth = mock(Authentication.class);
        Usuario u = new Usuario();
        ResolvedPrincipal p = new ResolvedPrincipal(JwtService.PRINCIPAL_USUARIO, 1L, "2000000010", "22030004",
                "ESTUDIANTE", "Andrea Prueba Grado", "andrea@test.com", null, null, u, null);
        when(principalResolver.resolve(auth)).thenReturn(p);
        return auth;
    }

    @Test
    @SuppressWarnings("unchecked")
    void formatoNoPermitido_respondeBadRequestConElMotivo() throws Exception {
        Authentication auth = autenticado();
        when(solicitudService.crearSolicitudGrado(any(), anyString(), anyString(), anyString(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Formato no permitido: foto.exe. Use PDF, PNG, JPG o DOCX."));

        ResponseEntity<?> r = controller.crearSolicitudGrado(auth, "Título", "Resumen", "INVESTIGACION",
                foto, acta, null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<String, Object>) r.getBody()).values().toString()).contains("Formato no permitido");
    }

    @Test
    void reglaDeNegocio_sigueRespondiendo422() throws Exception {
        Authentication auth = autenticado();
        when(solicitudService.crearSolicitudGrado(any(), anyString(), anyString(), anyString(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Ya existe una solicitud de grado con estado: EN_REVISION"));

        ResponseEntity<?> r = controller.crearSolicitudGrado(auth, "Título", "Resumen", "INVESTIGACION",
                foto, acta, null);

        assertThat(r.getStatusCode().value()).isEqualTo(422);
    }
}
