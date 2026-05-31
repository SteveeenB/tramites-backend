package com.ufps.tramites.service;

import com.ufps.tramites.model.Admin;
import com.ufps.tramites.model.Estudiante;
import com.ufps.tramites.model.PazYSalvo;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.AdminRepository;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.PazYSalvoRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import com.ufps.tramites.security.ResolvedPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PazYSalvoService {

    private static final Logger log = LoggerFactory.getLogger(PazYSalvoService.class);

    @Autowired private PazYSalvoRepository pazYSalvoRepository;
    @Autowired private SolicitudRepository solicitudRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private EstudianteRepository estudianteRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    // Remitente configurado en application.properties (spring.mail.username)
    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * Cuando el director aprueba una solicitud de GRADO, se crean los paz y
     * salvos: uno por cada admin con tipo DEPENDENCIA o POSGRADOS, y uno
     * para el propio director (sigue siendo Usuario).
     */
    public void iniciarProcesoPazYSalvo(Solicitud solicitud, Usuario director) {
        Usuario estudiante = usuarioRepository.findByCedula(solicitud.getCedula()).orElse(null);
        String nombreEstudiante = estudiante != null ? estudiante.getNombre() : solicitud.getCedula();

        List<Admin> admins = new ArrayList<>(adminRepository.findByTipo("DEPENDENCIA"));
        admins.addAll(adminRepository.findByTipo("POSGRADOS"));

        List<PazYSalvo> nuevos = new ArrayList<>();

        for (Admin admin : admins) {
            PazYSalvo ps = new PazYSalvo();
            ps.setSolicitudId(solicitud.getId());
            ps.setCedulaEstudiante(solicitud.getCedula());
            ps.setResponsableAdmin(admin);
            ps.setDependencia(admin.getDependencia());
            ps.setTipoDependencia(admin.getDependencia() != null
                    ? admin.getDependencia().getNombre()
                    : admin.getTipo());
            ps.setEstado("PENDIENTE");
            ps.setFechaSolicitud(LocalDateTime.now());
            nuevos.add(ps);
        }

        // Paz y salvo del director (sigue siendo Usuario)
        PazYSalvo psDirector = new PazYSalvo();
        psDirector.setSolicitudId(solicitud.getId());
        psDirector.setCedulaEstudiante(solicitud.getCedula());
        psDirector.setResponsableUsuario(director);
        psDirector.setTipoDependencia("DIRECTOR_PROGRAMA");
        psDirector.setEstado("PENDIENTE");
        psDirector.setFechaSolicitud(LocalDateTime.now());
        nuevos.add(psDirector);

        pazYSalvoRepository.saveAll(nuevos);

        for (Admin admin : admins) {
            enviarCorreoPazYSalvo(admin.getEmail(), admin.getNombreCompleto(),
                    nombreEstudiante, solicitud.getId());
        }
        enviarCorreoPazYSalvo(director.getCorreo(), "Director de Programa",
                nombreEstudiante, solicitud.getId());
    }

    private void enviarCorreoPazYSalvo(String correo, String nombreDependencia,
                                        String nombreEstudiante, Long solicitudId) {
        String asunto = "[UFPS Posgrados] Verificación de Paz y Salvo requerida";
        String cuerpo = "Estimado/a " + nombreDependencia + ",\n\n"
            + "El/la estudiante " + nombreEstudiante + " ha solicitado su grado académico "
            + "y requiere verificación de paz y salvo con su dependencia.\n\n"
            + "Por favor ingrese al sistema y confirme si el estudiante se encuentra a paz y salvo.\n"
            + "Solicitud N°: " + solicitudId + "\n\n"
            + "Atentamente,\nUniversidad Francisco de Paula Santander (UFPS)\n"
            + "Sistema de Trámites de Posgrado";

        if (correo == null || correo.isBlank()) {
            log.warn("[PAZ Y SALVO - SIN CORREO] Destinatario '{}' sin correo registrado.\nAsunto: {}\n{}",
                    nombreDependencia, asunto, cuerpo);
            return;
        }

        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                if (fromEmail != null && !fromEmail.isBlank()) msg.setFrom(fromEmail);
                msg.setTo(correo);
                msg.setSubject(asunto);
                msg.setText(cuerpo);
                mailSender.send(msg);
                log.info("[PAZ Y SALVO] Correo enviado a {} ({})", nombreDependencia, correo);
            } catch (Exception e) {
                log.error("[PAZ Y SALVO] Error enviando correo a {} ({}): {}",
                        nombreDependencia, correo, e.getMessage());
            }
        } else {
            log.info("=== [SIMULACIÓN CORREO PAZ Y SALVO] ===\nPara: {} <{}>\nAsunto: {}\n{}\n===",
                    nombreDependencia, correo, asunto, cuerpo);
        }
    }

    /**
     * Permite a una dependencia (Admin) o director (Usuario) responder su paz y salvo.
     * Valida que el actor sea el responsable real del registro.
     */
    public Map<String, Object> responderPazYSalvo(Long pazYSalvoId, ResolvedPrincipal actor,
                                                   String decision, String observaciones) {
        PazYSalvo ps = pazYSalvoRepository.findById(pazYSalvoId)
            .orElseThrow(() -> new IllegalArgumentException("Paz y salvo no encontrado"));

        boolean autorizado = (actor.isAdmin()
                && ps.getResponsableAdminId() != null
                && ps.getResponsableAdminId().equals(actor.id()))
            || (actor.isUsuario()
                && ps.getResponsableUsuarioId() != null
                && ps.getResponsableUsuarioId().equals(actor.id()));
        if (!autorizado) {
            throw new IllegalStateException("No tiene permiso para responder este paz y salvo");
        }
        if (!"PENDIENTE".equals(ps.getEstado())) {
            throw new IllegalStateException("Este paz y salvo ya fue respondido: " + ps.getEstado());
        }
        if (!"APROBADO".equals(decision) && !"RECHAZADO".equals(decision)) {
            throw new IllegalArgumentException("Decisión inválida. Use APROBADO o RECHAZADO");
        }

        ps.setEstado(decision);
        ps.setObservaciones(observaciones);
        ps.setFechaRespuesta(LocalDateTime.now());
        pazYSalvoRepository.save(ps);

        return mapearPazYSalvo(ps);
    }

    /**
     * Retorna los paz y salvos asignados al actor (admin DEPENDENCIA/POSGRADOS
     * o usuario DIRECTOR), filtrando opcionalmente por estado.
     */
    public List<Map<String, Object>> obtenerPazYSalvosPorResponsable(ResolvedPrincipal actor) {
        return cargarPorResponsable(actor, null);
    }

    public List<Map<String, Object>> obtenerPazYSalvosPendientes(ResolvedPrincipal actor) {
        return cargarPorResponsable(actor, "PENDIENTE");
    }

    private List<Map<String, Object>> cargarPorResponsable(ResolvedPrincipal actor, String estado) {
        List<PazYSalvo> lista;
        if (actor.isAdmin()) {
            lista = estado != null
                    ? pazYSalvoRepository.findByResponsableAdmin_IdAndEstado(actor.id(), estado)
                    : pazYSalvoRepository.findByResponsableAdmin_Id(actor.id());
        } else {
            lista = estado != null
                    ? pazYSalvoRepository.findByResponsableUsuario_IdAndEstado(actor.id(), estado)
                    : pazYSalvoRepository.findByResponsableUsuario_Id(actor.id());
        }
        return lista.stream().map(this::mapearPazYSalvoConEstudiante).collect(Collectors.toList());
    }

    /**
     * Retorna el estado completo de paz y salvos de una solicitud de grado.
     */
    public Map<String, Object> obtenerEstadoPazYSalvos(Long solicitudId) {
        List<PazYSalvo> lista = pazYSalvoRepository.findBySolicitudId(solicitudId);
        long total     = lista.size();
        long aprobados = lista.stream().filter(p -> "APROBADO".equals(p.getEstado())).count();
        long rechazados= lista.stream().filter(p -> "RECHAZADO".equals(p.getEstado())).count();
        long pendientes= lista.stream().filter(p -> "PENDIENTE".equals(p.getEstado())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total",         total);
        result.put("aprobados",     aprobados);
        result.put("rechazados",    rechazados);
        result.put("pendientes",    pendientes);
        result.put("todosAprobados", total > 0 && aprobados == total);
        result.put("detalle", lista.stream().map(this::mapearPazYSalvoConEstudiante).collect(Collectors.toList()));
        return result;
    }

    /**
     * Vista de estado del estudiante para el director.
     */
    public List<Map<String, Object>> obtenerEstadoEstudiantes(Usuario director) {
        Long programaId = director.getProgramaAcademico() != null
                ? director.getProgramaAcademico().getId() : null;
        if (programaId == null) return List.of();

        List<Usuario> estudiantes = usuarioRepository.findByProgramaAcademicoIdAndRol_Nombre(programaId, "ESTUDIANTE");
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Usuario est : estudiantes) {
            resultado.add(calcularEstadoEstudiante(est));
        }
        return resultado;
    }

    private Map<String, Object> calcularEstadoEstudiante(Usuario est) {
        Estudiante perfil = estudianteRepository.findByUsuario(est).orElse(null);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("cedula", est.getCedula());
        info.put("nombre", est.getNombre());
        info.put("codigo", est.getCodigo());
        int creditos      = perfil != null && perfil.getCreditosAprobados() != null ? perfil.getCreditosAprobados() : 0;
        int totalCreditos = est.getProgramaAcademico() != null ? est.getProgramaAcademico().getTotalCreditos() : 0;
        info.put("creditosAprobados", creditos);
        info.put("totalCreditos",     totalCreditos);

        boolean graduado = perfil != null
                && perfil.getEstadoEstudiante() != null
                && "GRADUADO".equals(perfil.getEstadoEstudiante().getNombre());
        if (graduado) {
            info.put("etapa",      "GRADUADO");
            info.put("etapaLabel", "Graduado");
            return info;
        }

        Optional<Solicitud> solicitudGrado =
            solicitudRepository.findFirstByCedulaAndTipo(est.getCedula(), "GRADO");
        if (solicitudGrado.isPresent()) {
            String estado = solicitudGrado.get().getEstado();
            if ("APROBADA".equals(estado)) {
                LocalDate fechaGrado = solicitudGrado.get().getFechaGrado();
                if (fechaGrado != null) {
                    String fechaFmt = fechaGrado.format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    info.put("etapa",      "FECHA_GRADO_ASIGNADA");
                    info.put("etapaLabel", "Fecha prevista: " + fechaFmt);
                    info.put("fechaGrado", fechaGrado.toString());
                } else {
                    info.put("etapa",      "SOLICITUD_GRADO_APROBADA");
                    info.put("etapaLabel", "Grado aprobado");
                }
            } else if ("RECHAZADA".equals(estado)) {
                info.put("etapa",      "SOLICITUD_GRADO_RECHAZADA");
                info.put("etapaLabel", "Solicitud de grado rechazada");
            } else {
                info.put("etapa",      "SOLICITUD_GRADO");
                info.put("etapaLabel", "Solicitud de grado en proceso");
            }
            info.put("solicitudGradoId",    solicitudGrado.get().getId());
            info.put("solicitudGradoEstado", estado);
            return info;
        }

        Optional<Solicitud> terminacion =
            solicitudRepository.findFirstByCedulaAndTipo(est.getCedula(), "TERMINACION_MATERIAS");
        if (terminacion.isPresent()) {
            String estado = terminacion.get().getEstado();
            info.put("etapa",            "MATERIAS_TERMINADAS");
            info.put("etapaLabel",       "APROBADA".equals(estado)
                    ? "Materias terminadas" : "Terminación de materias en proceso");
            info.put("terminacionEstado", estado);
            return info;
        }

        if (creditos >= totalCreditos) {
            info.put("etapa",      "REQUISITOS_COMPLETOS");
            info.put("etapaLabel", "Requisitos completos – puede iniciar terminación");
        } else {
            info.put("etapa",      "CURSANDO");
            info.put("etapaLabel", "Cursando materias");
        }
        return info;
    }

    private Map<String, Object> mapearPazYSalvo(PazYSalvo ps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                ps.getId());
        m.put("solicitudId",       ps.getSolicitudId());
        m.put("cedulaEstudiante",  ps.getCedulaEstudiante());
        m.put("responsableAdminId",   ps.getResponsableAdminId());
        m.put("responsableUsuarioId", ps.getResponsableUsuarioId());
        // Nombre legible del responsable para la UI
        if (ps.getResponsableAdmin() != null) {
            m.put("responsableNombre", ps.getResponsableAdmin().getNombreCompleto());
        } else if (ps.getResponsableUsuario() != null) {
            m.put("responsableNombre", ps.getResponsableUsuario().getNombreCompleto());
        } else {
            m.put("responsableNombre", null);
        }
        m.put("tipoDependencia",   ps.getTipoDependencia());
        m.put("estado",            ps.getEstado());
        m.put("observaciones",     ps.getObservaciones());
        m.put("fechaSolicitud",    ps.getFechaSolicitud());
        m.put("fechaRespuesta",    ps.getFechaRespuesta());
        return m;
    }

    private Map<String, Object> mapearPazYSalvoConEstudiante(PazYSalvo ps) {
        Map<String, Object> m = mapearPazYSalvo(ps);
        usuarioRepository.findByCedula(ps.getCedulaEstudiante()).ifPresent(est -> {
            m.put("nombreEstudiante", est.getNombre());
            m.put("codigoEstudiante", est.getCodigo());
            m.put("programa", est.getProgramaAcademico() != null
                    ? est.getProgramaAcademico().getNombre() : null);
        });
        return m;
    }
}
