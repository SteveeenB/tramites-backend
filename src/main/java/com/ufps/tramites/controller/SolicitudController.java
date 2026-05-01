package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.SolicitudService;
import com.ufps.tramites.service.UsuarioService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    @Autowired private SolicitudService solicitudService;
    @Autowired private UsuarioService usuarioService;

    /** POST /api/solicitudes/terminacion-materias?cedula=... */
    @PostMapping("/terminacion-materias")
    public ResponseEntity<?> crearSolicitudTerminacion(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crearSolicitudTerminacion(estudiante));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/grado?cedula=... */
    @PostMapping("/grado")
    public ResponseEntity<?> crearSolicitudGrado(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crearSolicitudGrado(estudiante));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** GET /api/solicitudes?cedula=... */
    @GetMapping
    public ResponseEntity<?> obtenerSolicitudes(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        return ResponseEntity.ok(solicitudService.obtenerSolicitudesPorCedula(cedula));
    }

    /** GET /api/solicitudes/bandeja?cedula=... — bandeja TERMINACION_MATERIAS del director */
    @GetMapping("/bandeja")
    public ResponseEntity<?> obtenerBandeja(@RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Director no encontrado"));
        if (!"DIRECTOR".equals(director.getRol())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores de programa"));
        return ResponseEntity.ok(solicitudService.obtenerBandejaDirector(director));
    }

    /** GET /api/solicitudes/bandeja-grado?cedula=... — bandeja GRADO del director */
    @GetMapping("/bandeja-grado")
    public ResponseEntity<?> obtenerBandejaGrado(@RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Director no encontrado"));
        if (!"DIRECTOR".equals(director.getRol())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores de programa"));
        return ResponseEntity.ok(solicitudService.obtenerBandejaGrado(director));
    }

    /**
     * GET /api/solicitudes/{id}/documentos
     * Retorna el estado de cada documento requerido para la solicitud de grado.
     * Paso 1 (carga por el estudiante) irá llenando los cargados=true.
     */
    @GetMapping("/{id}/documentos")
    public ResponseEntity<?> obtenerDocumentos(@PathVariable Long id, @RequestParam String cedula) {
        Usuario usuario = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (usuario == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Usuario no encontrado"));
        if (!"DIRECTOR".equals(usuario.getRol()) && !"ADMIN".equals(usuario.getRol()) && !"ESTUDIANTE".equals(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso no autorizado"));
        }
        return ResponseEntity.ok(solicitudService.obtenerDocumentosSolicitud(id));
    }

    /** POST /api/solicitudes/{id}/aprobar?cedula=... */
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id, @RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Director no encontrado"));
        if (!"DIRECTOR".equals(director.getRol())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores"));
        try {
            return ResponseEntity.ok(solicitudService.aprobarSolicitud(id, cedula));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/{id}/rechazar?cedula=...&motivo=... */
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable Long id, @RequestParam String cedula,
            @RequestParam(required = false) String motivo) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Director no encontrado"));
        if (!"DIRECTOR".equals(director.getRol())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores"));
        try {
            return ResponseEntity.ok(solicitudService.rechazarSolicitud(id, motivo, cedula));
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

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
