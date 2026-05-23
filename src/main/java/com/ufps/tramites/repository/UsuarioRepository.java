package com.ufps.tramites.repository;

import com.ufps.tramites.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCedula(String cedula);
    Optional<Usuario> findByCodigo(String codigo);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByGoogleId(String googleId);
    List<Usuario> findByProgramaAcademicoIdAndRol_Nombre(Long programaId, String rolNombre);
    List<Usuario> findByRol_Nombre(String rolNombre);
}
