package com.ufps.tramites.controller;

import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.security.PrincipalResolver;
import com.ufps.tramites.security.ResolvedPrincipal;
import com.ufps.tramites.service.CertificadoService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired private CertificadoService certificadoService;
    @Autowired private PrincipalResolver principalResolver;
    @Autowired private TipoCertificadoRepository tipoCertificadoRepository;

    @GetMapping("/tipos")
    public ResponseEntity<?> obtenerTipos() {
        return ResponseEntity.ok(tipoCertificadoRepository.findByActivoTrue());
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitarCertificado(
            @RequestParam String tipo,
            @RequestParam String modalidad,
            @RequestParam(required = false) String destinatario,
            Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(certificadoService.solicitarCertificado(p.usuario(), tipo, modalidad, destinatario));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @GetMapping
    public ResponseEntity<?> obtenerCertificados(Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(certificadoService.obtenerCertificadosPorCedula(p.cedula()));
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> simularPago(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.ok(certificadoService.simularPago(id, p.cedula()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DEPENDENCIA')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> descargarPdf(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            byte[] bytes = certificadoService.descargarPdf(id, p.cedula(), p.dependenciaId());
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

    @PreAuthorize("hasRole('DEPENDENCIA')")
    @GetMapping("/dependencia/{dependenciaId}")
    public ResponseEntity<?> bandejaDependencia(@PathVariable Long dependenciaId,
                                                @RequestParam(required = false) String estado,
                                                Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (p.dependenciaId() == null || !p.dependenciaId().equals(dependenciaId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso denegado"));
        String filtro = (estado == null || estado.isBlank() || "TODOS".equalsIgnoreCase(estado)) ? null : estado;
        return ResponseEntity.ok(certificadoService.obtenerPorDependencia(dependenciaId, filtro));
    }

    @PreAuthorize("hasRole('DEPENDENCIA')")
    @PostMapping("/{id}/marcar-listo")
    public ResponseEntity<?> marcarListo(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (p.dependenciaId() == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Sin dependencia asignada"));
        try {
            return ResponseEntity.ok(certificadoService.marcarListoRetiro(id, p.dependenciaId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('DEPENDENCIA')")
    @PostMapping("/{id}/marcar-entregado")
    public ResponseEntity<?> marcarEntregado(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (p.dependenciaId() == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Sin dependencia asignada"));
        try {
            return ResponseEntity.ok(certificadoService.marcarEntregado(id, p.dependenciaId()));
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
