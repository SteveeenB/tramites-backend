package com.ufps.tramites.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// Paso 1 (carga de documentos por el estudiante) completará el llenado de esta entidad.
// Por ahora solo existe el esquema para que el director vea el estado de cada documento.
@Entity
public class DocumentoCargado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long solicitudId;       // FK a Solicitud.id (convención ligera)
    private Long tipoDocumentoId;   // FK a TipoDocumentoRequerido.id
    private String urlArchivo;      // URL en Supabase Storage — Paso 1 lo completa
    private LocalDateTime fechaCarga;

    public DocumentoCargado() {}

    public Long getId() { return id; }

    public Long getSolicitudId() { return solicitudId; }
    public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }

    public Long getTipoDocumentoId() { return tipoDocumentoId; }
    public void setTipoDocumentoId(Long tipoDocumentoId) { this.tipoDocumentoId = tipoDocumentoId; }

    public String getUrlArchivo() { return urlArchivo; }
    public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }

    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
}
