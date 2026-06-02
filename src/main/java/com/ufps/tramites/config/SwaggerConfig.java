package com.ufps.tramites.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

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

                                **Autenticación:** JWT Bearer token. Obtén el token en `POST /api/auth/login` \
                                y agrégalo con el botón **Authorize** (formato: `Bearer <token>`).

                                Endpoints públicos (no requieren token): `POST /api/auth/**`, \
                                `GET /api/solicitudes/verificar`, `GET /api/notificaciones/subscribe`.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo TPostgrados – AyD")
                                .email("contacto@ufps.edu.co")))
                .servers(List.of(
                        new Server().url("https://tramites-backend.onrender.com").description("Producción (Render)"),
                        new Server().url("http://localhost:8080").description("Local")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en POST /api/auth/login")));
    }
}
