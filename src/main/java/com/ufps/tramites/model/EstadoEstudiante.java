package com.ufps.tramites.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catálogo de estados académicos del estudiante. Alineado con la tabla
 * `estados_estudiantes` de la BD oficial (clientes_finales_sistema.md §6).
 *
 * Reemplaza el campo string libre {@code Estudiante.estadoGrado} que vivía
 * con valores hardcoded como "PAGO_GRADO_PENDIENTE" y "GRADUADO".
 *
 * Seeds iniciales:
 *   - ACTIVO
 *   - PAGO_GRADO_PENDIENTE
 *   - GRADUADO
 */
@Entity
@Table(name = "estados_estudiantes")
public class EstadoEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nombre;

    public EstadoEstudiante() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
