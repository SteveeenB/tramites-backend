package com.ufps.tramites.service;

import com.ufps.tramites.model.Convocatoria;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.DocumentoSolicitudRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.annotation.Lazy;
import com.ufps.tramites.service.PazYSalvoService;

@Service
public class SolicitudService {

    // Costo fijo del trámite de terminación de materias (COP) — valor de prueba
    private static final double COSTO_TERMINACION = 150_000.0;

    // Costo fijo de los derechos de grado (COP) — valor de prueba
    private static final double COSTO_GRADO = 250_000.0;

    @Autowired
    private ConvocatoriaService convocatoriaService;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionSseService notificacionSseService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private ActaPdfGeneratorService actaPdfGeneratorService;

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private DocumentoSolicitudRepository documentoSolicitudRepository;

    @Autowired
    @Lazy
    private PazYSalvoService pazYSalvoService;

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
        if (!convocatoriaService.estaVigente()) {
            Convocatoria conv = convocatoriaService.getActiva();
            throw new IllegalStateException(
                "La solicitud está fuera del período habilitado por el calendario académico ("
                + conv.getFechaInicio() + " al " + conv.getFechaFin() + ")."
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
     * Crea una solicitud de grado académico con su detalle y documentos adjuntos.
     * Requiere que la terminación de materias esté APROBADA y que no exista solicitud de grado activa.
     */
    public Map<String, Object> crearSolicitudGrado(
            Usuario estudiante,
            String tituloProyecto,
            String resumen,
            String tipoProyecto,
            MultipartFile foto,
            MultipartFile actaSustentacion,
            MultipartFile certificadoIngles) throws IOException {

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

        // 3. Crear y guardar la solicitud con los datos del proyecto
        Solicitud solicitud = new Solicitud();
        solicitud.setCedula(estudiante.getCedula());
        solicitud.setTipo("GRADO");
        solicitud.setEstado("EN_REVISION");
        solicitud.setFechaSolicitud(LocalDate.now());
        solicitud.setCosto(COSTO_GRADO);
        solicitud.setObservaciones("Solicitud de grado registrada por el sistema.");
        solicitud.setTituloProyecto(tituloProyecto);
        solicitud.setResumenProyecto(resumen);
        solicitud.setTipoProyecto(tipoProyecto);
        solicitudRepository.save(solicitud);

        // 4. Subir documentos obligatorios
        documentoService.guardarDocumento(solicitud.getId(), foto, "FOTO_ESTUDIANTE");
        documentoService.guardarDocumento(solicitud.getId(), actaSustentacion, "ACTA_SUSTENTACION");

        // 5. Certificado de inglés es opcional
        if (certificadoIngles != null && !certificadoIngles.isEmpty()) {
            documentoService.guardarDocumento(solicitud.getId(), certificadoIngles, "CERTIFICADO_INGLES");
        }

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

    /**
     * Retorna la bandeja del director agrupada por estado, solo solicitudes de GRADO
     * de los estudiantes del mismo programa académico del director.
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

        List<Solicitud> pendientes = todas.stream()
                .filter(s -> "EN_REVISION".equals(s.getEstado()) || "PENDIENTE_PAGO".equals(s.getEstado()))
                .collect(Collectors.toList());
        List<Solicitud> aprobadas = todas.stream()
                .filter(s -> "APROBADA".equals(s.getEstado()))
                .collect(Collectors.toList());
        List<Solicitud> rechazadas = todas.stream()
                .filter(s -> "RECHAZADA".equals(s.getEstado()))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pendientes", mapearGradoConEstudiante(pendientes, porCedula));
        response.put("aprobadas",  mapearGradoConEstudiante(aprobadas,  porCedula));
        response.put("rechazadas", mapearGradoConEstudiante(rechazadas, porCedula));
        return response;
    }

    private List<Map<String, Object>> mapearGradoConEstudiante(
            List<Solicitud> solicitudes, Map<String, Usuario> porCedula) {
        return solicitudes.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",                    s.getId());
            map.put("tipo",                  s.getTipo());
            map.put("estado",                s.getEstado());
            map.put("fechaSolicitud",        s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : null);
            map.put("observacionesDirector", s.getObservacionesDirector());
            map.put("tituloProyecto",        s.getTituloProyecto());
            map.put("tipoProyecto",          s.getTipoProyecto());
            map.put("resumenProyecto",       s.getResumenProyecto());
            map.put("documentosCargados",    documentoSolicitudRepository.countBySolicitudId(s.getId()));
            map.put("documentosRequeridos",  2);

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
        return aprobarSolicitudConDirector(id, null);
    }

    public Map<String, Object> aprobarSolicitudConDirector(Long id, String cedulaDirector) {
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

        // Si es solicitud de GRADO: marcar estadoGrado del estudiante e iniciar paz y salvo
        if ("GRADO".equals(s.getTipo())) {
            // Marcar al estudiante con pago de grado pendiente
            usuarioRepository.findById(s.getCedula()).ifPresent(est -> {
                est.setEstadoGrado("PAGO_GRADO_PENDIENTE");
                usuarioRepository.save(est);
            });
            // Iniciar proceso de paz y salvo
            if (cedulaDirector != null) {
                usuarioRepository.findById(cedulaDirector).ifPresent(director ->
                    pazYSalvoService.iniciarProcesoPazYSalvo(s, director)
                );
            }
        }

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

    public Solicitud obtenerSolicitudPorId(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
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

        if ("GRADO".equals(s.getTipo())) {
            map.put("tituloProyecto",      s.getTituloProyecto());
            map.put("resumenProyecto",     s.getResumenProyecto());
            map.put("tipoProyecto",        s.getTipoProyecto());
            // Campos de progreso del proceso de grado
            map.put("estadoPagoGrado", s.getEstadoPagoGrado());
            map.put("fechaGrado",          s.getFechaGrado() != null ? s.getFechaGrado().toString() : null);
        }

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
     * Registra el pago de derechos de grado (demo).
     * Marca el campo pagoGradoRealizado = true en la solicitud.
     */
    public Map<String, Object> registrarPagoGrado(Long id) {
        Solicitud s = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"APROBADA".equals(s.getEstado()) || !"GRADO".equals(s.getTipo())) {
            throw new IllegalStateException("La solicitud no es una solicitud de grado aprobada");
        }
        s.setEstadoPagoGrado("APROBADO");
        solicitudRepository.save(s);
        return construirRespuestaSolicitud(s);
    }

    /**
     * Registra la fecha de graduación elegida por el estudiante.
     * Requiere que el pago ya esté registrado.
     */
    public Map<String, Object> registrarFechaGrado(Long id, LocalDate fechaGrado) {
        Solicitud s = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"APROBADA".equals(s.getEstado()) || !"GRADO".equals(s.getTipo())) {
            throw new IllegalStateException("La solicitud no es una solicitud de grado aprobada");
        }
        if (!"APROBADO".equals(s.getEstadoPagoGrado())) {
            throw new IllegalStateException("El pago de grado no ha sido registrado");
        }
        s.setFechaGrado(fechaGrado);
        solicitudRepository.save(s);
        return construirRespuestaSolicitud(s);
    }

    /**
     * Genera (o reutiliza) el acta de grado en PDF.
     * En la primera llamada:
     *   1. Genera el PDF con datos institucionales y fecha de grado elegida.
     *   2. Marca al estudiante como GRADUADO en la base de datos.
     *   3. Vincula el PDF al expediente digital (DocumentoSolicitud tipo ACTA).
     * En llamadas posteriores devuelve el PDF ya guardado en disco.
     */
    public byte[] generarActa(Long id) {
        Solicitud s = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"APROBADA".equals(s.getEstado())) {
            throw new IllegalStateException("El acta solo está disponible para solicitudes aprobadas");
        }
        if (!"GRADO".equals(s.getTipo())) {
            throw new IllegalStateException("Este endpoint es solo para solicitudes de tipo GRADO");
        }

        // Si el acta ya fue generada, devolver la versión guardada en disco
        Optional<byte[]> actaExistente = documentoService.obtenerActa(id);
        if (actaExistente.isPresent() && actaExistente.get() != null) {
            return actaExistente.get();
        }

        // Primera generación: construir PDF
        Usuario est = usuarioRepository.findById(s.getCedula()).orElse(null);
        String nombre   = est != null ? est.getNombre()   : "Estudiante";
        String cedula   = s.getCedula();
        String codigo   = est != null ? est.getCodigo()   : "-";
        String programa = est != null && est.getProgramaAcademico() != null
                ? est.getProgramaAcademico().getNombre() : "-";

        Locale es = new Locale("es", "CO");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", es);
        String fechaAprobacion = s.getFechaDecision() != null
                ? s.getFechaDecision().toLocalDate().format(fmt)
                : (s.getFechaSolicitud() != null ? s.getFechaSolicitud().format(fmt)
                    : LocalDate.now().format(fmt));

        String fechaExpedicion = LocalDate.now().format(fmt);

        // Fecha de grado elegida por el estudiante (o fecha de expedición como fallback)
        String fechaGradoStr = s.getFechaGrado() != null
                ? s.getFechaGrado().format(fmt)
                : fechaExpedicion;

        try {
            byte[] pdfBytes = actaPdfGeneratorService.generar(
                    nombre, cedula, codigo, programa,
                    fechaAprobacion, fechaExpedicion, fechaGradoStr);

            // Actualizar estado del estudiante a GRADUADO
            if (est != null && !"GRADUADO".equals(est.getEstadoGrado())) {
                est.setEstadoGrado("GRADUADO");
                usuarioRepository.save(est);
            }

            // Vincular PDF al expediente digital
            documentoService.guardarActaComoDocumento(id, pdfBytes);

            return pdfBytes;
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el PDF del acta de grado", e);
        }
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