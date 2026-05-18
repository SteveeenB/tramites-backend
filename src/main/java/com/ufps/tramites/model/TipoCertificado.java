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

    private String codigo;
    private String label;
    private String descripcion;

    private Double precioDigital;
    private Double costoLogisticaFisica;

    private String dependenciaCedula;
    private String direccionOficina;
    private Integer tiempoEntregaDias;

    private Boolean activo;

    public TipoCertificado() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getPrecioDigital() { return precioDigital; }
    public void setPrecioDigital(Double precioDigital) { this.precioDigital = precioDigital; }

    public Double getCostoLogisticaFisica() { return costoLogisticaFisica; }
    public void setCostoLogisticaFisica(Double costoLogisticaFisica) { this.costoLogisticaFisica = costoLogisticaFisica; }

    public String getDependenciaCedula() { return dependenciaCedula; }
    public void setDependenciaCedula(String dependenciaCedula) { this.dependenciaCedula = dependenciaCedula; }

    public String getDireccionOficina() { return direccionOficina; }
    public void setDireccionOficina(String direccionOficina) { this.direccionOficina = direccionOficina; }

    public Integer getTiempoEntregaDias() { return tiempoEntregaDias; }
    public void setTiempoEntregaDias(Integer tiempoEntregaDias) { this.tiempoEntregaDias = tiempoEntregaDias; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public double precioTotal(String modalidad) {
        double base = precioDigital != null ? precioDigital : 0.0;
        if ("FISICA".equals(modalidad)) {
            double delta = costoLogisticaFisica != null ? costoLogisticaFisica : 0.0;
            return base + delta;
        }
        return base;
    }
}
