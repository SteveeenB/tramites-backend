package com.ufps.tramites.repository;

import com.ufps.tramites.model.Admin;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByCodigo(String codigo);
    Optional<Admin> findByEmail(String email);
    List<Admin> findByTipo(String tipo);
    List<Admin> findByDependencia_Id(Long dependenciaId);
}
