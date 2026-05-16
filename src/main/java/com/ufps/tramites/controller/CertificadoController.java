package com.ufps.tramites.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private CertificadoService certificadoService;

    @Autowired
    private UsuarioService usuarioService;

    /**
     * POST /api/certificados/solicitar?cedula=...&tipo=...&modalidad=...&destinatario=...
     * Crea una solicitud de certificado académico para el estudiante.
     */
    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitarCertificado(
            @RequestParam String cedula,
            @RequestParam String tipo,
            @RequestParam String modalidad,
            @RequestParam(required = false) String destinatario) {

        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("Estudiante no encontrado"));
        }
        if (!"ESTUDIANTE".equals(estudiante.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(error("Solo los estudiantes pueden solicitar certificados"));
        }
        try {
            Map<String, Object> resultado = certificadoService.solicitarCertificado(
                estudiante, tipo, modalidad, destinatario
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    /**
     * GET /api/certificados?cedula=...
     * Retorna todos los certificados solicitados por el estudiante.
     */
    @GetMapping
    public ResponseEntity<?> obtenerCertificados(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("Estudiante no encontrado"));
        }
        return ResponseEntity.ok(certificadoService.obtenerCertificadosPorCedula(cedula));
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }

    /**
 * POST /api/certificados/{id}/pagar?cedula=...
 * Simula el pago de un certificado (reemplazar con Wompi en TP-82).
 */
@PostMapping("/{id}/pagar")
public ResponseEntity<?> simularPago(
        @PathVariable Long id,
        @RequestParam String cedula) {

    Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
    if (estudiante == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("Estudiante no encontrado"));
    }
    try {
        Map<String, Object> resultado = certificadoService.simularPago(id, cedula);
        return ResponseEntity.status(HttpStatus.OK).body(resultado);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
    }
}

@Autowired
private TipoCertificadoRepository tipoCertificadoRepository;

/**
 * GET /api/certificados/tipos
 * Retorna los tipos de certificado activos desde la BD.
 */
@GetMapping("/tipos")
public ResponseEntity<?> obtenerTipos() {
    return ResponseEntity.ok(tipoCertificadoRepository.findByActivoTrue());
}

}