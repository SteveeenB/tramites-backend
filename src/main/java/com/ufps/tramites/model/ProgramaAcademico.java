package com.ufps.tramites.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "programa_academico",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "nombre"),
           @UniqueConstraint(columnNames = "codigo")
       })
public class ProgramaAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código del programa en el Módulo de Matrícula (SC-03, N5). Es la llave
    // para mapear su respuesta; null hasta la primera sincronización.
    private String codigo;

    private String nombre;
    private String tipo; // DOCTORADO, MAESTRIA, ESPECIALIZACION
    private Integer totalCreditos;

    public ProgramaAcademico() {}

    public Long getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getTotalCreditos() { return totalCreditos; }
    public void setTotalCreditos(Integer totalCreditos) { this.totalCreditos = totalCreditos; }
}
