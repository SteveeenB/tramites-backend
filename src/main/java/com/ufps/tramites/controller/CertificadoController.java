package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.service.CertificadoService;
import com.ufps.tramites.service.UsuarioService;
import java.util.LinkedHashMap;
import java.util.List;
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
    @Autowired private UsuarioService usuarioService;
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
        Usuario estudiante = resolverUsuario(auth);
        if (estudiante == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(certificadoService.solicitarCertificado(estudiante, tipo, modalidad, destinatario));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @GetMapping
    public ResponseEntity<?> obtenerCertificados(Authentication auth) {
        Usuario estudiante = resolverUsuario(auth);
        if (estudiante == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(certificadoService.obtenerCertificadosPorCedula(estudiante.getCedula()));
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> simularPago(@PathVariable Long id, Authentication auth) {
        Usuario estudiante = resolverUsuario(auth);
        if (estudiante == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.ok(certificadoService.simularPago(id, estudiante.getCedula()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DEPENDENCIA')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> descargarPdf(@PathVariable Long id, Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            byte[] bytes = certificadoService.descargarPdf(id, u.getCedula());
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
    @GetMapping("/dependencia/{cedulaDependencia}")
    public ResponseEntity<?> bandejaDependencia(@PathVariable String cedulaDependencia,
                                                @RequestParam(required = false) String estado,
                                                Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!u.getCedula().equals(cedulaDependencia))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso denegado"));
        String filtro = (estado == null || estado.isBlank() || "TODOS".equalsIgnoreCase(estado)) ? null : estado;
        return ResponseEntity.ok(certificadoService.obtenerPorDependencia(cedulaDependencia, filtro));
    }

    @PreAuthorize("hasRole('DEPENDENCIA')")
    @PostMapping("/{id}/marcar-listo")
    public ResponseEntity<?> marcarListo(@PathVariable Long id, Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.ok(certificadoService.marcarListoRetiro(id, u.getCedula()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('DEPENDENCIA')")
    @PostMapping("/{id}/marcar-entregado")
    public ResponseEntity<?> marcarEntregado(@PathVariable Long id, Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.ok(certificadoService.marcarEntregado(id, u.getCedula()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    private Usuario resolverUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioService.obtenerUsuarioPorCedula(auth.getName());
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
