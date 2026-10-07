package com.ufps.tramites.service;

import com.ufps.tramites.model.DocumentoSolicitud;
import com.ufps.tramites.repository.DocumentoSolicitudRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentoService {

    private static final String MIME_DOCX =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /** Extensión permitida → tipo MIME canónico con el que se almacena. */
    private static final Map<String, String> MIME_POR_EXTENSION = Map.of(
        "pdf",  "application/pdf",
        "png",  "image/png",
        "jpg",  "image/jpeg",
        "jpeg", "image/jpeg",
        "docx", MIME_DOCX
    );

    /** Tipos MIME aceptados en la cabecera de cada parte (image/jpg lo envían algunos clientes). */
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
        "application/pdf", "image/png", "image/jpeg", "image/jpg", MIME_DOCX
    );

    /** Firmas (magic bytes) que debe tener el contenido según su extensión. */
    private static final byte[] FIRMA_PDF  = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] FIRMA_PNG  = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_ZIP  = {'P', 'K', 0x03, 0x04}; // DOCX es un contenedor ZIP

    /** Formatos admitidos por tipo de documento; los tipos no listados admiten todos. */
    private static final Map<String, Set<String>> EXTENSIONES_POR_TIPO = Map.of(
        "FOTO_ESTUDIANTE",   Set.of("png", "jpg", "jpeg"),
        "ACTA_SUSTENTACION", Set.of("pdf", "docx")
    );

    private static final Map<String, String> NOMBRE_TIPO = Map.of(
        "FOTO_ESTUDIANTE",    "Fotografía actualizada",
        "ACTA_SUSTENTACION",  "Acta de sustentación",
        "CERTIFICADO_INGLES", "Certificado de inglés",
        "SOPORTE",            "Documento de soporte"
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
        validarArchivo(archivo, tipo);

        String extension = obtenerExtension(archivo.getOriginalFilename());
        String contentType = MIME_POR_EXTENSION.get(extension);
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

    /**
     * Valida un archivo antes de almacenarlo: presencia, extensión y tipo MIME permitidos
     * para el tipo de documento, y que el contenido real (firma) corresponda a la extensión.
     * Lanza IllegalArgumentException con un mensaje que nombra el documento afectado.
     */
    public void validarArchivo(MultipartFile archivo, String tipo) throws IOException {
        String nombreDoc = NOMBRE_TIPO.getOrDefault(tipo, "Documento");

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Falta el documento obligatorio: " + nombreDoc + ".");
        }

        String nombreArchivo = archivo.getOriginalFilename();
        String extension = obtenerExtension(nombreArchivo);
        Set<String> permitidas = EXTENSIONES_POR_TIPO.getOrDefault(tipo, MIME_POR_EXTENSION.keySet());
        String formatos = describirFormatos(permitidas);

        String contentType = archivo.getContentType() != null ? archivo.getContentType().toLowerCase() : "";
        if (!permitidas.contains(extension) || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException(
                nombreDoc + ": formato no permitido (" + nombreArchivo + "). Use " + formatos + "."
            );
        }

        if (!firmaCorresponde(archivo, extension)) {
            throw new IllegalArgumentException(
                nombreDoc + ": el contenido de " + nombreArchivo
                    + " no corresponde a un archivo " + extension.toUpperCase() + " válido. Use " + formatos + "."
            );
        }
    }

    private boolean firmaCorresponde(MultipartFile archivo, String extension) throws IOException {
        byte[] esperada = switch (extension) {
            case "pdf"         -> FIRMA_PDF;
            case "png"         -> FIRMA_PNG;
            case "jpg", "jpeg" -> FIRMA_JPEG;
            case "docx"        -> FIRMA_ZIP;
            default            -> null;
        };
        if (esperada == null) return false;

        byte[] cabecera = new byte[esperada.length];
        try (InputStream in = archivo.getInputStream()) {
            if (in.readNBytes(cabecera, 0, cabecera.length) < cabecera.length) return false;
        }
        return Arrays.equals(cabecera, esperada);
    }

    private String describirFormatos(Set<String> extensiones) {
        return extensiones.stream()
                .map(e -> "jpeg".equals(e) ? "jpg" : e)
                .distinct()
                .sorted(Comparator.comparingInt(List.of("pdf", "png", "jpg", "docx")::indexOf))
                .map(String::toUpperCase)
                .collect(Collectors.joining(", "));
    }

    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
