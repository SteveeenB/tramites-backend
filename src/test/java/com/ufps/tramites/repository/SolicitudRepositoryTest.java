package com.ufps.tramites.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.ufps.tramites.model.Solicitud;

// FIX TP-195 (Kevin Arias, 07/10/2026): se reescribe como @DataJpaTest real
// (antes se mockeaba el repositorio con Mockito, lo que no probaba nada de
// la capa JPA). Se usa H2 en modo MySQL para que el DDL que genera Hibernate
// con dialecto MySQL (DATETIME, engine=InnoDB) sea aceptado por H2.
@DataJpaTest
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:solicitud-repo-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class SolicitudRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SolicitudRepository solicitudRepository;

    private Solicitud nuevaSolicitud(String cedula, String tipo, String estado) {
        Solicitud s = new Solicitud();
        s.setCedula(cedula);
        s.setTipo(tipo);
        s.setEstado(estado);
        s.setFechaSolicitud(LocalDate.now());
        return s;
    }

    @Test
    void findByEstado_devuelveSoloLasSolicitudesConEseEstado() {
        em.persist(nuevaSolicitud("111", "TERMINACION_MATERIAS", "EN_REVISION"));
        em.persist(nuevaSolicitud("222", "TERMINACION_MATERIAS", "PENDIENTE_PAGO"));
        em.persist(nuevaSolicitud("333", "GRADO", "EN_REVISION"));
        em.flush();

        List<Solicitud> enRevision = solicitudRepository.findByEstado("EN_REVISION");

        assertThat(enRevision)
                .hasSize(2)
                .extracting(Solicitud::getCedula)
                .containsExactlyInAnyOrder("111", "333");
    }

    @Test
    void findByEstado_sinCoincidencias_devuelveListaVacia() {
        em.persist(nuevaSolicitud("111", "TERMINACION_MATERIAS", "PENDIENTE_PAGO"));
        em.flush();

        assertThat(solicitudRepository.findByEstado("EN_REVISION")).isEmpty();
    }

    @Test
    void findFirstByCedulaAndTipoOrderByIdDesc_devuelveLaMasReciente() {
        em.persist(nuevaSolicitud("111", "TERMINACION_MATERIAS", "RECHAZADA"));
        em.persist(nuevaSolicitud("111", "TERMINACION_MATERIAS", "APROBADA"));
        em.flush();

        Optional<Solicitud> ultima = solicitudRepository
                .findFirstByCedulaAndTipoOrderByIdDesc("111", "TERMINACION_MATERIAS");

        assertThat(ultima).isPresent();
        assertThat(ultima.get().getEstado()).isEqualTo("APROBADA");
    }

    @Test
    void findGradoByEstado_filtraPorTipoYEstado() {
        em.persist(nuevaSolicitud("111", "GRADO", "EN_REVISION"));
        em.persist(nuevaSolicitud("222", "GRADO", "APROBADA_DIRECTOR"));
        em.persist(nuevaSolicitud("333", "TERMINACION_MATERIAS", "EN_REVISION"));
        em.flush();

        List<Solicitud> gradoEnRevision = solicitudRepository.findGradoByEstado("EN_REVISION");

        assertThat(gradoEnRevision)
                .hasSize(1)
                .first()
                .extracting(Solicitud::getCedula)
                .isEqualTo("111");
    }
}
