package com.ufps.tramites.service;

import com.ufps.tramites.model.TipoDocumentoRequerido;
import com.ufps.tramites.repository.TipoDocumentoRequeridoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TipoDocumentoService {

    private final TipoDocumentoRequeridoRepository repository;

    public TipoDocumentoService(TipoDocumentoRequeridoRepository repository) {
        this.repository = repository;
    }

    // Documentos requeridos por defecto. El admin puede modificarlos desde la UI.
    @PostConstruct
    void sembrarDocumentosDefecto() {
        if (repository.count() > 0) return;

        String[][] defaults = {
            { "Fotografía 3x4",                  "Fotografía reciente tamaño 3x4 en fondo blanco",              "true",  "1" },
            { "Acta de grado de bachillerato",    "Fotocopia legible del acta o diploma de bachillerato",        "true",  "2" },
            { "Fotocopia de cédula de ciudadanía","Fotocopia de la cédula por ambas caras",                      "true",  "3" },
            { "Certificado de notas",             "Certificado oficial de notas expedido por la secretaría",     "true",  "4" },
            { "Paz y salvo académico",            "Documento emitido por registro y control que certifica no deudas académicas", "true", "5" },
        };

        for (String[] d : defaults) {
            TipoDocumentoRequerido t = new TipoDocumentoRequerido();
            t.setNombre(d[0]);
            t.setDescripcion(d[1]);
            t.setObligatorio(Boolean.parseBoolean(d[2]));
            t.setOrden(Integer.parseInt(d[3]));
            repository.save(t);
        }
    }

    public List<TipoDocumentoRequerido> listarTodos() {
        return repository.findAllByOrderByOrdenAsc();
    }

    public long contarObligatorios() {
        return repository.countByObligatorio(true);
    }

    public TipoDocumentoRequerido crear(TipoDocumentoRequerido tipo) {
        return repository.save(tipo);
    }

    public TipoDocumentoRequerido actualizar(Long id, TipoDocumentoRequerido datos) {
        TipoDocumentoRequerido tipo = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de documento no encontrado"));
        tipo.setNombre(datos.getNombre());
        tipo.setDescripcion(datos.getDescripcion());
        tipo.setObligatorio(datos.isObligatorio());
        tipo.setOrden(datos.getOrden());
        return repository.save(tipo);
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Tipo de documento no encontrado");
        }
        repository.deleteById(id);
    }
}
