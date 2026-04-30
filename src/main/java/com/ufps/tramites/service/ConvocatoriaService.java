package com.ufps.tramites.service;

import com.ufps.tramites.model.Convocatoria;
import com.ufps.tramites.repository.ConvocatoriaRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConvocatoriaService {

    @Autowired
    private ConvocatoriaRepository convocatoriaRepository;

    // Siembra el registro inicial si la tabla está vacía (primer arranque)
    @PostConstruct
    public void inicializar() {
        if (convocatoriaRepository.count() == 0) {
            Convocatoria c = new Convocatoria();
            c.setFechaInicio(LocalDate.of(2026, 4, 7));
            c.setFechaFin(LocalDate.of(2026, 4, 25));
            convocatoriaRepository.save(c);
        }
    }

    public Convocatoria getActiva() {
        List<Convocatoria> todas = convocatoriaRepository.findAll();
        if (todas.isEmpty()) {
            throw new IllegalStateException("No hay convocatoria configurada en el sistema");
        }
        return todas.get(0);
    }

    public boolean estaVigente() {
        Convocatoria c = getActiva();
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(c.getFechaInicio()) && !hoy.isAfter(c.getFechaFin());
    }

    public Convocatoria actualizar(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException(
                "La fecha de fin no puede ser anterior a la fecha de inicio"
            );
        }
        Convocatoria c = getActiva();
        c.setFechaInicio(fechaInicio);
        c.setFechaFin(fechaFin);
        return convocatoriaRepository.save(c);
    }
}
