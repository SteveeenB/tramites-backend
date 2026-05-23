package com.ufps.tramites.service;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.UsuarioRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
}
