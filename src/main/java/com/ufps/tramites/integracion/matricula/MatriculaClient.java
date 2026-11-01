package com.ufps.tramites.integracion.matricula;

import java.util.regex.Pattern;

/**
 * Puerto hacia el Módulo de Matrícula (SC-03). Hay dos implementaciones,
 * elegidas con {@code matricula.modo}:
 * <ul>
 *   <li>{@code http} — {@link MatriculaHttpClient}, consume el API real.</li>
 *   <li>{@code mock} — {@link MatriculaMockClient}, datos fijos para desarrollo
 *       mientras el servicio real no esté disponible.</li>
 * </ul>
 */
public interface MatriculaClient {

    /** Cédula como texto, solo dígitos y sin puntos (contrato v0.1, convenciones). */
    Pattern CEDULA_VALIDA = Pattern.compile("\\d{5,15}");

    /** Operación C1 del contrato: situación académica por cédula. */
    ConsultaMatricula consultarSituacion(String cedula);

    /** Operación C2 del contrato: comprobar disponibilidad antes de mostrar trámites. */
    boolean estaDisponible();

    static boolean cedulaValida(String cedula) {
        return cedula != null && CEDULA_VALIDA.matcher(cedula).matches();
    }
}
