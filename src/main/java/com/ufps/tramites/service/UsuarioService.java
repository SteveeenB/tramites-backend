package com.ufps.tramites.service;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.UsuarioRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Usuario guardarUsuario(Usuario usuario) {
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
        return usuarioRepository.findByProgramaAcademicoIdAndRolNombre(programaId, rolNombre);
    }

    public List<Usuario> obtenerPorRol(String rolNombre) {
        return usuarioRepository.findByRolNombre(rolNombre);
    }
}
