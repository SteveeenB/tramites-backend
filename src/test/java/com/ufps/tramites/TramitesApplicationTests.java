package com.ufps.tramites;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// FIX TP-195 (Kevin Arias, 07/10/2026): fijamos las properties del perfil h2
// directamente en el test porque la activación de perfiles externa
// (@ActiveProfiles o SPRING_PROFILES_ACTIVE desde surefire) choca con el
// placeholder `spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}` del
// main `application.properties` en Spring Boot 4 / Spring 7 y deja de
// cargar las claves base. Con @TestPropertySource se evita el mecanismo de
// perfiles y queda `./mvnw test` funcional en un clon limpio sin variables
// de entorno.
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:tramites;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
        "demo.auth.enabled=true",
        "jwt.secret=test-only-not-for-production-min-32-characters-ok"
})
class TramitesApplicationTests {

	@Test
	void contextLoads() {
	}

}
