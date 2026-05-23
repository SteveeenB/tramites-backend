package com.ufps.tramites.controller;

import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.model.TipoCertificado;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.SolicitudCertificadoRepository;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import com.ufps.tramites.service.CertificadoPdfService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificados")
public class CertificadoController {

    @Autowired private TipoCertificadoRepository tipoCertificadoRepository;
    @Autowired private SolicitudCertificadoRepository solicitudCertificadoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CertificadoPdfService pdfService;

    /** Tipos activos — usados por el formulario del estudiante. */
    @GetMapping("/tipos")
    public List<Map<String, Object>> listarTiposActivos() {
        return tipoCertificadoRepository.findByActivoTrue().stream()
                .map(this::mapearTipo)
                .collect(Collectors.toList());
    }

    /** Historial de solicitudes del estudiante autenticado. */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @GetMapping
    public ResponseEntity<?> miHistorial(Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<Map<String, Object>> lista = solicitudCertificadoRepository
                .findByCedulaOrderByFechaSolicitudDesc(usuario.getCedula())
                .stream().map(s -> mapearSolicitud(s, null)).collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    /** Crea una nueva solicitud de certificado para el estudiante autenticado. */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitar(@RequestParam String tipo,
                                        @RequestParam(defaultValue = "DIGITAL") String modalidad,
                                        Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        TipoCertificado tc = tipoCertificadoRepository.findByCodigo(tipo).orElse(null);
        if (tc == null || !Boolean.TRUE.equals(tc.getActivo())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de certificado no disponible"));
        }

        // Un estudiante no puede tener dos solicitudes pendientes del mismo tipo
        if (solicitudCertificadoRepository.existsByCedulaAndTipoCertificadoAndEstado(
                usuario.getCedula(), tipo, "PENDIENTE_PAGO")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya tiene una solicitud pendiente de pago para este certificado"));
        }

        SolicitudCertificado sol = new SolicitudCertificado();
        sol.setCedula(usuario.getCedula());
        sol.setTipoCertificado(tipo);
        sol.setModalidadEnvio(modalidad);
        sol.setEstado("PENDIENTE_PAGO");
        sol.setCosto(tc.precioTotal(modalidad));
        sol.setCedulaDependencia(tc.getDependenciaCedula());
        sol.setFechaSolicitud(LocalDateTime.now());
        sol.setFechaVencimientoPago(LocalDateTime.now().plusDays(5));

        SolicitudCertificado guardada = solicitudCertificadoRepository.save(sol);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapearSolicitud(guardada, null));
    }

    /** Registra el pago y pasa el estado a GENERADO (listo para descargar). */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Long id, Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        SolicitudCertificado sol = solicitudCertificadoRepository.findById(id).orElse(null);
        if (sol == null) return ResponseEntity.notFound().build();
        if (!sol.getCedula().equals(usuario.getCedula())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (!"PENDIENTE_PAGO".equals(sol.getEstado())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Esta solicitud no está pendiente de pago"));
        }

        sol.setEstado("GENERADO");
        return ResponseEntity.ok(mapearSolicitud(solicitudCertificadoRepository.save(sol), null));
    }

    /** Descarga el PDF del certificado. */
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DEPENDENCIA')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> descargarPdf(@PathVariable Long id, Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        SolicitudCertificado sol = solicitudCertificadoRepository.findById(id).orElse(null);
        if (sol == null) return ResponseEntity.notFound().build();

        boolean esEstudiante = sol.getCedula().equals(usuario.getCedula());
        boolean esDependencia = sol.getCedulaDependencia() != null
                && sol.getCedulaDependencia().equals(usuario.getCedula());
        if (!esEstudiante && !esDependencia) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (!List.of("GENERADO", "LISTO_RETIRO", "ENTREGADO").contains(sol.getEstado())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El certificado aún no está disponible"));
        }

