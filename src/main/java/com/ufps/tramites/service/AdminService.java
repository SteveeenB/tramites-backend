package com.ufps.tramites.service;

import com.ufps.tramites.model.Admin;
import com.ufps.tramites.model.Dependencia;
import com.ufps.tramites.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminService {

    @Autowired private AdminRepository adminRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DependenciaService dependenciaService;

    /** Guarda un admin nuevo o actualiza uno existente. Encripta la contraseña con BCrypt si aún no lo está. */
    public Admin guardar(Admin admin) {
        if (admin.getPassword() != null && !admin.getPassword().startsWith("$2a$")) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        if (admin.getFechaCreacion() == null) {
            admin.setFechaCreacion(LocalDateTime.now());
        }
        return adminRepository.save(admin);
    }

    public Admin crearAdminDependencia(String nombreCompleto, String codigo, String email,
                                       String contrasena, Long dependenciaId) {
        Dependencia dep = dependenciaId != null ? dependenciaService.obtenerPorId(dependenciaId) : null;
        Admin a = new Admin();
        a.setNombreCompleto(nombreCompleto);
        a.setCodigo(codigo);
        a.setEmail(email);
        a.setPassword(contrasena);
        a.setTipo("DEPENDENCIA");
        a.setEsSuperAdmin(false);
        a.setActive(true);
        a.setDependencia(dep);
        return guardar(a);
    }

    public void eliminarPorCodigo(String codigo) {
        Admin a = adminRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Admin no encontrado: " + codigo));
        adminRepository.delete(a);
    }
}
