package com.ufps.tramites.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * Fecha de grado que la oficina de Posgrados publica para que los estudiantes
 * la elijan en el Paso 3 del proceso de grado. Antes estaban escritas a mano en
 * el frontend (y ya eran fechas pasadas); ahora Posgrados las crea, edita,
 * activa/desactiva y elimina.
 */
@Entity
@Table(name = "fechas_grado",
       uniqueConstraints = @UniqueConstraint(name = "uq_fecha_grado",
               columnNames = {"fecha", "modalidad", "hora"}))
public class FechaGrado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    /** CEREMONIA | SECRETARIA */
    @Column(nullable = false)
    private String modalidad;

    /** Texto libre, p. ej. "9:00 AM". */
    @Column(nullable = false)
    private String hora;

    @Column(nullable = false)
    private String lugar;

    /** Solo las activas y futuras se ofrecen al estudiante. */
    @Column(nullable = false)
    private Boolean activa = true;

    public FechaGrado() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}