        try {
            Usuario est = usuarioRepository.findByCedula(sol.getCedula()).orElse(null);
            String nombre   = est != null ? est.getNombreCompleto() : sol.getCedula();
            String cedula   = sol.getCedula();
            String codigo   = est != null && est.getCodigo() != null ? est.getCodigo() : "—";
            String programa = est != null && est.getProgramaAcademico() != null
                    ? est.getProgramaAcademico().getNombre() : "—";

            String fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDateTime.now());
            byte[] pdf = pdfService.generar(nombre, cedula, codigo, programa, fmt, fmt, id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "constancia-" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "No se pudo generar el PDF: " + e.getMessage()));
        }
    }

    /** Bandeja de la dependencia: solicitudes que le corresponden gestionar. */
    @PreAuthorize("hasRole('DEPENDENCIA')")
    @GetMapping("/dependencia/{cedula}")
    public ResponseEntity<?> bandejaDependencia(@PathVariable String cedula, Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!cedula.equals(usuario.getCedula())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        List<Map<String, Object>> lista = solicitudCertificadoRepository
                .findByCedulaDependenciaOrderByFechaSolicitudDesc(cedula)
                .stream()
                .filter(s -> List.of("PAGADO", "GENERADO", "LISTO_RETIRO", "ENTREGADO").contains(s.getEstado()))
                .map(s -> {
                    TipoCertificado tc = tipoCertificadoRepository.findByCodigo(s.getTipoCertificado()).orElse(null);
                    return mapearSolicitud(s, tc);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    /** La dependencia marca el certificado físico como listo para retiro. */
    @PreAuthorize("hasRole('DEPENDENCIA')")
    @PostMapping("/{id}/marcar-listo")
    public ResponseEntity<?> marcarListo(@PathVariable Long id,
                                          @RequestParam String cedulaDependencia,
                                          Authentication auth) {
        return cambiarEstadoDependencia(id, cedulaDependencia, "GENERADO", "LISTO_RETIRO", auth);
    }

    /** La dependencia marca el certificado como entregado al estudiante. */
    @PreAuthorize("hasRole('DEPENDENCIA')")
    @PostMapping("/{id}/marcar-entregado")
    public ResponseEntity<?> marcarEntregado(@PathVariable Long id,
                                              @RequestParam String cedulaDependencia,
                                              Authentication auth) {
        SolicitudCertificado sol = cambiarEstadoYRetornar(id, cedulaDependencia, "LISTO_RETIRO", "ENTREGADO", auth);
        if (sol != null) sol.setFechaEntrega(LocalDateTime.now());
        return sol == null
                ? ResponseEntity.badRequest().body(Map.of("error", "No se pudo actualizar el estado"))
                : ResponseEntity.ok(mapearSolicitud(solicitudCertificadoRepository.save(sol), null));
    }

    // ── helpers ───────────────────────────────────────────────────────

    private ResponseEntity<?> cambiarEstadoDependencia(Long id, String cedulaDep,
                                                        String estadoEsperado, String nuevoEstado,
                                                        Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!cedulaDep.equals(usuario.getCedula())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        SolicitudCertificado sol = solicitudCertificadoRepository.findById(id).orElse(null);
        if (sol == null) return ResponseEntity.notFound().build();
        if (!cedulaDep.equals(sol.getCedulaDependencia())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (!estadoEsperado.equals(sol.getEstado())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Estado incorrecto. Se esperaba: " + estadoEsperado));
        }
        sol.setEstado(nuevoEstado);
        return ResponseEntity.ok(mapearSolicitud(solicitudCertificadoRepository.save(sol), null));
    }

    private SolicitudCertificado cambiarEstadoYRetornar(Long id, String cedulaDep,
                                                         String estadoEsperado, String nuevoEstado,
                                                         Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null || !cedulaDep.equals(usuario.getCedula())) return null;
        SolicitudCertificado sol = solicitudCertificadoRepository.findById(id).orElse(null);
        if (sol == null || !cedulaDep.equals(sol.getCedulaDependencia())) return null;
        if (!estadoEsperado.equals(sol.getEstado())) return null;
        sol.setEstado(nuevoEstado);
        return sol;
    }

    private Usuario resolverUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioRepository.findByCedula(auth.getName()).orElse(null);
    }

    private Map<String, Object> mapearTipo(TipoCertificado t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                   t.getId());
        m.put("codigo",               t.getCodigo());
        m.put("label",                t.getLabel());
        m.put("descripcion",          t.getDescripcion());
        m.put("precioDigital",        t.getPrecioDigital());
        m.put("costoLogisticaFisica", t.getCostoLogisticaFisica());
        m.put("dependenciaCedula",    t.getDependenciaCedula());
        m.put("direccionOficina",     t.getDireccionOficina());
        m.put("tiempoEntregaDias",    t.getTiempoEntregaDias());
        m.put("activo",               t.getActivo());
        return m;
    }

    private Map<String, Object> mapearSolicitud(SolicitudCertificado s, TipoCertificado tc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                   s.getId());
        m.put("tipoCertificado",      s.getTipoCertificado());
        m.put("modalidadEnvio",       s.getModalidadEnvio());
        m.put("estado",               s.getEstado());
        m.put("costo",                s.getCosto());
        m.put("fechaSolicitud",       s.getFechaSolicitud());
        m.put("fechaVencimientoPago", s.getFechaVencimientoPago());
        m.put("fechaEntrega",         s.getFechaEntrega());

        if (tc != null) m.put("tipoLabel", tc.getLabel());

        // Datos del estudiante para la bandeja de dependencia
        usuarioRepository.findByCedula(s.getCedula()).ifPresent(est -> {
            Map<String, Object> estMap = new LinkedHashMap<>();
            estMap.put("cedula", est.getCedula());
            estMap.put("nombre", est.getNombreCompleto());
            m.put("estudiante", estMap);
        });
        return m;
    }
}
