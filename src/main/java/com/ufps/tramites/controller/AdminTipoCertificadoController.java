package com.ufps.tramites.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ufps.tramites.model.TipoCertificado;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.repository.UsuarioRepository;

/**
 * CRUD de tipos de certificado. Lo consume el panel de administrador
 * (rol ADMIN / POSGRADOS). La distinción entre tipos vive en filas, no en
 * código: añadir un tipo nuevo no requiere desplegar nada.
 */
@Tag(name = "Admin – Tipos de Certificado",
     description = "CRUD del catálogo de tipos de certificado. Requiere rol ADMIN o POSGRADOS.")
@RestController
@RequestMapping("/api/admin/tipos-certificado")
public class AdminTipoCertificadoController {

    @Autowired private TipoCertificadoRepository repository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Operation(summary = "Listar todos los tipos de certificado",
               description = "Devuelve el catálogo completo (activos e inactivos) con el nombre de la dependencia responsable.")
    @ApiResponse(responseCode = "200", description = "Lista de tipos de certificado")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Map<String, Object>> data = repository.findAll().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Obtener un tipo de certificado por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo de certificado encontrado"),
        @ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return repository.findById(id)
                .<ResponseEntity<?>>map(t -> ResponseEntity.ok(toMap(t)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Tipo no encontrado")));
    }

    @Operation(summary = "Crear un nuevo tipo de certificado",
               description = "El campo `codigo` es obligatorio y debe ser único. `precioDigital` es el precio base; `costoLogisticaFisica` es el delta para entrega física.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tipo creado exitosamente"),
        @ApiResponse(responseCode = "409", description = "Ya existe un tipo con ese código"),
        @ApiResponse(responseCode = "422", description = "El campo código es obligatorio")
    })
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        String codigo = strOrNull(body.get("codigo"));
        if (codigo == null || codigo.isBlank()) {
            return ResponseEntity.status(HttpStatus.valueOf(422))
                    .body(Map.of("error", "El código es obligatorio"));
        }
        if (repository.findByCodigo(codigo).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un tipo con el código " + codigo));
        }
        TipoCertificado t = new TipoCertificado();
        t.setCodigo(codigo);
        aplicar(body, t);
        if (t.getActivo() == null) t.setActivo(Boolean.TRUE);
        repository.save(t);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(t));
    }

    @Operation(summary = "Actualizar un tipo de certificado",
               description = "Permite modificar cualquier campo excepto `codigo`. Solo se aplican los campos presentes en el body.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo actualizado"),
        @ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repository.findById(id).<ResponseEntity<?>>map(t -> {
            aplicar(body, t);
            repository.save(t);
            return ResponseEntity.ok(toMap(t));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Tipo no encontrado")));
    }

    @Operation(summary = "Activar o desactivar un tipo de certificado",
               description = "Usa `?valor=true` para activar y `?valor=false` para desactivar. Los tipos inactivos no aparecen al estudiante.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    })
    @PatchMapping("/{id}/activo")
    public ResponseEntity<?> setActivo(
            @PathVariable Long id,
            @Parameter(description = "true = activo, false = inactivo") @RequestParam boolean valor) {
        return repository.findById(id).<ResponseEntity<?>>map(t -> {
            t.setActivo(valor);
            repository.save(t);
            return ResponseEntity.ok(toMap(t));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Tipo no encontrado")));
    }

    private void aplicar(Map<String, Object> body, TipoCertificado t) {
        if (body.containsKey("label"))                t.setLabel(strOrNull(body.get("label")));
        if (body.containsKey("descripcion"))          t.setDescripcion(strOrNull(body.get("descripcion")));
        if (body.containsKey("precioDigital"))        t.setPrecioDigital(numOrNull(body.get("precioDigital")));
        if (body.containsKey("costoLogisticaFisica")) t.setCostoLogisticaFisica(numOrNull(body.get("costoLogisticaFisica")));
        if (body.containsKey("dependenciaCedula"))    t.setDependenciaCedula(strOrNull(body.get("dependenciaCedula")));
        if (body.containsKey("direccionOficina"))     t.setDireccionOficina(strOrNull(body.get("direccionOficina")));
        if (body.containsKey("tiempoEntregaDias"))    t.setTiempoEntregaDias(intOrNull(body.get("tiempoEntregaDias")));
        if (body.containsKey("activo"))               t.setActivo(boolOrNull(body.get("activo")));
    }

    private Map<String, Object> toMap(TipoCertificado t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("codigo", t.getCodigo());
        m.put("label", t.getLabel());
        m.put("descripcion", t.getDescripcion());
        m.put("precioDigital", t.getPrecioDigital());
        m.put("costoLogisticaFisica", t.getCostoLogisticaFisica());
        m.put("dependenciaCedula", t.getDependenciaCedula());
        m.put("direccionOficina", t.getDireccionOficina());
        m.put("tiempoEntregaDias", t.getTiempoEntregaDias());
        m.put("activo", t.getActivo());

        if (t.getDependenciaCedula() != null) {
            usuarioRepository.findById(t.getDependenciaCedula()).ifPresent(u ->
                m.put("dependenciaNombre", u.getNombre()));
        }
        return m;
    }

    // ── helpers de parseo defensivo ──────────────────────────────────────────
    private static String strOrNull(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
    private static Double numOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return null; }
    }
    private static Integer intOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }
    private static Boolean boolOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }
}
