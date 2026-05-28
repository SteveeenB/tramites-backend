package com.ufps.tramites.repository;

import com.ufps.tramites.model.Estudiante;
import com.ufps.tramites.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Optional<Estudiante> findByUsuario(Usuario usuario);
    Optional<Estudiante> findByCodigo(String codigo);
    Optional<Estudiante> findByCedula(String cedula);
}
