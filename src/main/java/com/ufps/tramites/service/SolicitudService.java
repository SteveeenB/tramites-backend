package com.ufps.tramites.service;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.SolicitudRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SolicitudService {

    // Período habilitado por el calendario académico
    private static final LocalDate CONVOCATORIA_INICIO = LocalDate.of(2026, 4, 7);
    private static final LocalDate CONVOCATORIA_FIN    = LocalDate.of(2026, 4, 25);

    // Costo fijo del trámite de terminación de materias (COP)
    private static final double COSTO_TERMINACION = 150_000.0;

    @Autowired
    private SolicitudRepository solicitudRepository;

    /**
     * Crea una solicitud de terminación de materias.
     * Valida: período de convocatoria, créditos aprobados y duplicados.
     */
    public Map<String, Object> crearSolicitudTerminacion(Usuario estudiante) {
        // 1. Validar créditos (requisito reglamentario)
        int creditosAprobados = estudiante.getCreditosAprobados() != null ? estudiante.getCreditosAprobados() : 0;
        if (creditosAprobados < 100) {
            throw new IllegalStateException(
                "No cumple los requisitos académicos: tiene " + creditosAprobados + "/100 créditos aprobados."
            );
        }

        // 2. Validar calendario académico
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(CONVOCATORIA_INICIO) || hoy.isAfter(CONVOCATORIA_FIN)) {
            throw new IllegalStateException(
                "La solicitud está fuera del período habilitado por el calendario académico ("
                + CONVOCATORIA_INICIO + " al " + CONVOCATORIA_FIN + ")."
            );
        }

        // 3. Verificar que no exista una solicitud activa del mismo tipo
        Optional<Solicitud> existente = solicitudRepository.findByCedulaAndTipo(
            estudiante.getCedula(), "TERMINACION_MATERIAS"
        );
        if (existente.isPresent()) {
            throw new IllegalStateException(
                "Ya existe una solicitud de terminación de materias con estado: " + existente.get().getEstado()
            );
        }

        // 4. Crear y guardar la solicitud
        Solicitud solicitud = new Solicitud();
        solicitud.setCedula(estudiante.getCedula());
        solicitud.setTipo("TERMINACION_MATERIAS");
        solicitud.setEstado("PENDIENTE_PAGO");
        solicitud.setFechaSolicitud(hoy);
        solicitud.setCosto(COSTO_TERMINACION);
        solicitud.setObservaciones("Solicitud registrada por el sistema.");

        solicitudRepository.save(solicitud);

        return construirRespuestaSolicitud(solicitud);
    }

    /** Retorna todas las solicitudes de un estudiante. */
    public List<Map<String, Object>> obtenerSolicitudesPorCedula(String cedula) {
        List<Solicitud> solicitudes = solicitudRepository.findByCedula(cedula);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Solicitud s : solicitudes) {
            resultado.add(construirRespuestaSolicitud(s));
        }
        return resultado;
    }

    private Map<String, Object> construirRespuestaSolicitud(Solicitud s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("tipo", s.getTipo());
        map.put("estado", s.getEstado());
        map.put("fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null);
        map.put("costo", s.getCosto());
        map.put("observaciones", s.getObservaciones());
        map.put("liquidacion", construirLiquidacion(s));
        return map;
    }

    private Map<String, Object> construirLiquidacion(Solicitud s) {
        Map<String, Object> liq = new LinkedHashMap<>();
        liq.put("concepto", "Trámite de Terminación de Materias");
        liq.put("valor", s.getCosto());
        liq.put("fechaLimite", s.getFechaSolicitud() != null
            ? s.getFechaSolicitud().plusDays(5).toString()
            : null);
        liq.put("instrucciones", "Realiza el pago en la ventanilla de Tesorería o por PSE antes de la fecha límite.");
        return liq;
    }
}
