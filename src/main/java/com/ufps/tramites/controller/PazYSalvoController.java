package com.ufps.tramites.controller;

import com.ufps.tramites.model.PazYSalvo;
import com.ufps.tramites.service.PazYSalvoService;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/paz-y-salvo")
public class PazYSalvoController {

    @Autowired
    private PazYSalvoService pazYSalvoService;

    // ─────────────────────────────────────────────────────────────────────────
    // Para dependencias (acceso por token — sin login)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/paz-y-salvo/token/{token}
     * La dependencia consulta la información del token antes de subir el archivo.
     */
    @GetMapping("/token/{token}")
    public ResponseEntity<?> infoToken(@PathVariable String token) {
        try {
            return ResponseEntity.ok(pazYSalvoService.infoToken(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    /**
     * POST /api/paz-y-salvo/subir/{token}
     * La dependencia sube su documento usando el token del correo.
     */
    @PostMapping(value = "/subir/{token}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirPorToken(
            @PathVariable String token,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            return ResponseEntity.ok(pazYSalvoService.subirPorToken(token, archivo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("Error al guardar el archivo: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Para el Director (acceso autenticado por cédula)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/paz-y-salvo/director/pendientes?cedula=...
     * Lista de estudiantes donde el director tiene su paz y salvo pendiente.
     */
    @GetMapping("/director/pendientes")
    public ResponseEntity<?> pendientesDirector(@RequestParam String cedula) {
        try {
            return ResponseEntity.ok(pazYSalvoService.obtenerPendientesDirector(cedula));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    /**
     * POST /api/paz-y-salvo/director/subir?cedulaDirector=...&cedulaEstudiante=...
     * El director sube su paz y salvo para un estudiante específico.
     */
    @PostMapping(value = "/director/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirDirector(
            @RequestParam String cedulaDirector,
            @RequestParam String cedulaEstudiante,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            return ResponseEntity.ok(
                pazYSalvoService.subirDirector(cedulaDirector, cedulaEstudiante, archivo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("Error al guardar el archivo: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Para el Estudiante (ver estado de sus paz y salvos)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/paz-y-salvo/estudiante/{cedula}
     * El estudiante consulta el estado de sus paz y salvos por dependencia.
     */
    @GetMapping("/estudiante/{cedula}")
    public ResponseEntity<?> estadoEstudiante(@PathVariable String cedula) {
        return ResponseEntity.ok(pazYSalvoService.obtenerEstadoPorEstudiante(cedula));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Descarga de archivos
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/paz-y-salvo/{id}/archivo
     * Descarga el archivo de un paz y salvo guardado.
     */
    @GetMapping("/{id}/archivo")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long id) {
        try {
            PazYSalvo pys = pazYSalvoService.obtenerParaDescarga(id);
            if (pys.getArchivoContenido() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            String tipo  = pys.getArchivoTipo() != null ? pys.getArchivoTipo() : "application/octet-stream";
            String nombre = pys.getArchivoNombre() != null ? pys.getArchivoNombre() : "paz-y-salvo";
            return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType(tipo))
                .body(pys.getArchivoContenido());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", mensaje);
        return m;
    }
}
