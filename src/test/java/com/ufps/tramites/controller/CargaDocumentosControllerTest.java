package com.ufps.tramites.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.ufps.tramites.repository.DocumentoSolicitudRepository;
import com.ufps.tramites.security.PrincipalResolver;
import com.ufps.tramites.service.DocumentoService;
import com.ufps.tramites.service.SolicitudService;
import com.ufps.tramites.service.SupabaseStorageService;
import com.ufps.tramites.service.ValidacionGradoService;

/** PRB-06 (hallazgo F): re-prueba de CP-020 y CP-021 a nivel HTTP. */
@ExtendWith(MockitoExtension.class)
class CargaDocumentosControllerTest {

    @Mock private SolicitudService solicitudService;
    @Mock private PrincipalResolver principalResolver;
    @Mock private ValidacionGradoService validacionGradoService;
    @Mock private SupabaseStorageService storageService;
    @Mock private DocumentoSolicitudRepository documentoRepository;

    @InjectMocks private DocumentoService documentoService;
    @InjectMocks private SolicitudController controller;

    private GlobalExceptionHandler handler;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "documentoService", documentoService);
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "maxFileSize", DataSize.ofMegabytes(15));
        ReflectionTestUtils.setField(handler, "maxRequestSize", DataSize.ofMegabytes(60));
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(handler).build();
    }

    /** CP-020: .exe declarado como application/pdf → 400 con mensaje, no se almacena. */
    @Test
    void cp020_exeDeclaradoComoPdf_responde400ConMensaje() throws Exception {
        var exe = new MockMultipartFile("archivo", "programa.exe", "application/pdf", new byte[] {'M', 'Z', 0, 0});

        mvc.perform(multipart("/api/solicitudes/7/documentos").file(exe))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Documento de soporte: formato no permitido (programa.exe). Use PDF, PNG, JPG, DOCX."));
        verifyNoInteractions(storageService, documentoRepository);
    }

    /** CP-020: archivo por encima del límite → 413 con mensaje que indica el límite. */
    @Test
    void cp020_archivoDemasiadoGrande_responde413ConLimite() {
        var resp = handler.tamanoExcedido(new MaxUploadSizeExceededException(15L * 1024 * 1024));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(resp.getBody()).containsEntry("error",
                "El archivo supera el tamaño máximo permitido de 15 MB por archivo (60 MB en total por envío).");
    }

    /** CP-021: falta un documento obligatorio → 400 que indica cuál. */
    @Test
    void cp021_faltaActaSustentacion_responde400IndicandoCual() throws Exception {
        var foto = new MockMultipartFile("foto", "foto.png", "image/png", new byte[] {1});

        mvc.perform(multipart("/api/solicitudes/grado").file(foto)
                        .param("tituloProyecto", "Proyecto")
                        .param("resumen", "Resumen")
                        .param("tipoProyecto", "TESIS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Falta el documento obligatorio: Acta de sustentación."));
        verifyNoInteractions(solicitudService);
    }
}
