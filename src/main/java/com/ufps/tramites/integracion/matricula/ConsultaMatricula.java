package com.ufps.tramites.integracion.matricula;

import java.util.Optional;

/**
 * Resultado de consultar al Módulo de Matrícula. Nunca lanza excepción hacia el
 * llamador: las caídas se expresan como {@link Resultado#NO_DISPONIBLE} para que
 * el trámite se bloquee con un mensaje claro en lugar de usar datos simulados
 * (RF16, flujos alternos).
 */
public record ConsultaMatricula(Resultado resultado, SituacionAcademica situacion, String mensaje) {

    public enum Resultado {
        /** 200 y la respuesta cumple el contrato. */
        ENCONTRADO,
        /** 404: la cédula no figura como admitida y legalizada. */
        NO_ENCONTRADO,
        /** La cédula no es numérica o viene vacía (400 o validación local). */
        CEDULA_INVALIDA,
        /** Timeout, 5xx o credencial rechazada tras reintentar. */
        NO_DISPONIBLE,
        /** 200 pero el cuerpo no cumple el contrato. */
        RESPUESTA_INVALIDA
    }

    static ConsultaMatricula encontrado(SituacionAcademica situacion) {
        return new ConsultaMatricula(Resultado.ENCONTRADO, situacion, null);
    }

    static ConsultaMatricula noEncontrado() {
        return new ConsultaMatricula(Resultado.NO_ENCONTRADO, null,
                "El estudiante no aparece como admitido y legalizado en el Módulo de Matrícula.");
    }

    static ConsultaMatricula cedulaInvalida() {
        return new ConsultaMatricula(Resultado.CEDULA_INVALIDA, null,
                "La cédula consultada no es válida.");
    }

    static ConsultaMatricula noDisponible() {
        return new ConsultaMatricula(Resultado.NO_DISPONIBLE, null,
                "La validación académica no está disponible en este momento. Intenta más tarde.");
    }

    static ConsultaMatricula respuestaInvalida() {
        return new ConsultaMatricula(Resultado.RESPUESTA_INVALIDA, null,
                "No fue posible validar la información académica. Intenta más tarde.");
    }

    public boolean encontrado() {
        return resultado == Resultado.ENCONTRADO;
    }

    public Optional<SituacionAcademica> situacionOpcional() {
        return Optional.ofNullable(situacion);
    }
}
