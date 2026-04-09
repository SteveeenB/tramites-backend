package com.ufps.tramites.repository;

import com.ufps.tramites.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Usuario findByCodigo(String codigo);
}
