package com.ufps.tramites.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI tramitesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Módulo de Trámites – TPostgrados API")
                        .description("""
                                API REST para la gestión de trámites académicos de postgrado de la UFPS.

                                **Roles disponibles:**
                                - `ESTUDIANTE` – Consulta su proceso y crea solicitudes
                                - `DIRECTOR` – Aprueba o rechaza solicitudes de su programa
                                - `POSGRADOS` – Valida solicitudes de grado antes de la decisión del director
                                - `DEPENDENCIA` – Gestiona paz y salvos y entrega de certificados físicos
                                - `ADMIN` – Administra catálogo de certificados y convocatorias

                                **Autenticación:** Se pasa la cédula del usuario como query param `?cedula=` en cada endpoint que requiere rol.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo TPostgrados – AyD")
                                .email("contacto@ufps.edu.co")))
                .servers(List.of(
                        new Server().url("https://tramites-backend.onrender.com").description("Producción (Render)"),
                        new Server().url("http://localhost:8080").description("Local")
                ));
    }
}
