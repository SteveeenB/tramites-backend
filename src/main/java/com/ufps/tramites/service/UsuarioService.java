package com.ufps.tramites.service;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Usuario autenticar(String codigo, String contrasena) {
        Usuario usuario = usuarioRepository.findByCodigo(codigo);
        
        if (usuario != null && usuario.getContrasena().equals(contrasena)) {
            return usuario;
        }
        return null;
    }

    public Usuario guardarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public Usuario obtenerUsuarioPorCedula(String cedula) {
        return usuarioRepository.findById(cedula).orElse(null);
    }

    public Usuario obtenerUsuarioPorCodigo(String codigo) {
        return usuarioRepository.findByCodigo(codigo);
    }

    public Usuario obtenerPrimerUsuario() {
        return usuarioRepository.findAll().stream().findFirst().orElse(null);
    }
}
