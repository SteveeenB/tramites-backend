package com.ufps.tramites.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ufps.tramites.model.Pago;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.repository.PagoRepository;
import com.ufps.tramites.repository.SolicitudRepository;

@Service
public class WompiService {

    private static final Logger log = LoggerFactory.getLogger(WompiService.class);

    @Value("${wompi.public-key}")
    private String publicKey;

    @Value("${wompi.integrity-key}")
    private String integrityKey;

    @Value("${wompi.events-secret}")
    private String eventsSecret;

    @Value("${wompi.checkout-url:https://checkout.wompi.co/p/}")
    private String checkoutUrl;

    @Value("${wompi.redirect-url:http://localhost:3000/pago/resultado}")
    private String redirectUrl;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private SolicitudService solicitudService;

    // ─────────────────────────────────────────────────────────────────────
    // CREAR PAGO
    // ─────────────────────────────────────────────────────────────────────
    /**
     * Crea un registro de pago y devuelve la URL de checkout de Wompi.
     *
     * @param solicitudId ID de la solicitud a pagar
     * @param cedula Cédula del estudiante
     * @param tipoPago TERMINACION | GRADO | MODALIDAD_CEREMONIA
     * @return URL completa para redirigir al usuario a Wompi
     */
    public Map<String, Object> crearPago(Long solicitudId, String cedula, String tipoPago) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        // Calcular monto según tipo de pago
        long montoCOP = calcularMonto(solicitud, tipoPago);
        long montoCentavos = montoCOP * 100L;

