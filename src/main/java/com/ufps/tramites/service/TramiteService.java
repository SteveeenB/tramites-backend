package com.ufps.tramites.service;

import com.ufps.tramites.model.Usuario;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TramiteService {

    public Map<String, Object> construirModuloPorRol(Usuario usuario) {
        String rol = usuario.getRol();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("modulo", "TRAMITES");
        response.put("usuario", construirUsuario(usuario));
        response.put("sidebar", construirSidebar(rol));
        response.put("acciones", construirAcciones(rol));

        return response;
    }

    private Map<String, Object> construirUsuario(Usuario usuario) {
        Map<String, Object> usuarioMap = new LinkedHashMap<>();
        usuarioMap.put("cedula", usuario.getCedula());
        usuarioMap.put("nombre", usuario.getNombre());
        usuarioMap.put("codigo", usuario.getCodigo());
        usuarioMap.put("rol", usuario.getRol());
        return usuarioMap;
    }

    private List<Map<String, Object>> construirSidebar(String rol) {
        List<Map<String, Object>> menu = new ArrayList<>();

        menu.add(item("inicio", "Resumen", "/tramites"));

        switch (rol) {
            case "ESTUDIANTE":
                menu.add(item("nueva", "Nueva solicitud", "/tramites/nueva"));
                menu.add(item("mis", "Mis solicitudes", "/tramites/mis-solicitudes"));
                break;
            case "DIRECTOR":
                menu.add(item("bandeja", "Bandeja de aprobacion", "/tramites/aprobaciones"));
                menu.add(item("historial", "Historial de decisiones", "/tramites/historial"));
                break;
            case "ADMIN":
                menu.add(item("panel", "Panel general", "/tramites/panel"));
                menu.add(item("config", "Configuracion", "/tramites/configuracion"));
                menu.add(item("usuarios", "Gestion de usuarios", "/tramites/usuarios"));
                break;
            default:
                menu.add(item("consulta", "Consulta", "/tramites/consulta"));
                break;
        }

        return menu;
    }

    private List<Map<String, Object>> construirAcciones(String rol) {
        List<Map<String, Object>> acciones = new ArrayList<>();

        boolean puedeCrear = "ESTUDIANTE".equals(rol) || "ADMIN".equals(rol);
        boolean puedeAprobar = "DIRECTOR".equals(rol) || "ADMIN".equals(rol);
        boolean puedeRechazar = "DIRECTOR".equals(rol) || "ADMIN".equals(rol);
        boolean puedeGestionar = "ADMIN".equals(rol);

        acciones.add(accion("CREAR_SOLICITUD", "Crear solicitud", puedeCrear));
        acciones.add(accion("APROBAR_SOLICITUD", "Aprobar", puedeAprobar));
        acciones.add(accion("RECHAZAR_SOLICITUD", "Rechazar", puedeRechazar));
        acciones.add(accion("GESTION_TOTAL", "Gestion total", puedeGestionar));

        return acciones;
    }

    private Map<String, Object> item(String id, String label, String ruta) {
        Map<String, Object> menuItem = new LinkedHashMap<>();
        menuItem.put("id", id);
        menuItem.put("label", label);
        menuItem.put("ruta", ruta);
        return menuItem;
    }

    private Map<String, Object> accion(String codigo, String label, boolean habilitada) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("codigo", codigo);
        accion.put("label", label);
        accion.put("habilitada", habilitada);
        return accion;
    }
}
