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
