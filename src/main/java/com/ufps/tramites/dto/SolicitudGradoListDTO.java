package com.ufps.tramites.dto;

import java.util.List;
import java.util.Map;

public class SolicitudGradoListDTO {
    private List<Map<String, Object>> pendientes;
    private List<Map<String, Object>> aprobadas;
    private List<Map<String, Object>> rechazadas;

    public SolicitudGradoListDTO() {}

    public SolicitudGradoListDTO(List<Map<String, Object>> pendientes,
                                 List<Map<String, Object>> aprobadas,
                                 List<Map<String, Object>> rechazadas) {
        this.pendientes = pendientes;
        this.aprobadas = aprobadas;
        this.rechazadas = rechazadas;
    }

    public List<Map<String, Object>> getPendientes() { return pendientes; }
    public void setPendientes(List<Map<String, Object>> pendientes) { this.pendientes = pendientes; }
    public List<Map<String, Object>> getAprobadas() { return aprobadas; }
    public void setAprobadas(List<Map<String, Object>> aprobadas) { this.aprobadas = aprobadas; }
    public List<Map<String, Object>> getRechazadas() { return rechazadas; }
    public void setRechazadas(List<Map<String, Object>> rechazadas) { this.rechazadas = rechazadas; }
}
