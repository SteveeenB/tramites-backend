package com.ufps.tramites.dto;

/**
 * Avance en créditos de un estudiante frente a los que exige su programa.
 * Cualquiera de los dos valores puede ser null si no está registrado; en ese
 * caso el avance no es evaluable y nunca cuenta como 100 %.
 */
public record AvanceAcademico(Integer aprobados, Integer requeridos) {

    public boolean evaluable() {
        return aprobados != null && requeridos != null && requeridos > 0;
    }

    public boolean cumple() {
        return evaluable() && aprobados >= requeridos;
    }

    public int faltantes() {
        return evaluable() ? Math.max(0, requeridos - aprobados) : 0;
    }

    /** Motivo por el que no cumple, en lenguaje del estudiante; null si cumple. */
    public String mensaje() {
        if (requeridos == null || requeridos <= 0) {
            return "Tu programa no tiene registrados los créditos requeridos. Contacta a la oficina de Posgrados.";
        }
        if (aprobados == null) {
            return "Tus créditos aprobados no están registrados. Contacta a tu director de programa.";
        }
        if (!cumple()) {
            return "Te faltan " + faltantes() + " créditos (llevas " + aprobados + " de " + requeridos + ").";
        }
        return null;
    }
}
