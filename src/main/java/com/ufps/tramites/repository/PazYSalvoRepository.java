package com.ufps.tramites.repository;

import com.ufps.tramites.model.PazYSalvo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PazYSalvoRepository extends JpaRepository<PazYSalvo, Long> {

    /** Todos los paz y salvos de un estudiante (para la vista del director y del estudiante). */
    List<PazYSalvo> findByCedulaEstudiante(String cedula);

    /** Por token único (link del correo). */
    Optional<PazYSalvo> findByToken(String token);

    /** De un estudiante + dependencia específica. */
    Optional<PazYSalvo> findByCedulaEstudianteAndDependencia(String cedula, String dependencia);

    /** De una solicitud de grado específica. */
    List<PazYSalvo> findBySolicitudId(Long solicitudId);

    /** De varios estudiantes — para la vista del director (su propia dependencia). */
    List<PazYSalvo> findByCedulaEstudianteInAndDependencia(List<String> cedulas, String dependencia);
}
