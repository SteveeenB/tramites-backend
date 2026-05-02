package com.ufps.tramites.dto;

import java.util.List;

public class SolicitudGradoDetalleDTO {
    private Long id;
    private String tipo;
    private String cedula;
    private String nombreEstudiante;
    private String codigoEstudiante;
    private String fechaSolicitud;
    private String estado;
    private String decision;
    private String observacionesDirector;
    private String fechaDecision;
    private String fechaEnRevision;

    // Informacion especifica del proyecto de grado
    private String tituloProyecto;
    private String tipoProyecto;
    private String resumenProyecto;

    // Archivos asociados
    private List<ArchivoDTO> archivos;

    public static class ArchivoDTO {
        private Long id;
        private String tipoArchivo;
        private String nombreOriginal;
        private String url;
        private Long tamano;
        private String fechaSubida;
        private String contentType;

        public ArchivoDTO() {}

        public ArchivoDTO(Long id, String tipoArchivo, String nombreOriginal, String url, Long tamano, String fechaSubida, String contentType) {
            this.id = id;
            this.tipoArchivo = tipoArchivo;
            this.nombreOriginal = nombreOriginal;
            this.url = url;
            this.tamano = tamano;
            this.fechaSubida = fechaSubida;
            this.contentType = contentType;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTipoArchivo() { return tipoArchivo; }
        public void setTipoArchivo(String tipoArchivo) { this.tipoArchivo = tipoArchivo; }
        public String getNombreOriginal() { return nombreOriginal; }
        public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Long getTamano() { return tamano; }
        public void setTamano(Long tamano) { this.tamano = tamano; }
        public String getFechaSubida() { return fechaSubida; }
        public void setFechaSubida(String fechaSubida) { this.fechaSubida = fechaSubida; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }

    public SolicitudGradoDetalleDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getNombreEstudiante() { return nombreEstudiante; }
    public void setNombreEstudiante(String nombreEstudiante) { this.nombreEstudiante = nombreEstudiante; }
    public String getCodigoEstudiante() { return codigoEstudiante; }
    public void setCodigoEstudiante(String codigoEstudiante) { this.codigoEstudiante = codigoEstudiante; }
    public String getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(String fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getObservacionesDirector() { return observacionesDirector; }
    public void setObservacionesDirector(String observacionesDirector) { this.observacionesDirector = observacionesDirector; }
    public String getFechaDecision() { return fechaDecision; }
    public void setFechaDecision(String fechaDecision) { this.fechaDecision = fechaDecision; }
    public String getFechaEnRevision() { return fechaEnRevision; }
    public void setFechaEnRevision(String fechaEnRevision) { this.fechaEnRevision = fechaEnRevision; }
    public String getTituloProyecto() { return tituloProyecto; }
    public void setTituloProyecto(String tituloProyecto) { this.tituloProyecto = tituloProyecto; }
    public String getTipoProyecto() { return tipoProyecto; }
    public void setTipoProyecto(String tipoProyecto) { this.tipoProyecto = tipoProyecto; }
    public String getResumenProyecto() { return resumenProyecto; }
    public void setResumenProyecto(String resumenProyecto) { this.resumenProyecto = resumenProyecto; }
    public List<ArchivoDTO> getArchivos() { return archivos; }
    public void setArchivos(List<ArchivoDTO> archivos) { this.archivos = archivos; }
}
