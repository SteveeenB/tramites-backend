package com.ufps.tramites.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_certificado")
public class TipoCertificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;        // CONSTANCIA_MATRICULA
    private String label;         // CONSTANCIA MATRICULA ACADEMICA POSGRADO
    private Double precioDigital;
    private Double precioFisico;
    private Boolean activo;       // para habilitar/deshabilitar sin borrar

    public TipoCertificado() {}

    public Long getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Double getPrecioDigital() { return precioDigital; }
    public void setPrecioDigital(Double precioDigital) { this.precioDigital = precioDigital; }

    public Double getPrecioFisico() { return precioFisico; }
    public void setPrecioFisico(Double precioFisico) { this.precioFisico = precioFisico; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}