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
import com.ufps.tramites.repository.SolicitudRepository;

@ExtendWith(MockitoExtension.class)
class WompiServiceTest {

    private static final String EVENTS_SECRET = "test_events_secret";

    @Mock private PagoRepository pagoRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private SolicitudService solicitudService;

    @InjectMocks
    private WompiService wompiService;

    private Pago pago;

    @BeforeEach
    void setUp() {
        // Los campos @Value no los resuelve Spring en un test unitario puro
        ReflectionTestUtils.setField(wompiService, "eventsSecret", EVENTS_SECRET);

        pago = new Pago();
        pago.setReferencia("UFPS-TER-1-ABCD1234");
        pago.setSolicitudId(1L);
        pago.setTipoPago("TERMINACION");
        pago.setMontoCentavos(15_000_000L);
        pago.setEstado("PENDIENTE");
    }

    @Test
    void firmaValida_avanzaElEstadoDelPago() {
        String referencia = pago.getReferencia();
        String transactionId = "txn-123";
        String status = "APPROVED";
        long amountInCents = 15_000_000L;
        String checksumValido = sha256(transactionId + status + amountInCents + EVENTS_SECRET);

        Map<String, Object> evento = construirEvento(referencia, transactionId, status, amountInCents, checksumValido);

        when(pagoRepository.findByReferencia(referencia)).thenReturn(Optional.of(pago));

        wompiService.procesarWebhook(evento);

        assertThat(pago.getEstado()).isEqualTo("APROBADO");
        assertThat(pago.getWompiTransactionId()).isEqualTo(transactionId);
        verify(pagoRepository).save(pago);
        verify(solicitudService).registrarPagoTerminacion(1L);
    }

    @Test
    void firmaInvalida_noModificaElPago() {
        String referencia = pago.getReferencia();
        String transactionId = "txn-123";
        String status = "APPROVED";
        long amountInCents = 15_000_000L;
        String checksumInvalido = "checksum-manipulado";

        Map<String, Object> evento = construirEvento(referencia, transactionId, status, amountInCents, checksumInvalido);

        wompiService.procesarWebhook(evento);

        assertThat(pago.getEstado()).isEqualTo("PENDIENTE");
        assertThat(pago.getWompiTransactionId()).isNull();
        verify(pagoRepository, never()).findByReferencia(any());
        verify(pagoRepository, never()).save(any());
        verify(solicitudService, never()).registrarPagoTerminacion(any());
        verify(solicitudService, never()).registrarPagoGrado(any());
        verify(solicitudService, never()).registrarPagoModalidad(any());
    }

    private Map<String, Object> construirEvento(String referencia, String transactionId,
            String status, long amountInCents, String checksum) {
        Map<String, Object> transaction = new LinkedHashMap<>();
        transaction.put("id", transactionId);
        transaction.put("status", status);
        transaction.put("amount_in_cents", amountInCents);
        transaction.put("reference", referencia);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transaction", transaction);

        Map<String, Object> signature = new LinkedHashMap<>();
        signature.put("checksum", checksum);

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("event", "transaction.updated");
        evento.put("data", data);
        evento.put("signature", signature);
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
