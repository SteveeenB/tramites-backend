package com.ufps.tramites.repository;

import com.ufps.tramites.model.ProgramaAcademico;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramaAcademicoRepository extends JpaRepository<ProgramaAcademico, Long> {
    Optional<ProgramaAcademico> findByCodigo(String codigo);
}
