package com.ufps.tramites.repository;

import com.ufps.tramites.model.EstadoEstudiante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoEstudianteRepository extends JpaRepository<EstadoEstudiante, Long> {
    Optional<EstadoEstudiante> findByNombre(String nombre);
}
