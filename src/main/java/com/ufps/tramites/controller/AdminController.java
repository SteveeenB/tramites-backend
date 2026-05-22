package com.ufps.tramites.controller;

import com.ufps.tramites.model.TipoCertificado;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AdminController {

    @Autowired private TipoCertificadoRepository tipoCertificadoRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // ── /dependencias ────────────────────────────────────────────────
    @GetMapping("/dependencias")
    public List<Map<String, Object>> listarDependencias() {
        return usuarioRepository.findByRolNombre("DEPENDENCIA").stream()
                .map(this::mapearDep)
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapearDep(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cedula", u.getCedula());
        m.put("nombre", u.getNombreCompleto());
        m.put("email",  u.getEmail());
        return m;
    }

    // ── /admin/tipos-certificado ─────────────────────────────────────
    @GetMapping("/admin/tipos-certificado")
    public List<Map<String, Object>> listarTipos() {
        return tipoCertificadoRepository.findAll().stream()
                .map(this::mapearTipo)
                .collect(Collectors.toList());
    }

    @PostMapping("/admin/tipos-certificado")
    public ResponseEntity<?> crearTipo(@RequestBody TipoCertificado tipo) {
        if (tipo.getCodigo() == null || tipo.getCodigo().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo código es requerido"));
        }
        if (tipoCertificadoRepository.findByCodigo(tipo.getCodigo()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un tipo con el código: " + tipo.getCodigo()));
        }
        if (tipo.getActivo() == null) tipo.setActivo(true);
        TipoCertificado guardado = tipoCertificadoRepository.save(tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapearTipo(guardado));
    }

    @PutMapping("/admin/tipos-certificado/{id}")
    public ResponseEntity<?> actualizarTipo(@PathVariable Long id, @RequestBody TipoCertificado datos) {
        TipoCertificado tipo = tipoCertificadoRepository.findById(id).orElse(null);
        if (tipo == null) return ResponseEntity.notFound().build();

        tipo.setLabel(datos.getLabel());
        tipo.setDescripcion(datos.getDescripcion());
        tipo.setPrecioDigital(datos.getPrecioDigital());
        tipo.setCostoLogisticaFisica(datos.getCostoLogisticaFisica());
        tipo.setDependenciaCedula(datos.getDependenciaCedula());
        tipo.setDireccionOficina(datos.getDireccionOficina());
        tipo.setTiempoEntregaDias(datos.getTiempoEntregaDias());
        tipo.setActivo(datos.getActivo());

        return ResponseEntity.ok(mapearTipo(tipoCertificadoRepository.save(tipo)));
    }

    @PatchMapping("/admin/tipos-certificado/{id}/activo")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id, @RequestParam boolean valor) {
        TipoCertificado tipo = tipoCertificadoRepository.findById(id).orElse(null);
        if (tipo == null) return ResponseEntity.notFound().build();
        tipo.setActivo(valor);
        return ResponseEntity.ok(mapearTipo(tipoCertificadoRepository.save(tipo)));
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
        // Nombre de la dependencia para mostrar en la tabla
        if (t.getDependenciaCedula() != null) {
            usuarioRepository.findByCedula(t.getDependenciaCedula())
                    .ifPresent(u -> m.put("dependenciaNombre", u.getNombreCompleto()));
        }
        return m;
    }
}
