package com.ufps.tramites.service;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SolicitudService {

    // Período habilitado por el calendario académico
    public static final LocalDate CONVOCATORIA_INICIO = LocalDate.of(2026, 4, 7);
    public static final LocalDate CONVOCATORIA_FIN    = LocalDate.of(2026, 4, 25);

    // Costo fijo del trámite de terminación de materias (COP)
    private static final double COSTO_TERMINACION = 150_000.0;

    // Costo fijo de los derechos de grado (COP) — valor de prueba
    private static final double COSTO_GRADO = 250_000.0;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionSseService notificacionSseService;

    @Autowired
    private NotificacionService notificacionService;
    /**
     * Crea una solicitud de terminación de materias.
     * Valida: período de convocatoria, créditos aprobados y duplicados.
     */
    public Map<String, Object> crearSolicitudTerminacion(Usuario estudiante) {
        // 1. Validar créditos (requisito reglamentario)
        int creditosAprobados = estudiante.getCreditosAprobados() != null ? estudiante.getCreditosAprobados() : 0;
        int creditosRequeridos = estudiante.getProgramaAcademico() != null
                ? estudiante.getProgramaAcademico().getTotalCreditos()
                : Integer.MAX_VALUE;
        if (creditosAprobados < creditosRequeridos) {
            throw new IllegalStateException(
                "No cumple los requisitos académicos: tiene " + creditosAprobados
                + "/" + creditosRequeridos + " créditos aprobados."
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
        Optional<Solicitud> existente = solicitudRepository.findFirstByCedulaAndTipo(
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

    /**
     * Crea una solicitud de grado académico.
     * Requiere que la terminación de materias esté APROBADA y que no exista solicitud de grado activa.
     */
    public Map<String, Object> crearSolicitudGrado(Usuario estudiante) {
        // 1. Verificar que la terminación de materias esté aprobada (requisito previo)
        Optional<Solicitud> terminacion = solicitudRepository.findFirstByCedulaAndTipo(
            estudiante.getCedula(), "TERMINACION_MATERIAS"
        );
        if (terminacion.isEmpty() || !"APROBADA".equals(terminacion.get().getEstado())) {
            throw new IllegalStateException(
                "Debe tener la Terminación de Materias aprobada para solicitar el grado académico."
            );
        }

        // 2. Verificar que no exista ya una solicitud de grado activa
        Optional<Solicitud> existente = solicitudRepository.findFirstByCedulaAndTipo(
            estudiante.getCedula(), "GRADO"
        );
        if (existente.isPresent()) {
            throw new IllegalStateException(
                "Ya existe una solicitud de grado con estado: " + existente.get().getEstado()
            );
        }

        // 3. Crear y guardar la solicitud
        Solicitud solicitud = new Solicitud();
        solicitud.setCedula(estudiante.getCedula());
        solicitud.setTipo("GRADO");
        solicitud.setEstado("PENDIENTE_PAGO");
        solicitud.setFechaSolicitud(LocalDate.now());
        solicitud.setCosto(COSTO_GRADO);
        solicitud.setObservaciones("Solicitud de grado registrada por el sistema.");

        solicitudRepository.save(solicitud);

        return construirRespuestaSolicitud(solicitud);
    }

    /**
     * Retorna la bandeja del director agrupada por estado.
     * Incluye todas las solicitudes de TERMINACION_MATERIAS de los estudiantes
     * del mismo programa académico del director.
     */
    public Map<String, Object> obtenerBandejaDirector(Usuario director) {
        Long programaId = director.getProgramaAcademico() != null
                ? director.getProgramaAcademico().getId() : null;

        if (programaId == null) {
            return Map.of("pendientes", List.of(), "aprobadas", List.of(), "rechazadas", List.of());
        }

        List<Usuario> estudiantes = usuarioRepository.findByProgramaAcademicoIdAndRol(programaId, "ESTUDIANTE");
        List<String> cedulas = estudiantes.stream().map(Usuario::getCedula).collect(Collectors.toList());
        Map<String, Usuario> porCedula = estudiantes.stream()
                .collect(Collectors.toMap(Usuario::getCedula, u -> u));

        List<Solicitud> todas = solicitudRepository.findByCedulaInAndTipo(cedulas, "TERMINACION_MATERIAS");

        List<Solicitud> pendientes = todas.stream()
                .filter(s -> "PENDIENTE_PAGO".equals(s.getEstado()) || "EN_REVISION".equals(s.getEstado()))
                .collect(Collectors.toList());
        List<Solicitud> aprobadas  = todas.stream()
                .filter(s -> "APROBADA".equals(s.getEstado()))
                .collect(Collectors.toList());
        List<Solicitud> rechazadas = todas.stream()
                .filter(s -> "RECHAZADA".equals(s.getEstado()))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pendientes", mapearConEstudiante(pendientes, porCedula));
        response.put("aprobadas",  mapearConEstudiante(aprobadas,  porCedula));
        response.put("rechazadas", mapearConEstudiante(rechazadas, porCedula));
        return response;
    }

    private List<Map<String, Object>> mapearConEstudiante(List<Solicitud> solicitudes,
                                                          Map<String, Usuario> porCedula) {
        return solicitudes.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",             s.getId());
            map.put("tipo",           s.getTipo());
            map.put("estado",         s.getEstado());
            map.put("fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null);
            map.put("observaciones",  s.getObservaciones());

            Usuario est = porCedula.get(s.getCedula());
            if (est != null) {
                Map<String, Object> estMap = new LinkedHashMap<>();
                estMap.put("cedula",   est.getCedula());
                estMap.put("nombre",   est.getNombre());
                estMap.put("programa", est.getProgramaAcademico() != null
                        ? est.getProgramaAcademico().getNombre() : null);
                map.put("estudiante", estMap);
            }
            return map;
        }).collect(Collectors.toList());
    }

    /** Aprueba una solicitud de terminación de materias pendiente. */
    public Map<String, Object> aprobarSolicitud(Long id) {
        Solicitud s = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"PENDIENTE_PAGO".equals(s.getEstado()) && !"EN_REVISION".equals(s.getEstado())) {
            throw new IllegalStateException("Solo se pueden aprobar solicitudes en estado pendiente");
        }
        String estadoAnterior = s.getEstado();
        s.setEstado("APROBADA");
        s.setObservaciones("Aprobada por el director de programa.");
        solicitudRepository.save(s);

        notificarEstudiante(s, estadoAnterior);
        return construirRespuestaSolicitud(s);
    }

    /** Rechaza una solicitud de terminación de materias pendiente. */
    public Map<String, Object> rechazarSolicitud(Long id, String motivo) {
        Solicitud s = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"PENDIENTE_PAGO".equals(s.getEstado()) && !"EN_REVISION".equals(s.getEstado())) {
            throw new IllegalStateException("Solo se pueden rechazar solicitudes en estado pendiente");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalStateException("Se requiere motivo de rechazo");
        }
        String estadoAnterior = s.getEstado();
        s.setEstado("RECHAZADA");
        s.setObservaciones(motivo);
        solicitudRepository.save(s);

        notificarEstudiante(s, estadoAnterior);
        return construirRespuestaSolicitud(s);
    }

    private void notificarEstudiante(Solicitud s, String estadoAnterior) {
        notificacionSseService.notificarCambioEstado(s, estadoAnterior);
        usuarioRepository.findById(s.getCedula())
                .ifPresent(est -> notificacionService.notificarEstudianteCambioEstado(s, est));
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
        map.put("certificadoDisponible",
                "APROBADA".equals(s.getEstado()) && "TERMINACION_MATERIAS".equals(s.getTipo()));
        return map;
    }

    private Map<String, Object> construirLiquidacion(Solicitud s) {
        Map<String, Object> liq = new LinkedHashMap<>();
        boolean esGrado = "GRADO".equals(s.getTipo());
        liq.put("concepto", esGrado ? "Derechos de Grado" : "Trámite de Terminación de Materias");
        liq.put("valor", s.getCosto());
        liq.put("fechaLimite", s.getFechaSolicitud() != null
            ? s.getFechaSolicitud().plusDays(5).toString()
            : null);
        liq.put("instrucciones", "Realiza el pago en la ventanilla de Tesorería o por PSE antes de la fecha límite.");
        return liq;
    }

    /**
     * Genera el contenido de texto del certificado de terminación de materias.
     * Valida que la solicitud exista, sea de tipo TERMINACION_MATERIAS y esté APROBADA.
     */
    public byte[] generarCertificado(Long id) {
        Solicitud s = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"APROBADA".equals(s.getEstado())) {
            throw new IllegalStateException("El certificado solo está disponible para solicitudes aprobadas");
        }
        if (!"TERMINACION_MATERIAS".equals(s.getTipo())) {
            throw new IllegalStateException("Este tipo de solicitud no genera certificado");
        }

        Usuario est = usuarioRepository.findById(s.getCedula()).orElse(null);
        String nombre   = est != null ? est.getNombre() : "Estudiante";
        String cedula   = s.getCedula();
        String codigo   = est != null ? est.getCodigo() : "-";
        String programa = est != null && est.getProgramaAcademico() != null
                ? est.getProgramaAcademico().getNombre() : "-";
        String fecha    = s.getFechaSolicitud() != null
                ? s.getFechaSolicitud().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
                        new java.util.Locale("es", "CO")))
                : LocalDate.now().toString();

        String contenido =
            "================================================================\n" +
            "       UNIVERSIDAD FRANCISCO DE PAULA SANTANDER\n" +
            "         SISTEMA DE TRAMITES DE POSGRADO\n" +
            "================================================================\n\n" +
            "         CERTIFICADO DE TERMINACION DE MATERIAS\n\n" +
            "Se certifica que el/la estudiante:\n\n" +
            "  Nombre:             " + nombre   + "\n" +
            "  Cedula:             " + cedula   + "\n" +
            "  Codigo estudiantil: " + codigo   + "\n" +
            "  Programa:           " + programa + "\n\n" +
            "Ha cumplido satisfactoriamente con los requisitos academicos\n" +
            "establecidos para la Terminacion de Materias, segun resolucion\n" +
            "aprobada el " + fecha + ".\n\n" +
            "Expedido el: " + LocalDate.now().format(
                    DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
                            new java.util.Locale("es", "CO"))) + "\n\n" +
            "================================================================\n" +
            "  UNIVERSIDAD FRANCISCO DE PAULA SANTANDER\n" +
            "  San Jose de Cucuta, Colombia\n" +
            "  Este documento es valido ante la oficina de\n" +
            "  Registro y Control Academico.\n" +
            "================================================================\n";

        return contenido.getBytes(StandardCharsets.UTF_8);
    }
}
