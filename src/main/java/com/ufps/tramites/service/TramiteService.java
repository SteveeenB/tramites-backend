package com.ufps.tramites.service;

import com.ufps.tramites.model.Convocatoria;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.SolicitudRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TramiteService {

    @Autowired
    private ConvocatoriaService convocatoriaService;

    @Autowired
    private SolicitudRepository solicitudRepository;

    public Map<String, Object> construirModuloPorRol(Usuario usuario) {
        String rol = usuario.getRol();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("modulo", "TRAMITES");
        response.put("usuario", construirUsuario(usuario));
        response.put("sidebar", construirSidebar(rol));
        response.put("acciones", construirAcciones(rol));

        return response;
    }

    public Map<String, Object> construirProcesoDeGrado(Usuario usuario) {
        int creditosAprobados = usuario.getCreditosAprobados() != null ? usuario.getCreditosAprobados() : 0;
        int creditosRequeridos = usuario.getProgramaAcademico() != null
                ? usuario.getProgramaAcademico().getTotalCreditos() : Integer.MAX_VALUE;

        boolean enConvocatoria = convocatoriaService.estaVigente();

        // Etapa 1 se habilita si el estudiante tiene los créditos suficientes
        // Y la convocatoria académica está vigente
        boolean etapa1Habilitada = creditosAprobados >= creditosRequeridos && enConvocatoria;

        Optional<Solicitud> solicitudTerminacion = solicitudRepository
                .findFirstByCedulaAndTipo(usuario.getCedula(), "TERMINACION_MATERIAS");

        boolean terminacionAprobada = solicitudTerminacion.isPresent()
                && "APROBADA".equals(solicitudTerminacion.get().getEstado());
        boolean etapa2Disponible    = terminacionAprobada;
        boolean certificadoDisponible = terminacionAprobada;

        Optional<Solicitud> solicitudGrado = solicitudRepository
                .findFirstByCedulaAndTipo(usuario.getCedula(), "GRADO");

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("creditos", construirCreditos(usuario));
        response.put("estadoAcademico", "Regular");
        response.put("convocatoria", construirConvocatoria());
        response.put("etapa1Completada", etapa1Habilitada);
        response.put("etapa2Disponible", etapa2Disponible);
        response.put("certificadoDisponible", certificadoDisponible);
        response.put("solicitudGrado", solicitudGrado.map(this::construirResumenSolicitudGrado).orElse(null));

        return response;
    }

    private Map<String, Object> construirUsuario(Usuario usuario) {
        Map<String, Object> usuarioMap = new LinkedHashMap<>();
        usuarioMap.put("cedula", usuario.getCedula());
        usuarioMap.put("nombre", usuario.getNombre());
        usuarioMap.put("codigo", usuario.getCodigo());
        usuarioMap.put("rol", usuario.getRol());
        usuarioMap.put("creditosAprobados", usuario.getCreditosAprobados());
        usuarioMap.put("programaAcademico", usuario.getProgramaAcademico() != null
                ? usuario.getProgramaAcademico().getNombre() : null);
        return usuarioMap;
    }

    private Map<String, Object> construirCreditos(Usuario usuario) {
        Map<String, Object> creditos = new LinkedHashMap<>();
        int aprobados = usuario.getCreditosAprobados() != null ? usuario.getCreditosAprobados() : 0;
        int requeridos = usuario.getProgramaAcademico() != null
                ? usuario.getProgramaAcademico().getTotalCreditos() : 0;
        creditos.put("aprobados", aprobados);
        creditos.put("requeridos", requeridos);
        return creditos;
    }

    private Map<String, Object> construirConvocatoria() {
        Convocatoria c = convocatoriaService.getActiva();
        Map<String, Object> conv = new LinkedHashMap<>();
        conv.put("fechaInicio", c.getFechaInicio().toString());
        conv.put("fechaFin", c.getFechaFin().toString());
        return conv;
    }

    private List<Map<String, Object>> construirSidebar(String rol) {
        List<Map<String, Object>> menu = new ArrayList<>();

        menu.add(item("inicio", "Resumen", "/tramites"));

        switch (rol) {
            case "ESTUDIANTE":
                menu.add(item("nueva", "Nueva solicitud", "/tramites/nueva"));
                menu.add(item("mis", "Mis solicitudes", "/tramites/mis-solicitudes"));
                break;
            case "DIRECTOR":
                menu.add(item("bandeja", "Bandeja de aprobacion", "/tramites/aprobaciones"));
                menu.add(item("historial", "Historial de decisiones", "/tramites/historial"));
                break;
            case "ADMIN":
                menu.add(item("panel", "Panel general", "/tramites/panel"));
                menu.add(item("config", "Configuracion", "/tramites/configuracion"));
                menu.add(item("usuarios", "Gestion de usuarios", "/tramites/usuarios"));
                break;
            default:
                menu.add(item("consulta", "Consulta", "/tramites/consulta"));
                break;
        }

        return menu;
    }

    private List<Map<String, Object>> construirAcciones(String rol) {
        List<Map<String, Object>> acciones = new ArrayList<>();

        boolean puedeCrear = "ESTUDIANTE".equals(rol) || "ADMIN".equals(rol);
        boolean puedeAprobar = "DIRECTOR".equals(rol) || "ADMIN".equals(rol);
        boolean puedeRechazar = "DIRECTOR".equals(rol) || "ADMIN".equals(rol);
        boolean puedeGestionar = "ADMIN".equals(rol);

        acciones.add(accion("CREAR_SOLICITUD", "Crear solicitud", puedeCrear));
        acciones.add(accion("APROBAR_SOLICITUD", "Aprobar", puedeAprobar));
        acciones.add(accion("RECHAZAR_SOLICITUD", "Rechazar", puedeRechazar));
        acciones.add(accion("GESTION_TOTAL", "Gestion total", puedeGestionar));

        return acciones;
    }

    private Map<String, Object> construirResumenSolicitudGrado(Solicitud s) {
        Map<String, Object> liq = new LinkedHashMap<>();
        liq.put("concepto", "Derechos de Grado");
        liq.put("valor", s.getCosto());
        liq.put("fechaLimite", s.getFechaSolicitud() != null
                ? s.getFechaSolicitud().plusDays(5).toString() : null);
        liq.put("instrucciones", "Realiza el pago en la ventanilla de Tesorería o por PSE antes de la fecha límite.");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("tipo", s.getTipo());
        map.put("estado", s.getEstado());
        map.put("fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null);
        map.put("costo", s.getCosto());
        map.put("observaciones", s.getObservaciones());
        map.put("liquidacion", liq);
        return map;
    }

    private Map<String, Object> item(String id, String label, String ruta) {
        Map<String, Object> menuItem = new LinkedHashMap<>();
        menuItem.put("id", id);
        menuItem.put("label", label);
        menuItem.put("ruta", ruta);
        return menuItem;
    }

    private Map<String, Object> accion(String codigo, String label, boolean habilitada) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("codigo", codigo);
        accion.put("label", label);
        accion.put("habilitada", habilitada);
        return accion;
    }
}
