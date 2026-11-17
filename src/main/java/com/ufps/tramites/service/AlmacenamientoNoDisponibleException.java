package com.ufps.tramites.service;

/**
 * El almacenamiento de documentos (Supabase Storage) no respondió: dominio que no resuelve,
 * proyecto pausado, tiempo de espera o error HTTP. Se responde 503 con un mensaje legible
 * en vez de un 500 sin explicación (hallazgo de la regresión conjunta, TP-156).
 */
public class AlmacenamientoNoDisponibleException extends RuntimeException {
    public AlmacenamientoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