        // Generar referencia única
        String referencia = "UFPS-" + tipoPago.substring(0, 3) + "-" + solicitudId
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Guardar registro del pago
        Pago pago = new Pago();
        pago.setReferencia(referencia);
        pago.setSolicitudId(solicitudId);
        pago.setCedulaEstudiante(cedula);
        pago.setTipoPago(tipoPago);
        pago.setMontoCentavos(montoCentavos);
        pago.setEstado("PENDIENTE");
        String redirectConRef = redirectUrl
            + (redirectUrl.contains("?") ? "&" : "?")
            + "reference=" + referencia;
        pago.setRedirectUrl(redirectConRef);
        pago.setFechaCreacion(LocalDateTime.now());
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);

        // Generar firma de integridad
        String firma = generarFirmaIntegridad(referencia, montoCentavos, "COP");

        // Construir URL de checkout
        String url = checkoutUrl
                + "?public-key=" + publicKey
                + "&currency=COP"
                + "&amount-in-cents=" + montoCentavos
                + "&reference=" + referencia
                + "&signature:integrity=" + firma
            + "&redirect-url=" + URLEncoder.encode(redirectConRef, StandardCharsets.UTF_8)
                + "&customer-data:email=" + URLEncoder.encode(cedula + "@estudiante.ufps.edu.co", StandardCharsets.UTF_8);

        return Map.of(
                "referencia", referencia,
                "checkoutUrl", url,
                "monto", montoCOP,
                "montoCentavos", montoCentavos
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONSULTAR ESTADO
    // ─────────────────────────────────────────────────────────────────────
    public Map<String, Object> consultarEstado(String referencia) {
        Pago pago = pagoRepository.findByReferencia(referencia)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + referencia));
        return Map.of(
                "referencia", pago.getReferencia(),
                "estado", pago.getEstado(),
                "tipoPago", pago.getTipoPago(),
                "solicitudId", pago.getSolicitudId(),
                "monto", pago.getMontoCentavos() / 100L
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // WEBHOOK DE WOMPI
    // ─────────────────────────────────────────────────────────────────────
    /**
     * Procesa el evento de webhook enviado por Wompi. Verifica la firma y
     * actualiza el estado de la solicitud.
     */
    @SuppressWarnings("unchecked")
    public void procesarWebhook(Map<String, Object> evento) {
        try {
            String tipoEvento = (String) evento.get("event");
            if (!"transaction.updated".equals(tipoEvento)) {
                return;
            }

            Map<String, Object> data = (Map<String, Object>) evento.get("data");
            Map<String, Object> transaction = (Map<String, Object>) data.get("transaction");
            Map<String, Object> signature = (Map<String, Object>) evento.get("signature");

            String transactionId = (String) transaction.get("id");
            String status = (String) transaction.get("status");
            Object amountObj = transaction.get("amount_in_cents");
            Long amountInCents = amountObj instanceof Integer
                    ? ((Integer) amountObj).longValue()
                    : (Long) amountObj;
            String referencia = (String) transaction.get("reference");
            String checksum = (String) signature.get("checksum");

            // Verificar firma del webhook
            if (!verificarFirmaWebhook(transactionId, status, amountInCents, checksum)) {
                log.warn("[WOMPI] Firma inválida para transacción {}", transactionId);
                return;
            }

            // Buscar y actualizar el pago
            pagoRepository.findByReferencia(referencia).ifPresent(pago -> {
                pago.setWompiTransactionId(transactionId);
                pago.setEstado(mapearEstadoWompi(status));
                pago.setFechaActualizacion(LocalDateTime.now());
                pagoRepository.save(pago);

                // Si fue aprobado, actualizar la solicitud
                if ("APPROVED".equals(status)) {
                    actualizarSolicitudTrasAprobacion(pago);
                }

                log.info("[WOMPI] Pago {} → {} (solicitud {})", referencia, status, pago.getSolicitudId());
            });

        } catch (Exception e) {
            log.error("[WOMPI] Error procesando webhook: {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────
    private long calcularMonto(Solicitud solicitud, String tipoPago) {
        return switch (tipoPago) {
            case "TERMINACION" ->
                solicitud.getCosto() != null ? solicitud.getCosto().longValue() : 150_000L;
            case "GRADO" ->
                solicitud.getCosto() != null ? solicitud.getCosto().longValue() : 250_000L;
            case "MODALIDAD_CEREMONIA" ->
                40_000L;
            default ->
                throw new IllegalArgumentException("Tipo de pago inválido: " + tipoPago);
        };
    }

    /**
     * SHA256(referencia + montoCentavos + moneda + integrityKey)
     */
    private String generarFirmaIntegridad(String referencia, long montoCentavos, String moneda) {
        try {
            String datos = referencia + montoCentavos + moneda + integrityKey;
            log.info("[WOMPI] Firma input: '{}'", datos); // ← agrega esto
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(datos.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String firma = sb.toString();
            log.info("[WOMPI] Firma generada: '{}'", firma); // ← y esto
            return firma;
        } catch (Exception e) {
            throw new RuntimeException("Error generando firma de integridad", e);
        }
    }

    /**
     * SHA256(transactionId + status + amountInCents + eventsSecret)
     */
    private boolean verificarFirmaWebhook(String transactionId, String status,
            long amountInCents, String checksum) {
        try {
            String datos = transactionId + status + amountInCents + eventsSecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(datos.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equals(checksum);
        } catch (Exception e) {
            log.error("[WOMPI] Error verificando firma webhook: {}", e.getMessage());
            return false;
        }
    }

    private String mapearEstadoWompi(String wompiStatus) {
        return switch (wompiStatus) {
            case "APPROVED" ->
                "APROBADO";
            case "DECLINED" ->
                "RECHAZADO";
            case "VOIDED" ->
                "ANULADO";
            case "ERROR" ->
                "ERROR";
            default ->
                "PENDIENTE";
        };
    }

    private void actualizarSolicitudTrasAprobacion(Pago pago) {
        try {
            switch (pago.getTipoPago()) {
                case "TERMINACION" ->
                    solicitudService.registrarPagoTerminacion(pago.getSolicitudId());
                case "GRADO" ->
                    solicitudService.registrarPagoGrado(pago.getSolicitudId());
                case "MODALIDAD_CEREMONIA" ->
                    solicitudService.registrarPagoModalidad(pago.getSolicitudId());
            }
        } catch (Exception e) {
            log.error("[WOMPI] Error actualizando solicitud {} tras pago: {}",
                    pago.getSolicitudId(), e.getMessage());
        }
    }
}
