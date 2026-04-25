package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.SolicitudService;
import com.ufps.tramites.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    @Autowired
    private UsuarioService usuarioService;

    /** POST /api/solicitudes/terminacion-materias */
    @PostMapping("/terminacion-materias")
    public ResponseEntity<?> crearSolicitudTerminacion(HttpSession session) {
        Usuario estudiante = usuarioDeSession(session);
        if (estudiante == null) return noAutenticado();

        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(solicitudService.crearSolicitudTerminacion(estudiante));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/grado */
    @PostMapping("/grado")
    public ResponseEntity<?> crearSolicitudGrado(HttpSession session) {
        Usuario estudiante = usuarioDeSession(session);
        if (estudiante == null) return noAutenticado();

        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(solicitudService.crearSolicitudGrado(estudiante));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** GET /api/solicitudes — retorna las solicitudes del usuario en sesión */
    @GetMapping
    public ResponseEntity<?> obtenerSolicitudes(HttpSession session) {
        Usuario usuario = usuarioDeSession(session);
        if (usuario == null) return noAutenticado();

        return ResponseEntity.ok(solicitudService.obtenerSolicitudesPorCedula(usuario.getCedula()));
    }

    /** GET /api/solicitudes/bandeja — bandeja del director autenticado */
    @GetMapping("/bandeja")
    public ResponseEntity<?> obtenerBandeja(HttpSession session) {
        Usuario director = usuarioDeSession(session);
        if (director == null) return noAutenticado();
        if (!"DIRECTOR".equals(director.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(error("Acceso restringido a directores de programa"));
        }
        return ResponseEntity.ok(solicitudService.obtenerBandejaDirector(director));
    }

    /** POST /api/solicitudes/{id}/aprobar */
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id, HttpSession session) {
        Usuario director = usuarioDeSession(session);
        if (director == null) return noAutenticado();
        if (!"DIRECTOR".equals(director.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores"));
        }
        try {
            return ResponseEntity.ok(solicitudService.aprobarSolicitud(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/{id}/rechazar?motivo=... */
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            HttpSession session) {
        Usuario director = usuarioDeSession(session);
        if (director == null) return noAutenticado();
        if (!"DIRECTOR".equals(director.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores"));
        }
        try {
            return ResponseEntity.ok(solicitudService.rechazarSolicitud(id, motivo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** GET /api/solicitudes/{id}/certificado */
    @GetMapping("/{id}/certificado")
    public ResponseEntity<byte[]> descargarCertificado(@PathVariable Long id) {
        try {
            byte[] contenido = solicitudService.generarCertificado(id);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"certificado-terminacion-" + id + ".txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(contenido);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).build();
        }
    }

    private Usuario usuarioDeSession(HttpSession session) {
        String cedula = (String) session.getAttribute("usuarioCedula");
        if (cedula == null) return null;
        return usuarioService.obtenerUsuarioPorCedula(cedula);
    }

    private ResponseEntity<?> noAutenticado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
