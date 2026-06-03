package com.ufps.tramites.repository;

import com.ufps.tramites.model.Dependencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DependenciaRepository extends JpaRepository<Dependencia, Long> {
    List<Dependencia> findByActivaTrue();
    Optional<Dependencia> findByNombre(String nombre);
}
