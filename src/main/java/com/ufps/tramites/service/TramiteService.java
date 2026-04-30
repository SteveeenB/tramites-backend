package com.ufps.tramites.service;

import com.ufps.tramites.model.Convocatoria;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TramiteService {

    @Autowired private ConvocatoriaService  convocatoriaService;
    @Autowired private SolicitudRepository  solicitudRepository;
    @Autowired private UsuarioRepository    usuarioRepository;

    // ── Vista del módulo por rol ──────────────────────────────────────────────

    public Map<String, Object> construirModuloPorRol(Usuario usuario) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("modulo",   "TRAMITES");
        resp.put("usuario",  construirUsuario(usuario));
        resp.put("sidebar",  construirSidebar(usuario.getRol()));
        resp.put("acciones", construirAcciones(usuario.getRol()));
        return resp;
    }

    // ── Proceso de grado (vista estudiante) ──────────────────────────────────

    public Map<String, Object> construirProcesoDeGrado(Usuario usuario) {
        int aprobados  = usuario.getCreditosAprobados() != null ? usuario.getCreditosAprobados() : 0;
        int requeridos = usuario.getProgramaAcademico() != null
                ? usuario.getProgramaAcademico().getTotalCreditos() : Integer.MAX_VALUE;

        boolean enConvocatoria   = convocatoriaService.estaVigente();
        boolean etapa1Habilitada = aprobados >= requeridos && enConvocatoria;

        Optional<Solicitud> solTerminacion = solicitudRepository
            .findFirstByCedulaAndTipo(usuario.getCedula(), "TERMINACION_MATERIAS");
        boolean terminacionAprobada = solTerminacion.isPresent()
            && "APROBADA".equals(solTerminacion.get().getEstado());

        Optional<Solicitud> solGrado = solicitudRepository
            .findFirstByCedulaAndTipo(usuario.getCedula(), "GRADO");
        boolean gradoAprobado = solGrado.isPresent()
            && "APROBADA".equals(solGrado.get().getEstado());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("creditos",              construirCreditos(usuario));
        resp.put("estadoAcademico",       "Regular");
        resp.put("convocatoria",          construirConvocatoria());
        resp.put("etapa1Habilitada",      etapa1Habilitada);
        resp.put("etapa2Disponible",      terminacionAprobada);
        resp.put("etapa3PazYSalvo",       gradoAprobado);    // muestra sección paz y salvo
        resp.put("certificadoDisponible", terminacionAprobada);
        resp.put("solicitudGrado",
            solGrado.map(this::construirResumenSolicitud).orElse(null));
        return resp;
    }

    // ── Vista del director: lista de estudiantes con su estado ───────────────

    /**
     * Devuelve la lista de todos los estudiantes del programa del director,
     * con su estado actual en el proceso de grado:
     *   EN_TERMINACION | EN_GRADO | APROBADO_GRADUACION
     */
    public List<Map<String, Object>> construirListaEstudiantesDirector(Usuario director) {
        Long programaId = director.getProgramaAcademico() != null
                ? director.getProgramaAcademico().getId() : null;
        if (programaId == null) return List.of();

        List<Usuario> estudiantes = usuarioRepository
            .findByProgramaAcademicoIdAndRol(programaId, "ESTUDIANTE");

        return estudiantes.stream().map(est -> {
            Optional<Solicitud> solTerm = solicitudRepository
                .findFirstByCedulaAndTipo(est.getCedula(), "TERMINACION_MATERIAS");
            Optional<Solicitud> solGrado = solicitudRepository
                .findFirstByCedulaAndTipo(est.getCedula(), "GRADO");

            boolean termAprobada  = solTerm.isPresent()  && "APROBADA".equals(solTerm.get().getEstado());
            boolean gradoAprobado = solGrado.isPresent() && "APROBADA".equals(solGrado.get().getEstado());

            String etapa;
            String etapaLabel;
            if (gradoAprobado) {
                etapa = "APROBADO_GRADUACION";
                etapaLabel = "Aprobado para graduación";
            } else if (termAprobada) {
                etapa = "EN_GRADO";
                etapaLabel = "En proceso de grado";
            } else if (solTerm.isPresent()) {
                etapa = "EN_TERMINACION";
                etapaLabel = "En proceso de terminación de materias";
            } else {
                etapa = "SIN_SOLICITUD";
                etapaLabel = "Sin solicitud activa";
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cedula",   est.getCedula());
            m.put("nombre",   est.getNombre());
            m.put("codigo",   est.getCodigo() != null ? est.getCodigo() : "—");
            m.put("programa", est.getProgramaAcademico() != null
                ? est.getProgramaAcademico().getNombre() : "—");
            m.put("etapa",       etapa);
            m.put("etapaLabel",  etapaLabel);
            m.put("creditosAprobados", est.getCreditosAprobados());

            // Estado detallado de solicitudes
            m.put("terminacion", solTerm.map(s -> Map.of(
                "estado",         s.getEstado(),
                "fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : "—"
            )).orElse(null));

            m.put("grado", solGrado.map(s -> Map.of(
                "estado",         s.getEstado(),
                "fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : "—"
            )).orElse(null));

            return m;
        }).collect(Collectors.toList());
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Map<String, Object> construirUsuario(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cedula",            u.getCedula());
        m.put("nombre",            u.getNombre());
        m.put("codigo",            u.getCodigo());
        m.put("rol",               u.getRol());
        m.put("creditosAprobados", u.getCreditosAprobados());
        m.put("programaAcademico", u.getProgramaAcademico() != null
            ? u.getProgramaAcademico().getNombre() : null);
        return m;
    }

    private Map<String, Object> construirCreditos(Usuario u) {
        int aprobados  = u.getCreditosAprobados() != null ? u.getCreditosAprobados() : 0;
        int requeridos = u.getProgramaAcademico() != null
            ? u.getProgramaAcademico().getTotalCreditos() : 0;
        return Map.of("aprobados", aprobados, "requeridos", requeridos);
    }

    private Map<String, Object> construirConvocatoria() {
        Convocatoria c = convocatoriaService.getActiva();
        return Map.of("fechaInicio", c.getFechaInicio().toString(), "fechaFin", c.getFechaFin().toString());
    }

    private Map<String, Object> construirResumenSolicitud(Solicitud s) {
        Map<String, Object> liq = Map.of(
            "concepto",      "Derechos de Grado",
            "valor",         s.getCosto(),
            "instrucciones", "Realiza el pago en Tesorería o por PSE.",
            "fechaLimite",   s.getFechaSolicitud() != null
                ? s.getFechaSolicitud().plusDays(5).toString() : "—"
        );
        return Map.of(
            "id",             s.getId(),
            "tipo",           s.getTipo(),
            "estado",         s.getEstado(),
            "fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null,
            "costo",          s.getCosto(),
            "observaciones",  s.getObservaciones() != null ? s.getObservaciones() : "",
            "liquidacion",    liq
        );
    }

    private List<Map<String, Object>> construirSidebar(String rol) {
        List<Map<String, Object>> menu = new ArrayList<>();
        menu.add(item("inicio", "Resumen", "/tramites"));
        switch (rol) {
            case "ESTUDIANTE":
                menu.add(item("proceso", "Proceso de Grado", "/proceso-de-grado"));
                menu.add(item("certs",   "Certificados",     "/certificados"));
                break;
            case "DIRECTOR":
                menu.add(item("estudiantes", "Estado de Estudiantes", "/tramites/director/estudiantes"));
                menu.add(item("paz-salvo",   "Mis Paz y Salvos",      "/tramites/director/paz-y-salvo"));
                break;
            case "ADMIN":
                menu.add(item("panel",  "Panel general",   "/tramites"));
                menu.add(item("config", "Configuración",   "/tramites/admin/configuracion"));
                break;
            default: break;
        }
        return menu;
    }

    private List<Map<String, Object>> construirAcciones(String rol) {
        List<Map<String, Object>> acc = new ArrayList<>();
        acc.add(accion("CREAR_SOLICITUD",   "Crear solicitud",  "ESTUDIANTE".equals(rol)));
        acc.add(accion("APROBAR_SOLICITUD", "Aprobar",          "DIRECTOR".equals(rol) || "ADMIN".equals(rol)));
        acc.add(accion("RECHAZAR_SOLICITUD","Rechazar",         "DIRECTOR".equals(rol) || "ADMIN".equals(rol)));
        return acc;
    }

    private Map<String, Object> item(String id, String label, String ruta) {
        return Map.of("id", id, "label", label, "ruta", ruta);
    }

    private Map<String, Object> accion(String codigo, String label, boolean habilitada) {
        return Map.of("codigo", codigo, "label", label, "habilitada", habilitada);
    }
}
