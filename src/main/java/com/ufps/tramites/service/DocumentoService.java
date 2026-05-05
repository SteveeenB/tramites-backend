package com.ufps.tramites.service;

import com.ufps.tramites.model.DocumentoSolicitud;
import com.ufps.tramites.repository.DocumentoSolicitudRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentoService {

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

    @Autowired
    private SupabaseStorageService storageService;

    /**
     * Valida y sube un documento de soporte al bucket de Supabase.
     * La ruta en el bucket es: {solicitudId}/{uuid}.{ext}
     */
    public Map<String, Object> guardarDocumento(Long solicitudId, MultipartFile archivo) throws IOException {
        return guardarDocumento(solicitudId, archivo, "SOPORTE");
    }

    /**
     * Igual que guardarDocumento pero permite especificar el tipo de documento.
     * Tipos usados: SOPORTE, FOTO_ESTUDIANTE, ACTA_SUSTENTACION, CERTIFICADO_INGLES
     */
    public Map<String, Object> guardarDocumento(Long solicitudId, MultipartFile archivo, String tipo) throws IOException {
        String contentType = archivo.getContentType() != null ? archivo.getContentType() : "";
        String extension = obtenerExtension(archivo.getOriginalFilename());

        if (!TIPOS_PERMITIDOS.contains(contentType) && !EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException(
                "Formato no permitido: " + archivo.getOriginalFilename() + ". Use PDF, PNG, JPG o DOCX."
            );
        }

        String nombreAlmacenado = UUID.randomUUID() + "." + extension;
        storageService.subir(solicitudId + "/" + nombreAlmacenado, archivo.getBytes(), contentType);

        DocumentoSolicitud doc = new DocumentoSolicitud();
        doc.setSolicitudId(solicitudId);
        doc.setTipo(tipo);
        doc.setNombreOriginal(archivo.getOriginalFilename());
        doc.setNombreAlmacenado(nombreAlmacenado);
        doc.setContentType(contentType);
        doc.setTamano(archivo.getSize());
        doc.setFechaSubida(LocalDateTime.now());
        documentoRepository.save(doc);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("id", doc.getId());
        resultado.put("tipo", doc.getTipo());
        resultado.put("nombreOriginal", doc.getNombreOriginal());
        resultado.put("tamano", doc.getTamano());
        resultado.put("fechaSubida", doc.getFechaSubida().toString());
        return resultado;
    }

    /**
     * Sube el PDF del acta al bucket y lo registra en la BD como tipo ACTA.
     * Si ya existe un registro, lo actualiza (upsert).
     */
    public void guardarActaComoDocumento(Long solicitudId, byte[] pdfBytes) throws IOException {
        String nombreAlmacenado = "acta-grado-" + solicitudId + ".pdf";
        storageService.subir(solicitudId + "/" + nombreAlmacenado, pdfBytes, "application/pdf");

        Optional<DocumentoSolicitud> existente =
                documentoRepository.findBySolicitudIdAndTipo(solicitudId, "ACTA");

        DocumentoSolicitud doc = existente.orElseGet(DocumentoSolicitud::new);
        doc.setSolicitudId(solicitudId);
        doc.setTipo("ACTA");
        doc.setNombreOriginal(nombreAlmacenado);
        doc.setNombreAlmacenado(nombreAlmacenado);
        doc.setContentType("application/pdf");
        doc.setTamano((long) pdfBytes.length);
        doc.setFechaSubida(LocalDateTime.now());
        documentoRepository.save(doc);
    }

    /**
     * Descarga el acta desde Supabase si ya fue generada.
     */
    public Optional<byte[]> obtenerActa(Long solicitudId) {
        return documentoRepository.findBySolicitudIdAndTipo(solicitudId, "ACTA")
                .map(doc -> storageService.descargar(solicitudId + "/" + doc.getNombreAlmacenado()));
    }

    public List<Map<String, Object>> listarDocumentos(Long solicitudId) {
        return documentoRepository.findBySolicitudId(solicitudId).stream().map(doc -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", doc.getId());
            m.put("tipo", doc.getTipo());
            m.put("nombreOriginal", doc.getNombreOriginal());
            m.put("url", storageService.obtenerUrl(solicitudId + "/" + doc.getNombreAlmacenado()));
            m.put("tamano", doc.getTamano());
            m.put("fechaSubida", doc.getFechaSubida().toString());
            m.put("contentType", doc.getContentType());
            return m;
        }).toList();
    }

    /**
     * Descarga los bytes de un documento desde Supabase y retorna bytes + metadatos.
     * Lanza IllegalArgumentException si el documento no existe o no pertenece a la solicitud.
     */
    public Map<String, Object> obtenerArchivo(Long solicitudId, Long docId) {
        DocumentoSolicitud doc = documentoRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        if (!solicitudId.equals(doc.getSolicitudId())) {
            throw new IllegalArgumentException("El documento no pertenece a esta solicitud");
        }

        byte[] bytes = storageService.descargar(solicitudId + "/" + doc.getNombreAlmacenado());
        if (bytes == null) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nombreOriginal", doc.getNombreOriginal());
        result.put("contentType",    doc.getContentType());
        result.put("bytes",          bytes);
        return result;
    }

    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
