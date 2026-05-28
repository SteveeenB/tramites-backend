package com.ufps.tramites.service;

import com.ufps.tramites.model.Dependencia;
import com.ufps.tramites.model.Rol;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.RolRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DependenciaService dependenciaService;

    public Usuario guardarUsuario(Usuario usuario) {
        if (usuario.getPassword() != null && !usuario.getPassword().startsWith("$2a$")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    public Usuario obtenerUsuarioPorCedula(String cedula) {
        return usuarioRepository.findByCedula(cedula).orElse(null);
    }

    public Usuario obtenerUsuarioPorCodigo(String codigo) {
        return usuarioRepository.findByCodigo(codigo).orElse(null);
    }

    public Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    public Usuario obtenerUsuarioPorGoogleId(String googleId) {
        return usuarioRepository.findByGoogleId(googleId).orElse(null);
    }

    public Usuario obtenerPrimerUsuario() {
        return usuarioRepository.findAll().stream().findFirst().orElse(null);
    }

    public List<Usuario> obtenerPorProgramaYRol(Long programaId, String rolNombre) {
        return usuarioRepository.findByProgramaAcademicoIdAndRol_Nombre(programaId, rolNombre);
    }

    public List<Usuario> obtenerPorRol(String rolNombre) {
        return usuarioRepository.findByRol_Nombre(rolNombre);
    }

    public Usuario crearUsuarioDependencia(String nombreCompleto, String cedula,
                                            String codigo, String correo,
                                            String contrasena, Long dependenciaId) {
        Rol rol = rolRepository.findByNombre("DEPENDENCIA")
                .orElseThrow(() -> new IllegalArgumentException("Rol DEPENDENCIA no encontrado"));
        Dependencia dep = dependenciaId != null ? dependenciaService.obtenerPorId(dependenciaId) : null;
        Usuario u = new Usuario();
        u.setNombreCompleto(nombreCompleto);
        u.setCedula(cedula);
        u.setCodigo(codigo);
        u.setEmail(correo);
        u.setPassword(contrasena);
        u.setRol(rol);
        u.setDependencia(dep);
        return guardarUsuario(u);
    }

    public void eliminarUsuarioDependencia(String cedula) {
        Usuario u = usuarioRepository.findByCedula(cedula)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + cedula));
        if (!"DEPENDENCIA".equals(u.getRolNombre()))
            throw new IllegalStateException("El usuario no tiene rol DEPENDENCIA");
        usuarioRepository.delete(u);
    }
}
