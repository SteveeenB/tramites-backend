package com.ufps.tramites.repository;

import com.ufps.tramites.model.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Usuario findByCodigo(String codigo);
    List<Usuario> findByProgramaAcademicoIdAndRol(Long programaId, String rol);
}
