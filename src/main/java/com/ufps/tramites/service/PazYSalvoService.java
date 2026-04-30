package com.ufps.tramites.service;

import com.ufps.tramites.model.PazYSalvo;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.PazYSalvoRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PazYSalvoService {

    // Dependencias que participan en el proceso (sin el director que va aparte)
    private static final List<String> DEPENDENCIAS_EXTERNAS = List.of(
        "BIBLIOTECA", "TESORERIA", "BIENESTAR"
    );

    @Autowired private PazYSalvoRepository pazYSalvoRepository;
    @Autowired private SolicitudRepository  solicitudRepository;
    @Autowired private UsuarioRepository    usuarioRepository;
    @Autowired private EmailPazYSalvoService emailPazYSalvoService;

    @Value("${app.dependencias.biblioteca.nombre:Biblioteca Eduardo Cote Lamus}")
    private String nombreBiblioteca;

    @Value("${app.dependencias.tesoreria.nombre:Tesorería}")
    private String nombreTesoreria;

    @Value("${app.dependencias.bienestar.nombre:Bienestar Universitario}")
    private String nombreBienestar;

    // ─────────────────────────────────────────────────────────────────────────
    // Inicialización del proceso (llamado al aprobar GRADO)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crea los registros de Paz y Salvo para todas las dependencias
     * (incluyendo el DIRECTOR) y envía los correos a las dependencias externas.
     *
     * Se llama automáticamente desde SolicitudService cuando el director
     * aprueba la solicitud de GRADO.
     */
    public void iniciarProcesoPazYSalvo(Solicitud solicitudGrado, Usuario estudiante) {
        List<PazYSalvo> creados = new ArrayList<>();

        // Crear registro para cada dependencia externa
        for (String dep : DEPENDENCIAS_EXTERNAS) {
            // Evitar duplicados si por algún motivo ya existen
            Optional<PazYSalvo> existente = pazYSalvoRepository
                .findByCedulaEstudianteAndDependencia(estudiante.getCedula(), dep);
            if (existente.isPresent()) continue;

            PazYSalvo pys = new PazYSalvo();
            pys.setCedulaEstudiante(estudiante.getCedula());
            pys.setSolicitudId(solicitudGrado.getId());
            pys.setDependencia(dep);
            pys.setNombreDependencia(resolverNombre(dep));
            pys.setToken(UUID.randomUUID().toString());
            pys.setEstado("PENDIENTE");
            pys.setFechaCreacion(LocalDateTime.now());
            pazYSalvoRepository.save(pys);
            creados.add(pys);
        }

        // Crear registro del DIRECTOR (sin token — usa su sesión)
        Optional<PazYSalvo> dirExistente = pazYSalvoRepository
            .findByCedulaEstudianteAndDependencia(estudiante.getCedula(), "DIRECTOR");
        if (dirExistente.isEmpty()) {
            PazYSalvo dirPys = new PazYSalvo();
            dirPys.setCedulaEstudiante(estudiante.getCedula());
            dirPys.setSolicitudId(solicitudGrado.getId());
            dirPys.setDependencia("DIRECTOR");
            dirPys.setNombreDependencia("Director de Programa");
            dirPys.setToken(null); // el director no usa token
            dirPys.setEstado("PENDIENTE");
            dirPys.setFechaCreacion(LocalDateTime.now());
            pazYSalvoRepository.save(dirPys);
        }

        // Enviar correos a las dependencias externas
        emailPazYSalvoService.notificarDependencias(creados, estudiante);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subida de documentos
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Una dependencia sube su Paz y Salvo usando el token recibido por correo.
     */
    public Map<String, Object> subirPorToken(String token, MultipartFile archivo) throws Exception {
        PazYSalvo pys = pazYSalvoRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException(
                "El enlace no es válido o ya fue utilizado anteriormente."));

        if ("CARGADO".equals(pys.getEstado())) {
            throw new IllegalStateException(
                "Este paz y salvo ya fue cargado el " + pys.getFechaCarga() + ".");
        }

        guardarArchivo(pys, archivo);
        pazYSalvoRepository.save(pys);
        return construirRespuesta(pys);
    }

    /**
     * El Director sube su Paz y Salvo desde su vista autenticada.
     * Se identifica por cédula (no usa token).
     */
    public Map<String, Object> subirDirector(String cedulaDirector, String cedulaEstudiante,
                                              MultipartFile archivo) throws Exception {
        PazYSalvo pys = pazYSalvoRepository
            .findByCedulaEstudianteAndDependencia(cedulaEstudiante, "DIRECTOR")
            .orElseThrow(() -> new IllegalArgumentException(
                "No existe un paz y salvo pendiente para este estudiante."));

        if ("CARGADO".equals(pys.getEstado())) {
            throw new IllegalStateException("Ya cargaste el paz y salvo para este estudiante.");
        }

        guardarArchivo(pys, archivo);
        pazYSalvoRepository.save(pys);
        return construirRespuesta(pys);
    }

    private void guardarArchivo(PazYSalvo pys, MultipartFile archivo) throws Exception {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar un archivo.");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || (!tipo.equals("application/pdf")
                && !tipo.startsWith("image/"))) {
            throw new IllegalArgumentException(
                "Solo se aceptan archivos PDF o imágenes (JPG, PNG).");
        }
        pys.setArchivoNombre(archivo.getOriginalFilename());
        pys.setArchivoContenido(archivo.getBytes());
        pys.setArchivoTipo(tipo);
        pys.setEstado("CARGADO");
        pys.setFechaCarga(LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consultas
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Estado de todos los paz y salvos de un estudiante.
     * Usado en la vista del estudiante para ver el progreso.
     */
    public List<Map<String, Object>> obtenerEstadoPorEstudiante(String cedula) {
        return pazYSalvoRepository.findByCedulaEstudiante(cedula)
            .stream()
            .map(this::construirRespuestaSinArchivo)
            .collect(Collectors.toList());
    }

    /**
     * Estado del paz y salvo del DIRECTOR para un estudiante específico.
     * Usado en la vista del director — solo ve el suyo.
     */
    public Map<String, Object> obtenerEstadoDirectorParaEstudiante(String cedulaEstudiante) {
        return pazYSalvoRepository
            .findByCedulaEstudianteAndDependencia(cedulaEstudiante, "DIRECTOR")
            .map(this::construirRespuestaSinArchivo)
            .orElse(null);
    }

    /**
     * Info del token para la página de carga de las dependencias.
     * No devuelve el archivo — solo metadata.
     */
    public Map<String, Object> infoToken(String token) {
        PazYSalvo pys = pazYSalvoRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Enlace no válido."));

        Usuario est = usuarioRepository.findById(pys.getCedulaEstudiante()).orElse(null);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("dependencia",      pys.getDependencia());
        resp.put("nombreDependencia", pys.getNombreDependencia());
        resp.put("estado",            pys.getEstado());
        resp.put("fechaCarga",        pys.getFechaCarga() != null ? pys.getFechaCarga().toString() : null);
        resp.put("estudiante", est != null ? Map.of(
            "nombre",   est.getNombre(),
            "cedula",   est.getCedula(),
            "programa", est.getProgramaAcademico() != null ? est.getProgramaAcademico().getNombre() : "—"
        ) : null);
        return resp;
    }

    /**
     * Descarga el archivo de un paz y salvo (por ID).
     * Usado internamente para que las dependencias o el director descarguen.
     */
    public PazYSalvo obtenerParaDescarga(Long id) {
        return pazYSalvoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Paz y salvo no encontrado."));
    }

    /**
     * Lista de estudiantes pendientes de paz y salvo del DIRECTOR
     * (estudiantes del programa del director cuyo GRADO está aprobado
     * pero el director aún no ha subido su paz y salvo).
     */
    public List<Map<String, Object>> obtenerPendientesDirector(String cedulaDirector) {
        Usuario director = usuarioRepository.findById(cedulaDirector)
            .orElseThrow(() -> new IllegalArgumentException("Director no encontrado"));

        Long programaId = director.getProgramaAcademico() != null
            ? director.getProgramaAcademico().getId() : null;
        if (programaId == null) return List.of();

        List<Usuario> estudiantes = usuarioRepository
            .findByProgramaAcademicoIdAndRol(programaId, "ESTUDIANTE");

        List<String> cedulas = estudiantes.stream()
            .map(Usuario::getCedula).collect(Collectors.toList());

        List<PazYSalvo> suyos = pazYSalvoRepository
            .findByCedulaEstudianteInAndDependencia(cedulas, "DIRECTOR");

        Map<String, Usuario> porCedula = estudiantes.stream()
            .collect(Collectors.toMap(Usuario::getCedula, u -> u));

        return suyos.stream().map(pys -> {
            Usuario est = porCedula.get(pys.getCedulaEstudiante());
            Map<String, Object> m = new LinkedHashMap<>(construirRespuestaSinArchivo(pys));
            if (est != null) {
                m.put("estudiante", Map.of(
                    "nombre",  est.getNombre(),
                    "cedula",  est.getCedula(),
                    "codigo",  est.getCodigo() != null ? est.getCodigo() : "—",
                    "programa", est.getProgramaAcademico() != null
                        ? est.getProgramaAcademico().getNombre() : "—"
                ));
            }
            return m;
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> construirRespuesta(PazYSalvo pys) {
        Map<String, Object> m = construirRespuestaSinArchivo(pys);
        m.put("archivoDisponible", pys.getArchivoContenido() != null);
        return m;
    }

    private Map<String, Object> construirRespuestaSinArchivo(PazYSalvo pys) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               pys.getId());
        m.put("dependencia",      pys.getDependencia());
        m.put("nombreDependencia", pys.getNombreDependencia());
        m.put("estado",            pys.getEstado());
        m.put("archivoNombre",    pys.getArchivoNombre());
        m.put("archivoTipo",      pys.getArchivoTipo());
        m.put("fechaCreacion",    pys.getFechaCreacion() != null ? pys.getFechaCreacion().toString() : null);
        m.put("fechaCarga",       pys.getFechaCarga()    != null ? pys.getFechaCarga().toString()    : null);
        m.put("archivoDisponible", pys.getArchivoContenido() != null && pys.getArchivoContenido().length > 0);
        return m;
    }

    private String resolverNombre(String dep) {
        return switch (dep) {
            case "BIBLIOTECA" -> nombreBiblioteca;
            case "TESORERIA"  -> nombreTesoreria;
            case "BIENESTAR"  -> nombreBienestar;
            default           -> dep;
        };
    }
}
