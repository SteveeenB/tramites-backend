package com.ufps.tramites.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.ufps.tramites.repository.DocumentoSolicitudRepository;

/** PRB-06 (hallazgo F, CP-020/CP-021): validación de archivos subidos. */
@ExtendWith(MockitoExtension.class)
class DocumentoServiceValidacionTest {

    private static final byte[] PDF  = "%PDF-1.7\n...".getBytes();
    private static final byte[] PNG  = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] DOCX = {'P', 'K', 0x03, 0x04, 0, 0};
    private static final byte[] EXE  = {'M', 'Z', (byte) 0x90, 0, 3, 0};

    @Mock private DocumentoSolicitudRepository documentoRepository;
    @Mock private SupabaseStorageService storageService;

    @InjectMocks private DocumentoService documentoService;

    @Test
    void exeDeclaradoComoPdf_seRechazaYNoSeAlmacena() {
        var archivo = new MockMultipartFile("archivo", "programa.exe", "application/pdf", EXE);

        assertThatThrownBy(() -> documentoService.guardarDocumento(7L, archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formato no permitido")
                .hasMessageContaining("PDF, PNG, JPG, DOCX");
        verifyNoInteractions(storageService, documentoRepository);
    }

    @Test
    void exeRenombradoAPdf_seRechazaPorContenido() {
        var archivo = new MockMultipartFile("archivo", "programa.pdf", "application/pdf", EXE);

        assertThatThrownBy(() -> documentoService.guardarDocumento(7L, archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no corresponde a un archivo PDF");
        verifyNoInteractions(storageService, documentoRepository);
    }

    @Test
    void extensionValidaConMimeNoPermitido_seRechaza() {
        var archivo = new MockMultipartFile("archivo", "acta.pdf", "application/x-msdownload", PDF);

        assertThatThrownBy(() -> documentoService.validarArchivo(archivo, "SOPORTE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void archivosValidos_seAceptan() {
        assertThatCode(() -> {
            documentoService.validarArchivo(new MockMultipartFile("a", "a.pdf", "application/pdf", PDF), "SOPORTE");
            documentoService.validarArchivo(new MockMultipartFile("a", "a.PNG", "image/png", PNG), "SOPORTE");
            documentoService.validarArchivo(new MockMultipartFile("a", "a.jpg", "image/jpeg", JPEG), "SOPORTE");
            documentoService.validarArchivo(new MockMultipartFile("a", "a.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", DOCX), "SOPORTE");
        }).doesNotThrowAnyException();
    }

    @Test
    void pdfValido_seAlmacenaConMimeCanonico() throws Exception {
        var archivo = new MockMultipartFile("archivo", "acta.pdf", "application/pdf", PDF);

        documentoService.guardarDocumento(7L, archivo);

        verify(storageService).subir(anyString(), any(), eq("application/pdf"));
        verify(documentoRepository).save(any());
    }

    @Test
    void fotoEnPdf_seRechazaIndicandoFormatosDeLaFoto() {
        var archivo = new MockMultipartFile("foto", "foto.pdf", "application/pdf", PDF);

        assertThatThrownBy(() -> documentoService.validarArchivo(archivo, "FOTO_ESTUDIANTE"))
                .hasMessageStartingWith("Fotografía actualizada")
                .hasMessageContaining("Use PNG, JPG.");
    }

    @Test
    void documentoObligatorioVacio_indicaCualFalta() throws Exception {
        var vacio = new MockMultipartFile("actaSustentacion", "", "application/octet-stream", new byte[0]);

        assertThatThrownBy(() -> documentoService.validarArchivo(vacio, "ACTA_SUSTENTACION"))
                .hasMessage("Falta el documento obligatorio: Acta de sustentación.");
        assertThatThrownBy(() -> documentoService.validarArchivo(null, "FOTO_ESTUDIANTE"))
                .hasMessage("Falta el documento obligatorio: Fotografía actualizada.");
        verify(storageService, never()).subir(anyString(), any(), anyString());
    }
}
