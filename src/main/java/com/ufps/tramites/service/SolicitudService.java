package com.ufps.tramites.service;

import com.ufps.tramites.model.Convocatoria;
import com.ufps.tramites.model.DocumentoCargado;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.TipoDocumentoRequerido;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.DocumentoCargadoRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Costo fijo del trámite de terminación de materias (COP) — valor de prueba
    private static final double COSTO_TERMINACION = 150_000.0;

    // Costo fijo de los derechos de grado (COP) — valor de prueba
    private static final double COSTO_GRADO = 250_000.0;

    @Autowired private ConvocatoriaService convocatoriaService;
    @Autowired private SolicitudRepository solicitudRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private NotificacionSseService notificacionSseService;
    @Autowired private NotificacionService notificacionService;
    @Autowired private TipoDocumentoService tipoDocumentoService;
    @Autowired private DocumentoCargadoRepository documentoCargadoRepository;

    /**
     * Crea una solicitud de terminación de materias.
     * Valida: período de convocatoria, créditos aprobados y duplicados.
     */
    public Map<String, Object> crearSolicitudTerminacion(Usuario estudiante) {
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

        if (!convocatoriaService.estaVigente()) {
            Convocatoria conv = convocatoriaService.getActiva();
            throw new IllegalStateException(
                "La solicitud está fuera del período habilitado por el calendario académico ("
                + conv.getFechaInicio() + " al " + conv.getFechaFin() + ")."
            );
        }

        Optional<Solicitud> existente = solicitudRepository.findFirstByCedulaAndTipo(
            estudiante.getCedula(), "TERMINACION_MATERIAS"
        );
        if (existente.isPresent()) {
            throw new IllegalStateException(
                "Ya existe una solicitud de terminación de materias con estado: " + existente.get().getEstado()
            );
        }

        Solicitud solicitud = new Solicitud();
        solicitud.setCedula(estudiante.getCedula());
        solicitud.setTipo("TERMINACION_MATERIAS");
        solicitud.setEstado("PENDIENTE_PAGO");
        solicitud.setFechaSolicitud(LocalDate.now());
        solicitud.setCosto(COSTO_TERMINACION);
        solicitud.setObservaciones("Solicitud registrada por el sistema.");

        solicitudRepository.save(solicitud);
        return construirRespuestaSolicitud(solicitud);
    }

    /**
     * Crea una solicitud de grado académico.
     * Requiere terminación de materias APROBADA y no tener solicitud de grado activa.
     * El estado inicial es EN_REVISION: el director debe validar antes del pago.
     */
    public Map<String, Object> crearSolicitudGrado(Usuario estudiante) {
        Optional<Solicitud> terminacion = solicitudRepository.findFirstByCedulaAndTipo(
            estudiante.getCedula(), "TERMINACION_MATERIAS"
        );
        if (terminacion.isEmpty() || !"APROBADA".equals(terminacion.get().getEstado())) {
            throw new IllegalStateException(
                "Debe tener la Terminación de Materias aprobada para solicitar el grado académico."
            );
        }

        Optional<Solicitud> existente = solicitudRepository.findFirstByCedulaAndTipo(
            estudiante.getCedula(), "GRADO"
        );
        if (existente.isPresent()) {
            throw new IllegalStateException(
                "Ya existe una solicitud de grado con estado: " + existente.get().getEstado()
            );
        }

        Solicitud solicitud = new Solicitud();
        solicitud.setCedula(estudiante.getCedula());
        solicitud.setTipo("GRADO");
        // El director valida primero; el pago se habilita tras su aprobación (Paso 3)
        solicitud.setEstado("EN_REVISION");
        solicitud.setFechaSolicitud(LocalDate.now());
        solicitud.setFechaEnRevision(LocalDateTime.now());
        solicitud.setCosto(COSTO_GRADO);
        solicitud.setObservaciones("Solicitud de grado registrada por el sistema.");

        solicitudRepository.save(solicitud);
        return construirRespuestaSolicitud(solicitud);
    }

    /**
     * Retorna la bandeja del director para TERMINACION_MATERIAS, agrupada por estado.
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

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pendientes", mapearConEstudiante(
            todas.stream().filter(s -> "PENDIENTE_PAGO".equals(s.getEstado()) || "EN_REVISION".equals(s.getEstado())).collect(Collectors.toList()),
            porCedula));
        response.put("aprobadas",  mapearConEstudiante(
            todas.stream().filter(s -> "APROBADA".equals(s.getEstado())).collect(Collectors.toList()),
            porCedula));
        response.put("rechazadas", mapearConEstudiante(
            todas.stream().filter(s -> "RECHAZADA".equals(s.getEstado())).collect(Collectors.toList()),
            porCedula));
        return response;
    }

    /**
     * Retorna la bandeja del director para GRADO, agrupada por estado.
     * Pendientes = EN_REVISION (esperando decisión del director).
     * Aprobadas  = decisión=APROBADA (pago habilitado o etapas posteriores).
     * Rechazadas = decisión=RECHAZADA.
     */
    public Map<String, Object> obtenerBandejaGrado(Usuario director) {
        Long programaId = director.getProgramaAcademico() != null
                ? director.getProgramaAcademico().getId() : null;

        if (programaId == null) {
            return Map.of("pendientes", List.of(), "aprobadas", List.of(), "rechazadas", List.of());
        }

        List<Usuario> estudiantes = usuarioRepository.findByProgramaAcademicoIdAndRol(programaId, "ESTUDIANTE");
        List<String> cedulas = estudiantes.stream().map(Usuario::getCedula).collect(Collectors.toList());
        Map<String, Usuario> porCedula = estudiantes.stream()
                .collect(Collectors.toMap(Usuario::getCedula, u -> u));

        List<Solicitud> todas = solicitudRepository.findByCedulaInAndTipo(cedulas, "GRADO");
        long totalObligatorios = tipoDocumentoService.contarObligatorios();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pendientes", mapearConEstudianteGrado(
            todas.stream().filter(s -> "EN_REVISION".equals(s.getEstado())).collect(Collectors.toList()),
            porCedula, totalObligatorios));
        response.put("aprobadas", mapearConEstudianteGrado(
            todas.stream().filter(s -> "APROBADA".equals(s.getDecision())).collect(Collectors.toList()),
            porCedula, totalObligatorios));
        response.put("rechazadas", mapearConEstudianteGrado(
            todas.stream().filter(s -> "RECHAZADA".equals(s.getDecision())).collect(Collectors.toList()),
            porCedula, totalObligatorios));
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

    private List<Map<String, Object>> mapearConEstudianteGrado(List<Solicitud> solicitudes,
                                                               Map<String, Usuario> porCedula,
                                                               long totalObligatorios) {
        return solicitudes.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",                    s.getId());
            map.put("tipo",                  s.getTipo());
            map.put("estado",                s.getEstado());
            map.put("decision",              s.getDecision());
            map.put("fechaSolicitud",        s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null);
            map.put("observacionesDirector", s.getObservacionesDirector());
            map.put("fechaDecision",         s.getFechaDecision() != null ? s.getFechaDecision().toString() : null);

            long documentosCargados = documentoCargadoRepository.countBySolicitudId(s.getId());
            map.put("documentosCargados",    documentosCargados);
            map.put("documentosRequeridos",  totalObligatorios);

            Usuario est = porCedula.get(s.getCedula());
            if (est != null) {
                Map<String, Object> estMap = new LinkedHashMap<>();
                estMap.put("cedula",              est.getCedula());
                estMap.put("nombre",              est.getNombre());
                estMap.put("programa",            est.getProgramaAcademico() != null
                        ? est.getProgramaAcademico().getNombre() : null);
                estMap.put("creditosAprobados",   est.getCreditosAprobados());
                estMap.put("creditosRequeridos",  est.getProgramaAcademico() != null
                        ? est.getProgramaAcademico().getTotalCreditos() : null);
                map.put("estudiante", estMap);
            }
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Aprueba una solicitud. Para GRADO el nuevo estado es PAGO_PENDIENTE (habilita Paso 3).
     * Para TERMINACION_MATERIAS el estado pasa a APROBADA.
     * Registra quién decidió y cuándo.
     */
    public Map<String, Object> aprobarSolicitud(Long id, String cedulaDirector) {
        Solicitud s = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"PENDIENTE_PAGO".equals(s.getEstado()) && !"EN_REVISION".equals(s.getEstado())) {
            throw new IllegalStateException("Solo se pueden aprobar solicitudes en estado pendiente");
        }
        String estadoAnterior = s.getEstado();
        String nuevoEstado = "GRADO".equals(s.getTipo()) ? "PAGO_PENDIENTE" : "APROBADA";
        s.setEstado(nuevoEstado);
        s.setDecision("APROBADA");
        s.setObservacionesDirector("Solicitud aprobada por el director de programa.");
        s.setCedulaDirector(cedulaDirector);
        s.setFechaDecision(LocalDateTime.now());
        solicitudRepository.save(s);

        notificarEstudiante(s, estadoAnterior);
        return construirRespuestaSolicitud(s);
    }

    /**
     * Rechaza una solicitud. Las observaciones son obligatorias.
     * Registra quién decidió, cuándo y el motivo.
     */
    public Map<String, Object> rechazarSolicitud(Long id, String motivo, String cedulaDirector) {
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
        s.setDecision("RECHAZADA");
        s.setObservaciones(motivo);
        s.setObservacionesDirector(motivo);
        s.setCedulaDirector(cedulaDirector);
        s.setFechaDecision(LocalDateTime.now());
        solicitudRepository.save(s);

        notificarEstudiante(s, estadoAnterior);
        return construirRespuestaSolicitud(s);
    }

    /**
     * Retorna los tipos de documento requeridos junto con el estado de carga para una solicitud.
     * Paso 1 completará el llenado de DocumentoCargado; por ahora todos aparecen como no cargados.
     */
    public List<Map<String, Object>> obtenerDocumentosSolicitud(Long solicitudId) {
        List<TipoDocumentoRequerido> tipos = tipoDocumentoService.listarTodos();
        List<DocumentoCargado> cargados = documentoCargadoRepository.findBySolicitudId(solicitudId);
        Map<Long, DocumentoCargado> porTipo = cargados.stream()
                .collect(Collectors.toMap(DocumentoCargado::getTipoDocumentoId, d -> d));

        return tipos.stream().map(tipo -> {
            DocumentoCargado doc = porTipo.get(tipo.getId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("tipoDocumentoId", tipo.getId());
            map.put("nombre",          tipo.getNombre());
            map.put("descripcion",     tipo.getDescripcion());
            map.put("obligatorio",     tipo.isObligatorio());
            map.put("orden",           tipo.getOrden());
            map.put("cargado",         doc != null);
            map.put("urlArchivo",      doc != null ? doc.getUrlArchivo() : null);
            map.put("fechaCarga",      doc != null && doc.getFechaCarga() != null
                    ? doc.getFechaCarga().toString() : null);
            return map;
        }).collect(Collectors.toList());
    }

    private void notificarEstudiante(Solicitud s, String estadoAnterior) {
        notificacionSseService.notificarCambioEstado(s, estadoAnterior);
        usuarioRepository.findById(s.getCedula())
                .ifPresent(est -> notificacionService.notificarEstudianteCambioEstado(s, est));
    }

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
