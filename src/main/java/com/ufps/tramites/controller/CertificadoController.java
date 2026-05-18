package com.ufps.tramites.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.service.CertificadoService;
import com.ufps.tramites.service.UsuarioService;

@RestController
@RequestMapping("/api/certificados")
public class CertificadoController {

    @Autowired private CertificadoService certificadoService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private TipoCertificadoRepository tipoCertificadoRepository;

    // ── Catálogo (estudiante) ────────────────────────────────────────────────

    @GetMapping("/tipos")
    public ResponseEntity<?> obtenerTipos() {
        return ResponseEntity.ok(tipoCertificadoRepository.findByActivoTrue());
    }

    // ── Solicitud + pago + listado del estudiante ───────────────────────────

    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitarCertificado(
            @RequestParam String cedula,
            @RequestParam String tipo,
            @RequestParam String modalidad,
            @RequestParam(required = false) String destinatario) {

        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        }
        if (!"ESTUDIANTE".equals(estudiante.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(error("Solo los estudiantes pueden solicitar certificados"));
        }
        try {
            Map<String, Object> resultado = certificadoService.solicitarCertificado(
                estudiante, tipo, modalidad, destinatario);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> obtenerCertificados(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        }
        return ResponseEntity.ok(certificadoService.obtenerCertificadosPorCedula(cedula));
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> simularPago(@PathVariable Long id, @RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        }
        try {
            return ResponseEntity.ok(certificadoService.simularPago(id, cedula));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    // ── Descarga del PDF (estudiante dueño o dependencia encargada) ─────────

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> descargarPdf(@PathVariable Long id, @RequestParam String cedula) {
        try {
            byte[] bytes = certificadoService.descargarPdf(id, cedula);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "constancia-" + id + ".pdf");
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    // ── Bandeja de la dependencia ────────────────────────────────────────────

    @GetMapping("/dependencia/{cedulaDependencia}")
    public ResponseEntity<?> bandejaDependencia(@PathVariable String cedulaDependencia,
                                                @RequestParam(required = false) String estado) {
        Usuario u = usuarioService.obtenerUsuarioPorCedula(cedulaDependencia);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Dependencia no encontrada"));
        }
        if (!"DEPENDENCIA".equals(u.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(error("Solo las dependencias pueden consultar esta bandeja"));
        }
        String filtro = (estado == null || estado.isBlank() || "TODOS".equalsIgnoreCase(estado)) ? null : estado;
        List<Map<String, Object>> data = certificadoService.obtenerPorDependencia(cedulaDependencia, filtro);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/{id}/marcar-listo")
    public ResponseEntity<?> marcarListo(@PathVariable Long id, @RequestParam String cedulaDependencia) {
        try {
            return ResponseEntity.ok(certificadoService.marcarListoRetiro(id, cedulaDependencia));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/marcar-entregado")
    public ResponseEntity<?> marcarEntregado(@PathVariable Long id, @RequestParam String cedulaDependencia) {
        try {
            return ResponseEntity.ok(certificadoService.marcarEntregado(id, cedulaDependencia));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
