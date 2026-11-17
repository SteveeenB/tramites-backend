package com.ufps.tramites.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.ufps.tramites.config.GlobalExceptionHandler;

/** Regresión TP-156: si Supabase no resuelve, se responde 503 con mensaje y no 500. */
class SupabaseStorageServiceErrorTest {

    @Test
    void subir_conDominioInexistente_lanzaAlmacenamientoNoDisponible() {
        SupabaseStorageService svc = new SupabaseStorageService();
        ReflectionTestUtils.setField(svc, "supabaseUrl", "https://supabase-inexistente.invalid");
        ReflectionTestUtils.setField(svc, "serviceRoleKey", "llave-falsa");
        ReflectionTestUtils.setField(svc, "bucket", "tramites-documentos");
        svc.init();

        assertThrows(AlmacenamientoNoDisponibleException.class,
                () -> svc.subir("1/foto.png", new byte[] {1, 2, 3}, "image/png"));
    }

    @Test
    void manejador_responde503ConMensaje() {
        ResponseEntity<Map<String, String>> r = new GlobalExceptionHandler()
                .manejarAlmacenamientoNoDisponible(new AlmacenamientoNoDisponibleException(
                        "No se pudo guardar el documento en el almacenamiento. Intente más tarde.", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
        assertEquals("No se pudo guardar el documento en el almacenamiento. Intente más tarde.",
                r.getBody().get("error"));
    }
}
