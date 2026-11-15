package com.ufps.tramites.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ufps.tramites.model.Pago;
import com.ufps.tramites.repository.PagoRepository;
import com.ufps.tramites.repository.SolicitudCertificadoRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.TipoSolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;

/**
 * FIX TP-195 (Kevin Arias, 02/11/2026): se integra desde la rama "Test" y se
 * adapta al código actual de WompiService. La firma del webhook de Wompi es
 * SHA256(transactionId + status + amountInCents + timestamp + eventsSecret):
 * la versión original de la rama "Test" no incluía el timestamp.
 *
 * Cubre la seguridad del webhook de pagos: solo un evento con firma válida
 * puede cambiar el estado de un pago y avanzar la solicitud asociada.
 */
@ExtendWith(MockitoExtension.class)
class WompiServiceTest {

    private static final String EVENTS_SECRET = "test_events_secret";
    private static final long TIMESTAMP = 1_730_000_000L;

    @Mock private PagoRepository pagoRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private SolicitudCertificadoRepository certificadoRepository;
    @Mock private SolicitudService solicitudService;
    @Mock private CertificadoService certificadoService;
    @Mock private CorreoSolicitudService correoSolicitudService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TipoSolicitudRepository tipoSolicitudRepository;

    @InjectMocks
    private WompiService wompiService;

    private Pago pago;

    @BeforeEach
    void setUp() {
        // Los campos @Value no los resuelve Spring en un test unitario puro.
        ReflectionTestUtils.setField(wompiService, "eventsSecret", EVENTS_SECRET);

        pago = new Pago();
        pago.setReferencia("UFPS-TER-1-ABCD1234");
        pago.setSolicitudId(1L);
        pago.setTipoPago("TERMINACION");
        pago.setMontoCentavos(15_000_000L);
        pago.setEstado("PENDIENTE");
    }

    @Test
    void firmaValida_pagoAprobado_avanzaElEstadoDelPagoYLaSolicitud() {
        Map<String, Object> evento = eventoFirmado("txn-123", "APPROVED", 15_000_000L, TIMESTAMP);
        when(pagoRepository.findByReferencia(pago.getReferencia())).thenReturn(Optional.of(pago));

        wompiService.procesarWebhook(evento);

        assertThat(pago.getEstado()).isEqualTo("APROBADO");
        assertThat(pago.getWompiTransactionId()).isEqualTo("txn-123");
        verify(pagoRepository).save(pago);
        verify(solicitudService).registrarPagoTerminacion(1L);
    }

    @Test
    void firmaValida_pagoRechazado_actualizaElPagoPeroNoAvanzaLaSolicitud() {
        Map<String, Object> evento = eventoFirmado("txn-456", "DECLINED", 15_000_000L, TIMESTAMP);
        when(pagoRepository.findByReferencia(pago.getReferencia())).thenReturn(Optional.of(pago));

        wompiService.procesarWebhook(evento);

        assertThat(pago.getEstado()).isEqualTo("RECHAZADO");
        verify(pagoRepository).save(pago);
        verify(solicitudService, never()).registrarPagoTerminacion(any());
    }

    @Test
    void firmaInvalida_noModificaElPago() {
        Map<String, Object> evento = construirEvento(
                "txn-123", "APPROVED", 15_000_000L, TIMESTAMP, "checksum-manipulado");

        wompiService.procesarWebhook(evento);

        assertThat(pago.getEstado()).isEqualTo("PENDIENTE");
        assertThat(pago.getWompiTransactionId()).isNull();
        verify(pagoRepository, never()).findByReferencia(any());
        verify(pagoRepository, never()).save(any());
        verify(solicitudService, never()).registrarPagoTerminacion(any());
        verify(solicitudService, never()).registrarPagoGrado(any());
        verify(solicitudService, never()).registrarPagoModalidad(any());
    }

    @Test
    void firmaCalculadaConOtroTimestamp_esRechazada() {
        // Un atacante que reutilice el checksum de otro evento (otro timestamp)
        // no debe poder aprobar el pago.
        String checksumDeOtroEvento = sha256("txn-123" + "APPROVED" + 15_000_000L + 999L + EVENTS_SECRET);
        Map<String, Object> evento = construirEvento(
                "txn-123", "APPROVED", 15_000_000L, TIMESTAMP, checksumDeOtroEvento);

        wompiService.procesarWebhook(evento);

        assertThat(pago.getEstado()).isEqualTo("PENDIENTE");
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void eventoDeOtroTipo_seIgnora() {
        Map<String, Object> evento = eventoFirmado("txn-123", "APPROVED", 15_000_000L, TIMESTAMP);
        evento.put("event", "nequi_token.updated");

        wompiService.procesarWebhook(evento);

        verify(pagoRepository, never()).findByReferencia(any());
        verify(pagoRepository, never()).save(any());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private Map<String, Object> eventoFirmado(String transactionId, String status,
            long amountInCents, long timestamp) {
        String checksum = sha256(transactionId + status + amountInCents + timestamp + EVENTS_SECRET);
        return construirEvento(transactionId, status, amountInCents, timestamp, checksum);
    }

    private Map<String, Object> construirEvento(String transactionId, String status,
            long amountInCents, long timestamp, String checksum) {
        Map<String, Object> transaction = new LinkedHashMap<>();
        transaction.put("id", transactionId);
        transaction.put("status", status);
        transaction.put("amount_in_cents", amountInCents);
        transaction.put("reference", pago.getReferencia());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transaction", transaction);

        Map<String, Object> signature = new LinkedHashMap<>();
        signature.put("checksum", checksum);

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("event", "transaction.updated");
        evento.put("data", data);
        evento.put("signature", signature);
        evento.put("timestamp", timestamp);
        return evento;
    }

    private String sha256(String datos) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(datos.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
