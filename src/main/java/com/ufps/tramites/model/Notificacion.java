package com.ufps.tramites.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador del destinatario: cedula para ESTUDIANTE/DIRECTOR,
     * codigo para POSGRADOS/DEPENDENCIA/ADMIN.
     */
    @Column(nullable = false)
    private String destinatarioCedula;

    /** ESTADO_CAMBIADO | PLAZO_VENCIDO | GENERAL */
    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    /** Ruta relativa del frontend a la que navegar al hacer clic. */
    private String enlace;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
