package com.ufps.tramites.repository;

import com.ufps.tramites.model.FechaGrado;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FechaGradoRepository extends JpaRepository<FechaGrado, Long> {

    /** Fechas que se ofrecen al estudiante: activas y no vencidas. */
    List<FechaGrado> findByActivaTrueAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(LocalDate desde);

    /** Para validar que la fecha elegida sea una de las publicadas. */
    List<FechaGrado> findByActivaTrueAndFechaGreaterThanEqualAndFecha(LocalDate desde, LocalDate fecha);

    boolean existsByFechaAndModalidadAndHora(LocalDate fecha, String modalidad, String hora);

    boolean existsByFechaAndModalidadAndHoraAndIdNot(LocalDate fecha, String modalidad, String hora, Long id);
}
