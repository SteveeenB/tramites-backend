package com.ufps.tramites.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.ufps.tramites.repository.DocumentoSolicitudRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// FIX TP-191 (Bryan Niño, 07/10/2026): valida que DocumentoService.guardar
// rechaza con OR (no AND) y con validación de firma de contenido.
@ExtendWith(MockitoExtension.class)
class DocumentoServiceValidacionTest {

    @Mock private DocumentoSolicitudRepository documentoRepository;
    @Mock private SupabaseStorageService storageService;
    @InjectMocks private DocumentoService documentoService;

    @Test
    void archivoConMimePdfPeroExtensionExe_esRechazado() {
        // Cubre CP-020: antes esto pasaba porque la condición era && y la MIME
        // declarada era permitida; ahora la extensión fuera del catálogo
        // también cuenta.
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "malicioso.exe", "application/pdf",
                "contenido ejecutable arbitrario".getBytes());

        assertThatThrownBy(() -> documentoService.guardarDocumento(1L, archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato no permitido");
    }

    @Test
    void archivoConMimeYExtensionValidosPeroContenidoNoCoincide_esRechazado() {
        // MIME y extensión OK pero los primeros bytes no son los de un PDF
        // (debe empezar con %PDF). Se bloquea para evitar que renombrando un
        // ejecutable como .pdf se cuele.
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "falso.pdf", "application/pdf",
                "no es un PDF real".getBytes());

        assertThatThrownBy(() -> documentoService.guardarDocumento(1L, archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide");
    }

    @Test
    void archivoConMimeImagenPeroExtensionDesconocida_esRechazado() {
        // Simétrico del primer test, pero ahora la MIME es la del catálogo y
        // la extensión no — también debe fallar porque la condición es OR.
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "payload.sh", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        assertThatThrownBy(() -> documentoService.guardarDocumento(1L, archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato no permitido");
    }
}
