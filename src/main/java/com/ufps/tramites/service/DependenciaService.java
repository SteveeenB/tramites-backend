package com.ufps.tramites.service;

import com.ufps.tramites.model.Dependencia;
import com.ufps.tramites.repository.DependenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DependenciaService {

    @Autowired
    private DependenciaRepository dependenciaRepository;

    public List<Dependencia> obtenerTodas() {
        return dependenciaRepository.findByActivaTrue();
    }

    public Dependencia obtenerPorId(Long id) {
        return dependenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dependencia no encontrada: " + id));
    }

    public Dependencia obtenerPorNombre(String nombre) {
        return dependenciaRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException("Dependencia no encontrada: " + nombre));
    }

    public Dependencia crear(Dependencia dependencia) {
        dependencia.setActiva(true);
        return dependenciaRepository.save(dependencia);
    }

    public void desactivar(Long id) {
        Dependencia dep = obtenerPorId(id);
        dep.setActiva(false);
        dependenciaRepository.save(dep);
    }
}
