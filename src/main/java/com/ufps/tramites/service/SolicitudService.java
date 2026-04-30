package com.ufps.tramites.service;

import com.ufps.tramites.model.Convocatoria;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);
    private static final double COSTO_TERMINACION = 150_000.0;
    private static final double COSTO_GRADO       = 250_000.0;

    @Autowired private ConvocatoriaService    convocatoriaService;
    @Autowired private SolicitudRepository    solicitudRepository;
    @Autowired private UsuarioRepository      usuarioRepository;
    @Autowired private NotificacionSseService notificacionSseService;
    @Autowired private NotificacionService    notificacionService;

    @Autowired(required = false)
    private PazYSalvoService pazYSalvoService;

    public Map<String, Object> crearSolicitudTerminacion(Usuario estudiante) {
        int aprobados  = estudiante.getCreditosAprobados() != null ? estudiante.getCreditosAprobados() : 0;
        int requeridos = estudiante.getProgramaAcademico() != null
                ? estudiante.getProgramaAcademico().getTotalCreditos() : Integer.MAX_VALUE;
        if (aprobados < requeridos)
            throw new IllegalStateException("No cumple los requisitos: tiene " + aprobados + "/" + requeridos + " créditos.");
        if (!convocatoriaService.estaVigente()) {
            Convocatoria conv = convocatoriaService.getActiva();
            throw new IllegalStateException("Fuera del período habilitado (" + conv.getFechaInicio() + " al " + conv.getFechaFin() + ").");
        }
        solicitudRepository.findFirstByCedulaAndTipo(estudiante.getCedula(), "TERMINACION_MATERIAS")
            .ifPresent(e -> { throw new IllegalStateException("Ya existe una solicitud con estado: " + e.getEstado()); });

        Solicitud s = new Solicitud();
        s.setCedula(estudiante.getCedula()); s.setTipo("TERMINACION_MATERIAS");
        s.setEstado("PENDIENTE_PAGO"); s.setFechaSolicitud(LocalDate.now());
        s.setCosto(COSTO_TERMINACION); s.setObservaciones("Solicitud registrada por el sistema.");
        solicitudRepository.save(s);
        return construirRespuestaSolicitud(s);
    }

    public Map<String, Object> crearSolicitudGrado(Usuario estudiante) {
        Optional<Solicitud> term = solicitudRepository.findFirstByCedulaAndTipo(estudiante.getCedula(), "TERMINACION_MATERIAS");
        if (term.isEmpty() || !"APROBADA".equals(term.get().getEstado()))
            throw new IllegalStateException("Debe tener la Terminación de Materias aprobada.");
        solicitudRepository.findFirstByCedulaAndTipo(estudiante.getCedula(), "GRADO")
            .ifPresent(e -> { throw new IllegalStateException("Ya existe una solicitud de grado con estado: " + e.getEstado()); });

        Solicitud s = new Solicitud();
        s.setCedula(estudiante.getCedula()); s.setTipo("GRADO");
        s.setEstado("PENDIENTE_PAGO"); s.setFechaSolicitud(LocalDate.now());
        s.setCosto(COSTO_GRADO); s.setObservaciones("Solicitud de grado registrada por el sistema.");
        solicitudRepository.save(s);
        return construirRespuestaSolicitud(s);
    }

    public Map<String, Object> obtenerBandejaDirector(Usuario director) {
        Long programaId = director.getProgramaAcademico() != null ? director.getProgramaAcademico().getId() : null;
        if (programaId == null) return Map.of("pendientes", List.of(), "aprobadas", List.of(), "rechazadas", List.of());

        List<Usuario> estudiantes = usuarioRepository.findByProgramaAcademicoIdAndRol(programaId, "ESTUDIANTE");
        List<String> cedulas = estudiantes.stream().map(Usuario::getCedula).collect(Collectors.toList());
        Map<String, Usuario> porCedula = estudiantes.stream().collect(Collectors.toMap(Usuario::getCedula, u -> u));

        // Incluye TERMINACION_MATERIAS y GRADO
        List<Solicitud> todas = solicitudRepository.findByCedulaInAndTipoIn(cedulas, List.of("TERMINACION_MATERIAS", "GRADO"));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("pendientes", mapearConEstudiante(todas.stream()
            .filter(s -> "PENDIENTE_PAGO".equals(s.getEstado()) || "EN_REVISION".equals(s.getEstado()))
            .collect(Collectors.toList()), porCedula));
        resp.put("aprobadas",  mapearConEstudiante(todas.stream()
            .filter(s -> "APROBADA".equals(s.getEstado())).collect(Collectors.toList()), porCedula));
        resp.put("rechazadas", mapearConEstudiante(todas.stream()
            .filter(s -> "RECHAZADA".equals(s.getEstado())).collect(Collectors.toList()), porCedula));
        return resp;
    }

    public Map<String, Object> aprobarSolicitud(Long id) {
        Solicitud s = solicitudRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"PENDIENTE_PAGO".equals(s.getEstado()) && !"EN_REVISION".equals(s.getEstado()))
            throw new IllegalStateException("Solo se pueden aprobar solicitudes en estado pendiente");

        s.setEstado("APROBADA");
        s.setObservaciones("Aprobada por el director de programa.");
        solicitudRepository.save(s);   // se guarda primero — la aprobación nunca falla por el correo

        try { notificarEstudiante(s, "PENDIENTE_PAGO"); } catch (Exception e) {
            log.warn("Fallo notificación SSE #{}: {}", id, e.getMessage());
        }

        // Iniciar paz y salvos — si falla el correo, la aprobación ya está guardada
        if ("GRADO".equals(s.getTipo()) && pazYSalvoService != null) {
            try {
                usuarioRepository.findById(s.getCedula()).ifPresent(est ->
                    pazYSalvoService.iniciarProcesoPazYSalvo(s, est));
            } catch (Exception e) {
                log.error("Error al iniciar paz y salvo / correos para solicitud #{}: {}", id, e.getMessage(), e);
            }
        }
        return construirRespuestaSolicitud(s);
    }

    public Map<String, Object> rechazarSolicitud(Long id, String motivo) {
        Solicitud s = solicitudRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"PENDIENTE_PAGO".equals(s.getEstado()) && !"EN_REVISION".equals(s.getEstado()))
            throw new IllegalStateException("Solo se pueden rechazar solicitudes en estado pendiente");
        if (motivo == null || motivo.isBlank()) throw new IllegalStateException("Se requiere motivo de rechazo");

        s.setEstado("RECHAZADA"); s.setObservaciones(motivo);
        solicitudRepository.save(s);
        try { notificarEstudiante(s, "PENDIENTE_PAGO"); } catch (Exception e) {
            log.warn("Fallo notificación SSE #{}: {}", id, e.getMessage());
        }
        return construirRespuestaSolicitud(s);
    }

    public List<Map<String, Object>> obtenerSolicitudesPorCedula(String cedula) {
        return solicitudRepository.findByCedula(cedula).stream()
            .map(this::construirRespuestaSolicitud).collect(Collectors.toList());
    }

    public byte[] generarCertificado(Long id) {
        Solicitud s = solicitudRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"APROBADA".equals(s.getEstado())) throw new IllegalStateException("Solo disponible para solicitudes aprobadas");
        if (!"TERMINACION_MATERIAS".equals(s.getTipo())) throw new IllegalStateException("Este tipo no genera certificado");

        Usuario est = usuarioRepository.findById(s.getCedula()).orElse(null);
        String nombre   = est != null ? est.getNombre() : "Estudiante";
        String cedula   = s.getCedula();
        String codigo   = est != null && est.getCodigo() != null ? est.getCodigo() : "-";
        String programa = est != null && est.getProgramaAcademico() != null ? est.getProgramaAcademico().getNombre() : "-";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "CO"));
        String fecha = s.getFechaSolicitud() != null ? s.getFechaSolicitud().format(fmt) : LocalDate.now().toString();

        String contenido =
            "================================================================\n" +
            "       UNIVERSIDAD FRANCISCO DE PAULA SANTANDER\n" +
            "         SISTEMA DE TRÁMITES DE POSGRADO\n" +
            "================================================================\n\n" +
            "         CERTIFICADO DE TERMINACIÓN DE MATERIAS\n\n" +
            "  Nombre:             " + nombre   + "\n" +
            "  Cédula:             " + cedula   + "\n" +
            "  Código estudiantil: " + codigo   + "\n" +
            "  Programa:           " + programa + "\n\n" +
            "Ha cumplido los requisitos para la Terminación de Materias,\n" +
            "aprobada el " + fecha + ".\n\n" +
            "Expedido el: " + LocalDate.now().format(fmt) + "\n\n" +
            "================================================================\n" +
            "  UNIVERSIDAD FRANCISCO DE PAULA SANTANDER — Cúcuta, Colombia\n" +
            "================================================================\n";
        return contenido.getBytes(StandardCharsets.UTF_8);
    }

    private void notificarEstudiante(Solicitud s, String anterior) {
        notificacionSseService.notificarCambioEstado(s, anterior);
        usuarioRepository.findById(s.getCedula())
            .ifPresent(est -> notificacionService.notificarEstudianteCambioEstado(s, est));
    }

    private List<Map<String, Object>> mapearConEstudiante(List<Solicitud> lista, Map<String, Usuario> porCedula) {
        return lista.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             s.getId());
            m.put("tipo",           s.getTipo());
            m.put("estado",         s.getEstado());
            m.put("costo",          s.getCosto());
            m.put("fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null);
            m.put("observaciones",  s.getObservaciones());
            Usuario est = porCedula.get(s.getCedula());
            if (est != null) m.put("estudiante", Map.of(
                "cedula",   est.getCedula(),
                "nombre",   est.getNombre(),
                "programa", est.getProgramaAcademico() != null ? est.getProgramaAcademico().getNombre() : ""
            ));
            return m;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> construirRespuestaSolicitud(Solicitud s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             s.getId());
        m.put("tipo",           s.getTipo());
        m.put("estado",         s.getEstado());
        m.put("fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null);
        m.put("costo",          s.getCosto());
        m.put("observaciones",  s.getObservaciones());
        m.put("liquidacion",    construirLiquidacion(s));
        m.put("certificadoDisponible",
            "APROBADA".equals(s.getEstado()) && "TERMINACION_MATERIAS".equals(s.getTipo()));
        return m;
    }

    private Map<String, Object> construirLiquidacion(Solicitud s) {
        boolean esGrado = "GRADO".equals(s.getTipo());
        return Map.of(
            "concepto",      esGrado ? "Derechos de Grado" : "Trámite de Terminación de Materias",
            "valor",         s.getCosto(),
            "fechaLimite",   s.getFechaSolicitud() != null ? s.getFechaSolicitud().plusDays(5).toString() : "",
            "instrucciones", "Realiza el pago en la ventanilla de Tesorería o por PSE antes de la fecha límite."
        );
    }
}