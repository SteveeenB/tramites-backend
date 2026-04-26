package com.ufps.tramites.service;

import com.ufps.tramites.model.DocumentoSolicitud;
import com.ufps.tramites.repository.DocumentoSolicitudRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentoService {

    @Value("${app.uploads.directorio:./uploads}")
    private String directorioBase;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
        "application/pdf",
        "image/png",
        "image/jpeg",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
        "pdf", "png", "jpg", "jpeg", "docx"
    );

    @Autowired
    private DocumentoSolicitudRepository documentoRepository;

    public Map<String, Object> guardarDocumento(Long solicitudId, MultipartFile archivo) throws IOException {
        String contentType = archivo.getContentType() != null ? archivo.getContentType() : "";
        String extension = obtenerExtension(archivo.getOriginalFilename());

        if (!TIPOS_PERMITIDOS.contains(contentType) && !EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException(
                "Formato no permitido: " + archivo.getOriginalFilename() + ". Use PDF, PNG, JPG o DOCX."
            );
        }

        Path directorio = Paths.get(directorioBase, String.valueOf(solicitudId));
        Files.createDirectories(directorio);

        String nombreAlmacenado = UUID.randomUUID() + "." + extension;
        Files.copy(archivo.getInputStream(), directorio.resolve(nombreAlmacenado), StandardCopyOption.REPLACE_EXISTING);

        DocumentoSolicitud doc = new DocumentoSolicitud();
        doc.setSolicitudId(solicitudId);
        doc.setTipo("SOPORTE");
        doc.setNombreOriginal(archivo.getOriginalFilename());
        doc.setNombreAlmacenado(nombreAlmacenado);
        doc.setContentType(contentType);
        doc.setTamano(archivo.getSize());
        doc.setFechaSubida(LocalDateTime.now());
        documentoRepository.save(doc);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("id", doc.getId());
        resultado.put("nombreOriginal", doc.getNombreOriginal());
        resultado.put("tamano", doc.getTamano());
        resultado.put("fechaSubida", doc.getFechaSubida().toString());
        return resultado;
    }

    /**
     * Guarda el PDF del acta generado como ACTA en el expediente digital.
     * Si ya existe un acta para esta solicitud, la sobreescribe.
     */
    public void guardarActaComoDocumento(Long solicitudId, byte[] pdfBytes) throws IOException {
        Path directorio = Paths.get(directorioBase, String.valueOf(solicitudId));
        Files.createDirectories(directorio);

        String nombreAlmacenado = "acta-grado-" + solicitudId + ".pdf";
        Files.write(directorio.resolve(nombreAlmacenado), pdfBytes);

        Optional<DocumentoSolicitud> existente =
                documentoRepository.findBySolicitudIdAndTipo(solicitudId, "ACTA");

        DocumentoSolicitud doc = existente.orElseGet(DocumentoSolicitud::new);
        doc.setSolicitudId(solicitudId);
        doc.setTipo("ACTA");
        doc.setNombreOriginal("acta-grado-" + solicitudId + ".pdf");
        doc.setNombreAlmacenado(nombreAlmacenado);
        doc.setContentType("application/pdf");
        doc.setTamano((long) pdfBytes.length);
        doc.setFechaSubida(LocalDateTime.now());
        documentoRepository.save(doc);
    }

    /**
     * Devuelve los bytes del acta si ya fue generada y guardada en disco.
     */
    public Optional<byte[]> obtenerActa(Long solicitudId) {
        return documentoRepository.findBySolicitudIdAndTipo(solicitudId, "ACTA")
                .map(doc -> {
                    Path ruta = Paths.get(directorioBase, String.valueOf(solicitudId),
                            doc.getNombreAlmacenado());
                    try {
                        return Files.exists(ruta) ? Files.readAllBytes(ruta) : null;
                    } catch (IOException e) {
                        return null;
                    }
                });
    }

    public List<Map<String, Object>> listarDocumentos(Long solicitudId) {
        return documentoRepository.findBySolicitudId(solicitudId).stream().map(doc -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", doc.getId());
            m.put("nombreOriginal", doc.getNombreOriginal());
            m.put("tamano", doc.getTamano());
            m.put("fechaSubida", doc.getFechaSubida().toString());
            return m;
        }).toList();
    }

    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
