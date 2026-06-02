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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.ufps.tramites.event.CertificadoEstadoCambiadoEvent;
import com.ufps.tramites.model.Estudiante;
import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.model.TipoCertificado;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.SolicitudCertificadoRepository;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.repository.UsuarioRepository;

@Service
public class CertificadoService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoService.class);
    private static final int DIAS_VIGENCIA_PAGO = 3;

    @Autowired private SolicitudCertificadoRepository certificadoRepository;
    @Autowired private TipoCertificadoRepository tipoCertificadoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private CertificadoConstanciaPdfService pdfService;
    @Autowired private CorreoConstanciaService correoService;
    @Autowired private SupabaseStorageService storage;
    @Autowired private ApplicationEventPublisher eventPublisher;

    // ── 1) SOLICITUD ──────────────────────────────────────────────────────────

    public Map<String, Object> solicitarCertificado(Usuario estudiante,
                                                    String tipoCertificado,
                                                    String modalidadEnvio,
                                                    String destinatario) {
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(tipoCertificado)
            .orElseThrow(() -> new IllegalStateException("Tipo de certificado inválido: " + tipoCertificado));

        if (Boolean.FALSE.equals(tipo.getActivo()))
            throw new IllegalStateException("Este tipo de certificado no está disponible actualmente.");

        if (!"FISICA".equals(modalidadEnvio) && !"DIGITAL".equals(modalidadEnvio))
            throw new IllegalStateException("Modalidad de envío inválida: " + modalidadEnvio);

        List<SolicitudCertificado> existentes = certificadoRepository
            .findByCedulaAndTipoCertificado(estudiante.getCedula(), tipoCertificado);
        boolean tieneVigente = existentes.stream()
            .anyMatch(s -> "PENDIENTE_PAGO".equals(s.getEstado()));
        if (tieneVigente)
            throw new IllegalStateException(
                "Ya tienes una solicitud vigente de este tipo de certificado. " +
                "Debes pagar o esperar a que venza el recibo antes de generar uno nuevo.");

        LocalDate hoy = LocalDate.now();
        double costo = tipo.precioTotal(modalidadEnvio);

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

    // ── 3) PAGO + GENERACIÓN INMEDIATA ────────────────────────────────────────

    public Map<String, Object> simularPago(Long id, String cedula) {
        SolicitudCertificado s = certificadoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con id: " + id));

        if (!cedula.equals(s.getCedula()))
            throw new IllegalStateException("Esta solicitud no pertenece al estudiante.");
        if (!"PENDIENTE_PAGO".equals(s.getEstado()))
            throw new IllegalStateException("Esta solicitud no está pendiente de pago. Estado actual: " + s.getEstado());
        if (s.getFechaVencimientoPago() != null && LocalDate.now().isAfter(s.getFechaVencimientoPago())) {
            s.setEstado("VENCIDA");
            certificadoRepository.save(s);
            throw new IllegalStateException("El recibo de pago está vencido. Genera una nueva solicitud.");
        }

        String estadoAnterior = s.getEstado();
        s.setEstado("PAGADO");
        s.setFechaPago(LocalDateTime.now());
        s.setObservaciones("Pago confirmado por el sistema.");
        certificadoRepository.save(s);

        // Publicar evento PENDIENTE_PAGO → PAGADO
        Usuario estudianteEvento = usuarioRepository.findByCedula(cedula).orElse(null);
        publicarCambioEstado(s, estadoAnterior, estudianteEvento);

        // Generación inmediata
        generarYNotificar(s.getId());

        SolicitudCertificado actualizada = certificadoRepository.findById(s.getId()).orElse(s);
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(actualizada.getTipoCertificado()).orElse(null);
        return construirRespuesta(actualizada, estudianteEvento, tipo);
    }

    public void generarYNotificar(Long solicitudId) {
        SolicitudCertificado s = certificadoRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con id: " + solicitudId));

        if (!"PAGADO".equals(s.getEstado())) {
            log.warn("generarYNotificar invocado con estado {} para id {}", s.getEstado(), solicitudId);
            return;
        }

        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado())
            .orElseThrow(() -> new IllegalStateException("Tipo de certificado no existe: " + s.getTipoCertificado()));
        Usuario estudiante = usuarioRepository.findByCedula(s.getCedula()).orElse(null);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generar(tipo, s, estudiante);
        } catch (Exception e) {
            log.error("[CONSTANCIA] Error generando PDF para solicitud {}: {}", solicitudId, e.getMessage());
            throw new IllegalStateException("No se pudo generar el certificado PDF. Intenta de nuevo en unos minutos.");
        }

        String hash = sha256Hex(pdfBytes);
        String path = "certificados/" + s.getCedula() + "/constancia-" + s.getId() + ".pdf";
        try {
            storage.subir(path, pdfBytes, "application/pdf");
            s.setUrlPdf(path);
        } catch (Exception e) {
            log.warn("[CONSTANCIA] Fallback: storage no disponible para solicitud {}: {}", solicitudId, e.getMessage());
        }

        String estadoAnterior = s.getEstado();
        s.setHashPdf(hash);
        s.setEstado("GENERADO");
        s.setFechaGeneracion(LocalDateTime.now());
        s.setObservaciones("Certificado generado y notificado por correo.");
        certificadoRepository.save(s);

        // Publicar evento PAGADO → GENERADO
        publicarCambioEstado(s, estadoAnterior, estudiante);

        try {
            correoService.enviarConstancia(estudiante, tipo, s, pdfBytes);
        } catch (Exception e) {
            log.error("[CONSTANCIA] PDF generado pero correo falló para solicitud {}: {}", solicitudId, e.getMessage());
        }
    }

    // ── 4) DESCARGA DEL PDF ───────────────────────────────────────────────────

    public byte[] descargarPdf(Long solicitudId, String cedulaEstudianteActor, Long dependenciaIdActor) {
        SolicitudCertificado s = certificadoRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        boolean esDueno = cedulaEstudianteActor != null && cedulaEstudianteActor.equals(s.getCedula());
        boolean esDependenciaEncargada = false;
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);
        if (tipo != null && tipo.getDependencia() != null
                && dependenciaIdActor != null
                && tipo.getDependencia().getId().equals(dependenciaIdActor)) {
            esDependenciaEncargada = true;
        }
        if (!esDueno && !esDependenciaEncargada)
            throw new IllegalStateException("No autorizado para descargar este certificado.");
        if (!estadoPermiteDescarga(s.getEstado()))
            throw new IllegalStateException("El certificado todavía no está generado.");

        if (s.getUrlPdf() != null) {
            byte[] bytes = storage.descargar(s.getUrlPdf());
            if (bytes != null) return bytes;
        }

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

    // ── 5) FLUJO DE LA DEPENDENCIA (FÍSICOS) ─────────────────────────────────

    public List<Map<String, Object>> obtenerPorDependencia(Long dependenciaId, String estadoFiltro) {
        List<SolicitudCertificado> solicitudes =
            certificadoRepository.findByDependencia(dependenciaId, estadoFiltro);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (SolicitudCertificado s : solicitudes) {
            Usuario estudiante = usuarioRepository.findByCedula(s.getCedula()).orElse(null);
            TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);
            resultado.add(construirRespuesta(s, estudiante, tipo));
        }
        return resultado;
    }

    public Map<String, Object> marcarListoRetiro(Long solicitudId, Long dependenciaId) {
        SolicitudCertificado s = certificadoRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);
        validarDependenciaEncargada(tipo, dependenciaId);

        if (!"FISICA".equals(s.getModalidadEnvio()))
            throw new IllegalStateException("Esta solicitud no es de modalidad física.");
        if (!"GENERADO".equals(s.getEstado()))
            throw new IllegalStateException(
                "Solo se puede marcar como listo un certificado en estado GENERADO. Estado actual: " + s.getEstado());

        String estadoAnterior = s.getEstado();
        s.setEstado("LISTO_RETIRO");
        s.setObservaciones("Documento físico listo para retiro en oficina.");
        certificadoRepository.save(s);

        Usuario estudiante = usuarioRepository.findByCedula(s.getCedula()).orElse(null);

        // Publicar evento GENERADO → LISTO_RETIRO
        publicarCambioEstado(s, estadoAnterior, estudiante);

        try {
            correoService.enviarAvisoListoRetiro(estudiante, tipo, s);
        } catch (Exception e) {
            log.error("[CONSTANCIA] Error enviando aviso de retiro: {}", e.getMessage());
        }
        return construirRespuesta(s, estudiante, tipo);
    }

    public Map<String, Object> marcarEntregado(Long solicitudId, Long dependenciaId) {
        SolicitudCertificado s = certificadoRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        TipoCertificado tipo = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);
        validarDependenciaEncargada(tipo, dependenciaId);

        if (!"LISTO_RETIRO".equals(s.getEstado()))
            throw new IllegalStateException(
                "Solo se puede marcar como entregado un certificado en estado LISTO_RETIRO. Estado actual: " + s.getEstado());

        String estadoAnterior = s.getEstado();
        s.setEstado("ENTREGADO");
        s.setFechaEntrega(LocalDateTime.now());
        s.setObservaciones("Documento físico entregado al estudiante.");
        certificadoRepository.save(s);

        Usuario estudiante = usuarioRepository.findByCedula(s.getCedula()).orElse(null);

        // Publicar evento LISTO_RETIRO → ENTREGADO
        publicarCambioEstado(s, estadoAnterior, estudiante);

        return construirRespuesta(s, estudiante, tipo);
    }

    private void validarDependenciaEncargada(TipoCertificado tipo, Long dependenciaId) {
        if (tipo == null || tipo.getDependencia() == null)
            throw new IllegalStateException("El tipo de certificado no tiene dependencia asignada.");
        if (!tipo.getDependencia().getId().equals(dependenciaId))
            throw new IllegalStateException("Esta dependencia no es responsable de este tipo de certificado.");
    }

    // ── 6) HELPER: PUBLICAR EVENTO ────────────────────────────────────────────

    private void publicarCambioEstado(SolicitudCertificado s, String estadoAnterior, Usuario actor) {
        if (actor == null) return;
        eventPublisher.publishEvent(
            new CertificadoEstadoCambiadoEvent(this, s, actor, estadoAnterior)
        );
    }

    // ── 7) SERIALIZACIÓN ──────────────────────────────────────────────────────

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
            t.put("dependenciaId",     tipo.getDependenciaId());
            t.put("dependenciaNombre", tipo.getDependenciaNombre());
            t.put("direccionOficina",  tipo.getDireccionOficina());
            t.put("tiempoEntregaDias", tipo.getTiempoEntregaDias());
            map.put("tipo", t);
        }

        if (estudiante != null) {
            Map<String, Object> est = new LinkedHashMap<>();
            est.put("nombre",  estudiante.getNombre());
            est.put("cedula",  estudiante.getCedula());
            est.put("codigo",  estudiante.getCodigo());
            est.put("correo",  estudiante.getCorreo());
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
        liq.put("instrucciones", "Realiza el pago por PSE o en la ventanilla de Tesorería antes de la fecha límite.");
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