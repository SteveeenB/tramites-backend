package com.ufps.tramites.service;

import com.ufps.tramites.dto.AvanceAcademico;
import com.ufps.tramites.model.Estudiante;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.EstudianteRepository;
import org.springframework.stereotype.Service;

/**
 * Único punto de cálculo del requisito de créditos (SC-03, N3). Antes la
 * misma cuenta estaba copiada en SolicitudService, TramiteService y
 * PazYSalvoService con fallbacks distintos (MAX_VALUE frente a 0).
 *
 * Aprobados: estudiante.creditos_aprobados, precargado en nuestra BD.
 * Requeridos: programa_academico.total_creditos del programa del usuario.
 */
@Service
public class AvanceAcademicoService {

    private final EstudianteRepository estudianteRepository;

    public AvanceAcademicoService(EstudianteRepository estudianteRepository) {
        this.estudianteRepository = estudianteRepository;
    }

    public AvanceAcademico calcular(Usuario usuario) {
        return calcular(usuario, estudianteRepository.findByUsuario(usuario).orElse(null));
    }

    /** Variante para quien ya cargó el perfil y quiere evitar otra consulta. */
    public AvanceAcademico calcular(Usuario usuario, Estudiante perfil) {
        Integer aprobados = perfil != null ? perfil.getCreditosAprobados() : null;
        Integer requeridos = usuario.getProgramaAcademico() != null
                ? usuario.getProgramaAcademico().getTotalCreditos() : null;
        return new AvanceAcademico(aprobados, requeridos);
    }
}
