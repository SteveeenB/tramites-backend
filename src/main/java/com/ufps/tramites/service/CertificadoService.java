package com.ufps.tramites.service;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.ufps.tramites.model.Estudiante;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.model.TipoCertificado;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.SolicitudCertificadoRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.repository.UsuarioRepository;

// FIX TP-194 (Diego Bermúdez, 07/10/2026): solicitarCertificado y
// registrarPagoCertificado escriben SolicitudCertificado + Pago + envían
// notificaciones; se envuelve toda la clase en transacción.
@Service
@org.springframework.transaction.annotation.Transactional
public class CertificadoService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoService.class);

    /** Días que tiene el estudiante para pagar antes de que el recibo se considere vencido. */
    private static final int DIAS_VIGENCIA_PAGO = 3;

    @Autowired private SolicitudCertificadoRepository certificadoRepository;
    @Autowired private TipoCertificadoRepository tipoCertificadoRepository;
    // FIX TP-189 (Johan Bueno, 07/10/2026): el certificado
    // TERMINACION_MATERIAS requiere que el estudiante tenga una solicitud
    // de terminación en estado APROBADA; consultamos aquí.
    @Autowired private SolicitudRepository solicitudRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private CertificadoConstanciaPdfService pdfService;
    @Autowired private PlantillaCertificadoService plantillaService;
    @Autowired private CorreoConstanciaService correoService;
    @Autowired private SupabaseStorageService storage;
    @Autowired private NotificacionService notificacionService;

    // ── 1) SOLICITUD ──────────────────────────────────────────────────────────

    public Map<String, Object> solicitarCertificado(Usuario estudiante,
                                                    String tipoCertificado,
                                                    String modalidadEnvio,
                                                    String destinatario) {
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(tipoCertificado)
            .orElseThrow(() -> new IllegalStateException("Tipo de certificado inválido: " + tipoCertificado));

        if (Boolean.FALSE.equals(tipo.getActivo())) {
            throw new IllegalStateException("Este tipo de certificado no está disponible actualmente.");
        }

        if (!"FISICA".equals(modalidadEnvio) && !"DIGITAL".equals(modalidadEnvio)) {
            throw new IllegalStateException("Modalidad de envío inválida: " + modalidadEnvio);
        }

        List<SolicitudCertificado> existentes = certificadoRepository
            .findByCedulaAndTipoCertificado(estudiante.getCedula(), tipoCertificado);
        boolean tieneVigente = existentes.stream()
            .anyMatch(s -> "PENDIENTE_PAGO".equals(s.getEstado()));
        if (tieneVigente) {
            throw new IllegalStateException(
                "Ya tienes una solicitud vigente de este tipo de certificado. " +
                "Debes pagar o esperar a que venza el recibo antes de generar uno nuevo."
            );
        }

        // FIX TP-189 (Johan Bueno, 07/10/2026): el certificado de
        // Terminación de Materias sólo se puede solicitar si existe una
        // solicitud de terminación en estado APROBADA del mismo estudiante.
        // Las demás constancias (matrícula, buena conducta, registro
        // calificado) siguen sin restricción, por eso la regla se aplica
        // sólo al código TERMINACION_MATERIAS.
        if ("TERMINACION_MATERIAS".equals(tipoCertificado)
                && !tieneTerminacionAprobada(estudiante.getCedula())) {
            throw new IllegalStateException(
                "El certificado de Terminación de Materias requiere tener una "
                + "solicitud de terminación en estado APROBADA."
            );
        }

        LocalDate hoy = LocalDate.now();
        double costo = tipo.precioTotal(modalidadEnvio);

        // Doble-write transicional (Bloque 5a): cedula viejo + FK Estudiante
        Estudiante perfilEstudiante = estudianteRepository.findByUsuario(estudiante).orElse(null);
        SolicitudCertificado s = new SolicitudCertificado();
        s.setCedula(estudiante.getCedula());
        s.setEstudiante(perfilEstudiante);
        s.setTipoCertificado(tipoCertificado);
        s.setModalidadEnvio(modalidadEnvio);
        s.setEstado("PENDIENTE_PAGO");
        s.setFechaSolicitud(hoy);
        s.setFechaVencimientoPago(hoy.plusDays(DIAS_VIGENCIA_PAGO));
        s.setCosto(costo);
        s.setDestinatario(destinatario);
        s.setObservaciones("Solicitud de certificado registrada por el sistema.");

        certificadoRepository.save(s);
        return construirRespuesta(s, estudiante, tipo);
    }

    // FIX TP-189 (Johan Bueno, 07/10/2026): helper que determina si un
    // estudiante ya tiene la terminación de materias aprobada; se usa
    // tanto para bloquear la solicitud del certificado como para marcar
    // su disponibilidad en GET /api/certificados/tipos.
    public boolean tieneTerminacionAprobada(String cedula) {
        if (cedula == null) return false;
        return solicitudRepository.findByCedula(cedula).stream()
                .anyMatch(s -> "TERMINACION_MATERIAS".equals(s.getTipo())
                        && "APROBADA".equals(s.getEstado()));
    }

    /** Devuelve el catálogo de tipos activos marcando la disponibilidad real
     *  para el estudiante que consulta. Hoy la única restricción es la del
     *  certificado TERMINACION_MATERIAS; los demás van siempre disponibles. */
    public List<Map<String, Object>> listarTiposParaEstudiante(String cedulaEstudiante) {
        boolean puedeTerminacion = tieneTerminacionAprobada(cedulaEstudiante);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (TipoCertificado t : tipoCertificadoRepository.findByActivoTrue()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", t.getCodigo());
            m.put("label", t.getLabel());
            m.put("activo", t.getActivo());
            // TP-189 dejó este mapa con solo codigo/label/activo/disponible, así que
            // el estudiante no recibía el precio y la pantalla de certificados
            // mostraba "Valor: $ 0" (y el recargo de la modalidad física). Se incluyen
            // los mismos campos de catálogo que devuelve findByActivoTrue(), salvo
            // plantillaHtml, que es pesada y solo la usa el admin.
            m.put("descripcion", t.getDescripcion());
            m.put("precioDigital", t.getPrecioDigital());
            m.put("costoLogisticaFisica", t.getCostoLogisticaFisica());
            m.put("direccionOficina", t.getDireccionOficina());
            m.put("tiempoEntregaDias", t.getTiempoEntregaDias());
            boolean esTerminacion = "TERMINACION_MATERIAS".equals(t.getCodigo());
            boolean disponible = !esTerminacion || puedeTerminacion;
            m.put("disponible", disponible);
            if (!disponible) {
                m.put("motivo",
                        "Requiere tener la Terminación de Materias aprobada.");
            }
            resultado.add(m);
        }
        return resultado;
    }

    // ── 2) HISTORIAL DEL ESTUDIANTE ───────────────────────────────────────────

    public List<Map<String, Object>> obtenerCertificadosPorCedula(String cedula) {
        List<SolicitudCertificado> solicitudes = certificadoRepository.findByCedula(cedula);
        Usuario estudiante = usuarioRepository.findByCedula(cedula).orElse(null);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (SolicitudCertificado s : solicitudes) {
            TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);
            resultado.add(construirRespuesta(s, estudiante, tipo));
        }
        return resultado;
    }

    // ── 3a) PAGO CONFIRMADO POR WEBHOOK DE WOMPI ─────────────────────────────

    public void registrarPagoCertificado(Long id) {
        SolicitudCertificado s = certificadoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud de certificado no encontrada: " + id));

        if (!"PENDIENTE_PAGO".equals(s.getEstado())) {
            log.warn("[WOMPI] Certificado {} ya procesado, estado actual: {}", id, s.getEstado());
            return;
        }

        s.setEstado("PAGADO");
        s.setFechaPago(LocalDateTime.now());
        s.setObservaciones("Pago confirmado vía Wompi.");
        certificadoRepository.save(s);

        generarYNotificar(id);
    }

    // ── 3b) PAGO + GENERACIÓN INMEDIATA (Regla PRD: 3-5 min) ─────────────────

    public Map<String, Object> simularPago(Long id, String cedula) {
        SolicitudCertificado s = certificadoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con id: " + id));

        if (!cedula.equals(s.getCedula())) {
            throw new IllegalStateException("Esta solicitud no pertenece al estudiante.");
        }
        if (!"PENDIENTE_PAGO".equals(s.getEstado())) {
            throw new IllegalStateException(
                "Esta solicitud no está pendiente de pago. Estado actual: " + s.getEstado());
        }
        if (s.getFechaVencimientoPago() != null && LocalDate.now().isAfter(s.getFechaVencimientoPago())) {
            s.setEstado("VENCIDA");
            certificadoRepository.save(s);
            throw new IllegalStateException("El recibo de pago está vencido. Genera una nueva solicitud.");
        }

        s.setEstado("PAGADO");
        s.setFechaPago(LocalDateTime.now());
        s.setObservaciones("Pago confirmado por el sistema.");
        certificadoRepository.save(s);

        // Generación inmediata: PDF + storage + correo + transición a GENERADO.
        generarYNotificar(s.getId());

        SolicitudCertificado actualizada = certificadoRepository.findById(s.getId()).orElse(s);
        Usuario estudiante = usuarioRepository.findByCedula(cedula).orElse(null);
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(actualizada.getTipoCertificado()).orElse(null);
        return construirRespuesta(actualizada, estudiante, tipo);
    }

    // FIX TP-199 (Johan Bueno, 07/10/2026): número de intentos y espera
    // base del backoff exponencial para la generación del PDF. 3 intentos
    // con 1s, 2s, 4s cubren fallos transitorios (Supabase/plantilla) sin
    // dejar al usuario esperando demasiado.
    private static final int PDF_MAX_INTENTOS = 3;
    private static final long PDF_BACKOFF_BASE_MS = 1_000L;

    /**
     * Genera el PDF, lo sube a storage, calcula hash y envía el correo.
     *
     * FIX TP-199 (Johan Bueno, 07/10/2026): si la generación falla, se
     * reintenta hasta PDF_MAX_INTENTOS veces con backoff exponencial.
     * Antes de generar la solicitud queda en EN_GENERACION_PDF (estado
     * intermedio visible al estudiante). Si los reintentos se agotan, la
     * solicitud queda en GENERACION_FALLIDA y se envía una notificación
     * al administrador POS001 para que la relance con el endpoint de
     * reintento manual. Antes el service lanzaba una excepción y la
     * solicitud quedaba en PAGADO, sin estado intermedio, sin reintento
     * y sin aviso (CP-038).
     *
     * Punto de extensión para firma digital: aplicar la firma sobre
     * `pdfBytes` antes de calcular el hash y subirlo a storage. Si la
     * firma falla, NO se sube el PDF y la solicitud queda en PAGADO para
     * reintento.
     */
    public void generarYNotificar(Long solicitudId) {
        SolicitudCertificado s = certificadoRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con id: " + solicitudId));

        // Permitimos arrancar desde PAGADO (flujo normal) y desde
        // GENERACION_FALLIDA (reintento manual del administrador).
        if (!"PAGADO".equals(s.getEstado()) && !"GENERACION_FALLIDA".equals(s.getEstado())) {
            log.warn("generarYNotificar invocado con estado {} para id {}", s.getEstado(), solicitudId);
            return;
        }

        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado())
            .orElseThrow(() -> new IllegalStateException("Tipo de certificado no existe: " + s.getTipoCertificado()));
        Usuario estudiante = usuarioRepository.findByCedula(s.getCedula()).orElse(null);

        // Estado intermedio mientras los intentos están en curso.
        s.setEstado("EN_GENERACION_PDF");
        s.setObservaciones("Generando certificado…");
        certificadoRepository.save(s);

        byte[] pdfBytes = intentarGenerarPdf(tipo, s, estudiante, solicitudId);
        if (pdfBytes == null) {
            marcarGeneracionFallida(s, estudiante, tipo);
            return;
        }

        String hash = sha256Hex(pdfBytes);
        String path = "certificados/" + s.getCedula() + "/constancia-" + s.getId() + ".pdf";
        try {
            storage.subir(path, pdfBytes, "application/pdf");
            s.setUrlPdf(path);
        } catch (Exception e) {
            log.warn("[CONSTANCIA] Fallback: storage no disponible para solicitud {}: {}", solicitudId, e.getMessage());
            // Sin storage configurado: continuar — el PDF se puede re-generar bajo demanda.
        }

        s.setHashPdf(hash);
        s.setEstado("GENERADO");
        s.setFechaGeneracion(LocalDateTime.now());
        s.setObservaciones("Certificado generado.");
        certificadoRepository.save(s);

        // Notificar a Posgrados si es física para que lo imprima
        if ("FISICA".equals(s.getModalidadEnvio())) {
            notificacionService.crear("POS001", "NUEVO_TRAMITE",
                "Nuevo certificado por imprimir",
                "Se ha generado un certificado físico para " + (estudiante != null ? estudiante.getNombre() : s.getCedula()),
                "/tramites"
            );
        } else {
            notificacionService.crear(s.getCedula(), "ESTADO_CAMBIADO", 
                "Certificado generado", 
                "Tu certificado " + tipo.getLabel() + " ya está disponible para descargar.", 
                "/certificados"
            );
        }

        try {
            correoService.enviarConstancia(estudiante, tipo, s, pdfBytes);
        } catch (Exception e) {
            log.error("[CONSTANCIA] PDF generado pero correo falló para solicitud {}: {}", solicitudId, e.getMessage());
            // No revertimos el estado: el PDF existe y es descargable.
        }
    }

    // FIX TP-199 (Johan Bueno, 07/10/2026): intento con backoff
    // exponencial. Devuelve los bytes del PDF si alguno de los
    // PDF_MAX_INTENTOS logra generarlo; null si todos fallan.
    private byte[] intentarGenerarPdf(TipoCertificado tipo, SolicitudCertificado s,
                                      Usuario estudiante, Long solicitudId) {
        Exception ultimo = null;
        for (int intento = 1; intento <= PDF_MAX_INTENTOS; intento++) {
            try {
                return generarPdfConstancia(tipo, s, estudiante);
            } catch (Exception e) {
                ultimo = e;
                log.warn("[CONSTANCIA] Intento {}/{} fallido para solicitud {}: {}",
                        intento, PDF_MAX_INTENTOS, solicitudId, e.getMessage());
                if (intento < PDF_MAX_INTENTOS) {
                    try {
                        long espera = PDF_BACKOFF_BASE_MS * (1L << (intento - 1)); // 1s, 2s
                        Thread.sleep(espera);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        log.error("[CONSTANCIA] PDF falló tras {} intentos para solicitud {}: {}",
                PDF_MAX_INTENTOS, solicitudId, ultimo != null ? ultimo.getMessage() : "sin detalle");
        return null;
    }

    // FIX TP-199 (Johan Bueno, 07/10/2026): cuando los reintentos se
    // agotan, la solicitud pasa a GENERACION_FALLIDA y se notifica al
    // administrador POS001 (patrón que ya usa generarYNotificar para los
    // físicos). El administrador puede relanzar con el endpoint
    // POST /api/certificados/{id}/reintentar-pdf.
    private void marcarGeneracionFallida(SolicitudCertificado s, Usuario estudiante, TipoCertificado tipo) {
        s.setEstado("GENERACION_FALLIDA");
        s.setObservaciones("No se pudo generar el PDF tras " + PDF_MAX_INTENTOS
                + " intentos. Pendiente de intervención del administrador.");
        certificadoRepository.save(s);
        notificacionService.crear("POS001", "FALLO_GENERACION",
                "Fallo generando certificado",
                "El certificado " + (tipo != null ? tipo.getLabel() : s.getTipoCertificado())
                        + " del estudiante " + (estudiante != null ? estudiante.getNombre() : s.getCedula())
                        + " (solicitud " + s.getId() + ") no se pudo generar automáticamente.",
                "/tramites");
    }

    // FIX TP-199 (Johan Bueno, 07/10/2026): reintento manual disparado
    // desde la bandeja de Posgrados cuando una solicitud quedó en
    // GENERACION_FALLIDA. Reusa el flujo normal pasando por
    // generarYNotificar, que acepta GENERACION_FALLIDA como punto de
    // entrada además de PAGADO.
    public Map<String, Object> reintentarGeneracionPdf(Long solicitudId) {
        SolicitudCertificado s = certificadoRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con id: " + solicitudId));
        if (!"GENERACION_FALLIDA".equals(s.getEstado())) {
            throw new IllegalStateException(
                "El reintento solo aplica cuando la solicitud está en GENERACION_FALLIDA. "
                + "Estado actual: " + s.getEstado());
        }
        generarYNotificar(solicitudId);
        SolicitudCertificado actualizada = certificadoRepository.findById(solicitudId).orElse(s);
        Usuario estudiante = usuarioRepository.findByCedula(actualizada.getCedula()).orElse(null);
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(actualizada.getTipoCertificado()).orElse(null);
        return construirRespuesta(actualizada, estudiante, tipo);
    }

    private byte[] generarPdfConstancia(TipoCertificado tipo, SolicitudCertificado s, Usuario estudiante) throws Exception {
        if (tipo.getPlantillaHtml() != null && !tipo.getPlantillaHtml().isBlank()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "CO"));
            String fechaExpedicion = LocalDate.now().format(fmt);
            String codigoVerif = "UFPS-CERT-" + s.getId() + "-"
                    + s.getCedula().substring(Math.max(0, s.getCedula().length() - 4));
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("nombre_completo", estudiante != null ? estudiante.getNombre() : "—");
            vars.put("cedula", s.getCedula());
            vars.put("codigo_estudiantil", estudiante != null ? estudiante.getCodigo() : "—");
            vars.put("programa", estudiante != null && estudiante.getProgramaAcademico() != null
                    ? estudiante.getProgramaAcademico().getNombre() : "—");
            vars.put("tipo_certificado", tipo.getLabel());
            vars.put("fecha_expedicion", fechaExpedicion);
            vars.put("fecha_aprobacion", fechaExpedicion);
            vars.put("numero_solicitud", String.valueOf(s.getId()));
            vars.put("codigo_verificacion", codigoVerif);
            vars.put("dependencia", "Oficina de Posgrados");
            String html = plantillaService.aplicarVariables(tipo.getPlantillaHtml(), vars);
            return plantillaService.renderizarPdf(html);
        }
        return pdfService.generar(tipo, s, estudiante);
    }

    // ── 4) DESCARGA DEL PDF ───────────────────────────────────────────────────

    /**
     * Descarga el PDF de la solicitud. Acepta tanto al dueño (estudiante por cédula)
     * como a la dependencia encargada (por id de Dependencia). Un caller solo
     * llenará uno de los dos parámetros según su rol; el otro será null.
     */
    public byte[] descargarPdf(Long solicitudId, String cedulaEstudianteActor, boolean esPosgrados) {
        SolicitudCertificado s = certificadoRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        boolean esDueno = cedulaEstudianteActor != null && cedulaEstudianteActor.equals(s.getCedula());
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);
        
        if (!esDueno && !esPosgrados) {
            throw new IllegalStateException("No autorizado para descargar este certificado.");
        }

        if (!estadoPermiteDescarga(s.getEstado())) {
            throw new IllegalStateException("El certificado todavía no está generado.");
        }

        if (s.getUrlPdf() != null) {
            byte[] bytes = storage.descargar(s.getUrlPdf());
            if (bytes != null) return bytes;
        }

        // Fallback: regenerar el PDF al vuelo si storage no devolvió nada.
        Usuario estudiante = usuarioRepository.findByCedula(s.getCedula()).orElse(null);
        try {
            return pdfService.generar(tipo, s, estudiante);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo recuperar el certificado: " + e.getMessage());
        }
    }

    private boolean estadoPermiteDescarga(String estado) {
        return "GENERADO".equals(estado) || "LISTO_RETIRO".equals(estado) || "ENTREGADO".equals(estado);
    }

    // ── 5) FLUJO DE POSGRADOS (FÍSICOS) ─────────────────────────────────

    public List<Map<String, Object>> obtenerBandejaPosgrados(String estadoFiltro) {
        return obtenerBandejaPosgrados(estadoFiltro, null);
    }

    // FIX TP-200 (Johan Bueno, 07/10/2026): la bandeja de Posgrados ahora
    // incluye los certificados digitales además de los físicos y acepta
    // búsqueda por identificación (coincidencia parcial). Antes sólo
    // mostraba FISICA y no tenía filtro de cédula (CP-042).
    public List<Map<String, Object>> obtenerBandejaPosgrados(String estadoFiltro, String cedulaFiltro) {
        String cedula = (cedulaFiltro != null && !cedulaFiltro.isBlank()) ? cedulaFiltro.trim() : null;
        List<SolicitudCertificado> solicitudes =
            certificadoRepository.findBandejaPosgrados(estadoFiltro, cedula);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (SolicitudCertificado s : solicitudes) {
            Usuario estudiante = usuarioRepository.findByCedula(s.getCedula()).orElse(null);
            TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);
            resultado.add(construirRespuesta(s, estudiante, tipo));
        }
        return resultado;
    }

    public Map<String, Object> marcarListoRetiro(Long solicitudId) {
        SolicitudCertificado s = certificadoRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);

        if (!"FISICA".equals(s.getModalidadEnvio())) {
            throw new IllegalStateException("Esta solicitud no es de modalidad física.");
        }
        if (!"GENERADO".equals(s.getEstado())) {
            throw new IllegalStateException(
                "Solo se puede marcar como listo un certificado en estado GENERADO. Estado actual: " + s.getEstado());
        }
        s.setEstado("LISTO_RETIRO");
        s.setObservaciones("Documento físico listo para retiro en oficina.");
        certificadoRepository.save(s);

        Usuario estudiante = usuarioRepository.findByCedula(s.getCedula()).orElse(null);
        notificacionService.crear(s.getCedula(), "ESTADO_CAMBIADO", 
            "Certificado listo para retiro", 
            "Tu certificado " + (tipo != null ? tipo.getLabel() : "físico") + " está listo para que lo retires en la Oficina de Posgrados.", 
            "/certificados"
        );

        try {
            correoService.enviarAvisoListoRetiro(estudiante, tipo, s);
        } catch (Exception e) {
            log.error("[CONSTANCIA] Error enviando aviso de retiro: {}", e.getMessage());
        }
        return construirRespuesta(s, estudiante, tipo);
    }

    public Map<String, Object> marcarEntregado(Long solicitudId) {
        SolicitudCertificado s = certificadoRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);

        if (!"LISTO_RETIRO".equals(s.getEstado())) {
            throw new IllegalStateException(
                "Solo se puede marcar como entregado un certificado en estado LISTO_RETIRO. Estado actual: " + s.getEstado());
        }
        s.setEstado("ENTREGADO");
        s.setFechaEntrega(LocalDateTime.now());
        s.setObservaciones("Documento físico entregado al estudiante.");
        certificadoRepository.save(s);
        
        notificacionService.crear(s.getCedula(), "ESTADO_CAMBIADO", 
            "Certificado entregado", 
            "Se ha registrado la entrega física de tu certificado " + (tipo != null ? tipo.getLabel() : "") + ".", 
            "/certificados"
        );

        Usuario estudiante = usuarioRepository.findByCedula(s.getCedula()).orElse(null);
        return construirRespuesta(s, estudiante, tipo);
    }

    // ── 6) SERIALIZACIÓN ──────────────────────────────────────────────────────

    private Map<String, Object> construirRespuesta(SolicitudCertificado s, Usuario estudiante, TipoCertificado tipo) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("tipoCertificado", s.getTipoCertificado());
        map.put("tipoLabel", tipo != null ? tipo.getLabel() : null);
        map.put("modalidadEnvio", s.getModalidadEnvio());
        map.put("estado", s.getEstado());
        map.put("fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null);
        map.put("fechaVencimientoPago", s.getFechaVencimientoPago() != null ? s.getFechaVencimientoPago().toString() : null);
        map.put("fechaPago", s.getFechaPago() != null ? s.getFechaPago().toString() : null);
        map.put("fechaGeneracion", s.getFechaGeneracion() != null ? s.getFechaGeneracion().toString() : null);
        map.put("fechaEntrega", s.getFechaEntrega() != null ? s.getFechaEntrega().toString() : null);
        map.put("costo", s.getCosto());
        map.put("destinatario", s.getDestinatario());
        map.put("observaciones", s.getObservaciones());
        map.put("urlPdf", s.getUrlPdf());
        map.put("hashPdf", s.getHashPdf());
        map.put("liquidacion", construirLiquidacion(s));

        if (tipo != null) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("codigo", tipo.getCodigo());
            t.put("label", tipo.getLabel());
            t.put("descripcion", tipo.getDescripcion());
            t.put("precioDigital", tipo.getPrecioDigital());
            t.put("costoLogisticaFisica", tipo.getCostoLogisticaFisica());
            t.put("direccionOficina", tipo.getDireccionOficina());
            t.put("tiempoEntregaDias", tipo.getTiempoEntregaDias());
            map.put("tipo", t);
        }

        if (estudiante != null) {
            Map<String, Object> est = new LinkedHashMap<>();
            est.put("nombre", estudiante.getNombre());
            est.put("cedula", estudiante.getCedula());
            est.put("codigo", estudiante.getCodigo());
            est.put("correo", estudiante.getCorreo());
            est.put("programa", estudiante.getProgramaAcademico() != null
                    ? estudiante.getProgramaAcademico().getNombre() : null);
            map.put("estudiante", est);
        }
        return map;
    }

    private Map<String, Object> construirLiquidacion(SolicitudCertificado s) {
        Map<String, Object> liq = new LinkedHashMap<>();
        liq.put("concepto", "Certificado Académico — " + s.getTipoCertificado());
        liq.put("valor", s.getCosto());
        liq.put("fechaLimite", s.getFechaVencimientoPago() != null ? s.getFechaVencimientoPago().toString() : null);
        liq.put("instrucciones",
                "Realiza el pago por PSE o en la ventanilla de Tesorería antes de la fecha límite.");
        return liq;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
