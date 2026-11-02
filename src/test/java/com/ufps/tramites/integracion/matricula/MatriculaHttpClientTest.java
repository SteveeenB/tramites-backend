package com.ufps.tramites.integracion.matricula;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.ufps.tramites.integracion.matricula.ConsultaMatricula.Resultado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// TP-146: adaptador del Módulo de Matrícula contra un servidor simulado.
// Cubre el contrato v1 (TP-144), el login técnico y los umbrales de RNF06.
class MatriculaHttpClientTest {

    private static final String BASE = "http://matricula.test/posgrados-project";
    private static final String CEDULA = "1090000001";
    private static final String URL_SITUACION = BASE + "/api/application/case/integracion/v1/estudiantes/" + CEDULA;
    private static final String URL_SALUD = BASE + "/actuator/health";
    private static final String URL_LOGIN = BASE + "/auth/login";

    private static final String RESPUESTA_OK = """
            {
              "cedula": "1090000001", "codigo": "11550007",
              "nombres": "Laura Marcela", "apellidos": "Pérez Gómez",
              "email": "laura.perez@example.com", "estado": "ACTIVO",
              "programa": { "id": 12, "codigo": "1155", "nombre": "Maestría de ejemplo", "creditosRequeridos": 56 },
              "admision": { "cohorteId": 34, "cohorte": "Cohorte 2025-1", "semestre": "2025-1",
                            "fechaAdmision": "2025-01-20", "fechaIngreso": "2025-02-03" },
              "matricula": { "estado": "COMPLETADO", "valor": 4500000, "fechaVencimiento": "2025-02-01" },
              "creditos": { "requeridos": 56, "aprobados": 40 },
              "campoNuevoQueTramitesNoConoce": true
            }
            """;

    private RelojFalso reloj;
    private MockRestServiceServer server;
    private MatriculaHttpClient client;

    @BeforeEach
    void setUp() {
        reloj = new RelojFalso();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MatriculaHttpClient(builder, BASE, "svc-tramites", "clave",
                Duration.ofSeconds(7), 2, Duration.ofSeconds(15), reloj);
    }

    @Test
    void respuestaValida_traduceTodosLosCamposYEnviaCredencial() {
        esperarLogin("token-servicio");
        server.expect(requestTo(URL_SITUACION))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-servicio"))
                .andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

        ConsultaMatricula c = client.consultarSituacion(CEDULA);

        server.verify();
        assertThat(c.resultado()).isEqualTo(Resultado.ENCONTRADO);
        SituacionAcademica s = c.situacion();
        assertThat(s.cedula()).isEqualTo(CEDULA);
        assertThat(s.nombres()).isEqualTo("Laura Marcela");
        assertThat(s.programaId()).isEqualTo(12L);
        assertThat(s.programaNombre()).isEqualTo("Maestría de ejemplo");
        assertThat(s.fechaIngreso()).isEqualTo(LocalDate.of(2025, 2, 3));
        assertThat(s.estadoMatricula()).isEqualTo("COMPLETADO");
        assertThat(s.valorMatricula()).isEqualByComparingTo(new BigDecimal("4500000"));
        assertThat(s.creditosRequeridos()).isEqualTo(56);
        assertThat(s.creditosAprobados()).isEqualTo(40);
    }

    @Test
    void creditosAprobadosNulo_esValido() {
        // Brecha B1: Matrícula no registra créditos aprobados; el contrato admite null.
        esperarLogin("token-servicio");
        server.expect(requestTo(URL_SITUACION)).andRespond(withSuccess(
                RESPUESTA_OK.replace("\"aprobados\": 40", "\"aprobados\": null"), MediaType.APPLICATION_JSON));

        ConsultaMatricula c = client.consultarSituacion(CEDULA);

        assertThat(c.resultado()).isEqualTo(Resultado.ENCONTRADO);
        assertThat(c.situacion().creditosAprobados()).isNull();
    }

    @Test
    void cedula404_noEncontradoSinReintentar() {
        esperarLogin("token-servicio");
        server.expect(ExpectedCount.once(), requestTo(URL_SITUACION))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"ESTUDIANTE_NOT_FOUND\",\"message\":\"No existe\",\"param\":null}"));

        ConsultaMatricula c = client.consultarSituacion(CEDULA);

        server.verify();
        assertThat(c.resultado()).isEqualTo(Resultado.NO_ENCONTRADO);
        assertThat(c.mensaje()).contains("admitido y legalizado");
    }

    @Test
    void error400_cedulaInvalidaSinReintentar() {
        esperarLogin("token-servicio");
        server.expect(ExpectedCount.once(), requestTo(URL_SITUACION))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.CEDULA_INVALIDA);
        server.verify();
    }

    @Test
    void cedulaConFormatoInvalido_noLlamaAlServicio() {
        // Sin expectativas: cualquier llamada haría fallar la prueba.
        assertThat(client.consultarSituacion("10.900.001").resultado()).isEqualTo(Resultado.CEDULA_INVALIDA);
        assertThat(client.consultarSituacion("../admin").resultado()).isEqualTo(Resultado.CEDULA_INVALIDA);
        assertThat(client.consultarSituacion(null).resultado()).isEqualTo(Resultado.CEDULA_INVALIDA);
        server.verify();
    }

    @Test
    void errores5xx_reintentaHastaDosVecesYRecupera() {
        esperarLogin("token-servicio");
        server.expect(requestTo(URL_SITUACION)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo(URL_SITUACION)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(URL_SITUACION)).andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.ENCONTRADO);
        server.verify();
    }

    @Test
    void servicioCaido_tresIntentosYNoDisponible() {
        esperarLogin("token-servicio");
        server.expect(ExpectedCount.times(3), requestTo(URL_SITUACION))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        ConsultaMatricula c = client.consultarSituacion(CEDULA);

        server.verify();
        assertThat(c.resultado()).isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(c.situacion()).isNull();
        assertThat(c.mensaje()).contains("no está disponible");
    }

    @Test
    void timeout_seReintentaYTerminaEnNoDisponible() {
        esperarLogin("token-servicio");
        server.expect(ExpectedCount.times(3), requestTo(URL_SITUACION))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.NO_DISPONIBLE);
        server.verify();
    }

    @Test
    void intentosLentos_noSePasaDelPresupuestoTotal() {
        // Cada intento agota 7 s (2 s conexión + 5 s respuesta). Tras dos intentos
        // (14 s) un tercero podría llegar a 21 s > 15 s, así que no se lanza.
        esperarLogin("token-servicio");
        server.expect(ExpectedCount.times(2), requestTo(URL_SITUACION))
                .andRespond(request -> {
                    reloj.avanzar(Duration.ofSeconds(7));
                    throw new SocketTimeoutException("Read timed out");
                });

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.NO_DISPONIBLE);
        server.verify();
        assertThat(reloj.transcurrido()).isLessThanOrEqualTo(Duration.ofSeconds(15));
    }

    @Test
    void credencialRechazadaUnaVez_renuevaElTokenYRecupera() {
        esperarLogin("token-viejo");
        server.expect(requestTo(URL_SITUACION))
                .andExpect(header("Authorization", "Bearer token-viejo"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        esperarLogin("token-nuevo");
        server.expect(requestTo(URL_SITUACION))
                .andExpect(header("Authorization", "Bearer token-nuevo"))
                .andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.ENCONTRADO);
        server.verify();
    }

    @Test
    void credencialRechazadaDosVeces_noDisponibleSinMasIntentos() {
        esperarLogin("token-servicio");
        server.expect(requestTo(URL_SITUACION)).andRespond(withStatus(HttpStatus.FORBIDDEN));
        esperarLogin("token-servicio");
        server.expect(requestTo(URL_SITUACION)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.NO_DISPONIBLE);
        server.verify();
    }

    @Test
    void respuestaSinCampoObligatorio_esInvalidaYNoSeReintenta() {
        esperarLogin("token-servicio");
        server.expect(ExpectedCount.once(), requestTo(URL_SITUACION)).andRespond(withSuccess(
                RESPUESTA_OK.replace("\"estado\": \"ACTIVO\",", ""), MediaType.APPLICATION_JSON));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.RESPUESTA_INVALIDA);
        server.verify();
    }

    @Test
    void respuestaDeOtraCedula_esInvalida() {
        esperarLogin("token-servicio");
        server.expect(requestTo(URL_SITUACION)).andRespond(withSuccess(
                RESPUESTA_OK.replace("\"cedula\": \"1090000001\"", "\"cedula\": \"1090000002\""),
                MediaType.APPLICATION_JSON));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.RESPUESTA_INVALIDA);
    }

    @Test
    void fechaFueraDeIso8601_esInvalida() {
        esperarLogin("token-servicio");
        server.expect(requestTo(URL_SITUACION)).andRespond(withSuccess(
                RESPUESTA_OK.replace("\"2025-02-03\"", "\"03/02/2025\""), MediaType.APPLICATION_JSON));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.RESPUESTA_INVALIDA);
    }

    @Test
    void cuerpoQueNoEsJson_esInvalido() {
        esperarLogin("token-servicio");
        server.expect(requestTo(URL_SITUACION)).andRespond(withSuccess("<html>proxy</html>", MediaType.TEXT_HTML));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.RESPUESTA_INVALIDA);
    }

    @Test
    void tokenVigente_seReutilizaEntreConsultas() {
        esperarLogin("token-servicio");
        server.expect(ExpectedCount.times(2), requestTo(URL_SITUACION))
                .andExpect(header("Authorization", "Bearer token-servicio"))
                .andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

        client.consultarSituacion(CEDULA);
        reloj.avanzar(Duration.ofMinutes(100));
        client.consultarSituacion(CEDULA);

        server.verify();
    }

    @Test
    void tokenVencido_vuelveAHacerLogin() {
        esperarLogin("token-1");
        server.expect(requestTo(URL_SITUACION)).andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));
        esperarLogin("token-2");
        server.expect(requestTo(URL_SITUACION))
                .andExpect(header("Authorization", "Bearer token-2"))
                .andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

        client.consultarSituacion(CEDULA);
        reloj.avanzar(MatriculaHttpClient.VIGENCIA_TOKEN);
        client.consultarSituacion(CEDULA);

        server.verify();
    }

    @Test
    void loginRechazado_noDisponibleYSinCredencial() {
        // Cuenta técnica mal configurada: el login falla dos veces y la consulta sale sin token.
        server.expect(ExpectedCount.times(1), requestTo(URL_LOGIN)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(URL_SITUACION)).andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(ExpectedCount.times(1), requestTo(URL_LOGIN)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(URL_SITUACION)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThat(client.consultarSituacion(CEDULA).resultado()).isEqualTo(Resultado.NO_DISPONIBLE);
        server.verify();
    }

    @Test
    void salud_200EsDisponible() {
        server.expect(requestTo(URL_SALUD)).andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        assertThat(client.estaDisponible()).isTrue();
    }

    @Test
    void salud_sinRespuestaNoEsDisponible() {
        server.expect(requestTo(URL_SALUD)).andRespond(withException(new SocketTimeoutException("connect timed out")));

        assertThat(client.estaDisponible()).isFalse();
    }

    @Test
    void modoHttpSinUrl_fallaAlArrancar() {
        assertThatThrownBy(() -> new MatriculaHttpClient(RestClient.builder(), "", "u", "p",
                Duration.ofSeconds(7), 2, Duration.ofSeconds(15), reloj))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MATRICULA_URL");
    }

    /** Login técnico esperado: POST /auth/login con el rol SERVICIO_TRAMITES. */
    private void esperarLogin(String accessToken) {
        server.expect(requestTo(URL_LOGIN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.username").value("svc-tramites"))
                .andExpect(jsonPath("$.requestedRole").value("SERVICIO_TRAMITES"))
                .andRespond(withSuccess("{\"accessToken\":\"" + accessToken + "\",\"refreshToken\":\"r\"}",
                        MediaType.APPLICATION_JSON));
    }

    /** Reloj controlado por la prueba para simular intentos lentos sin esperar. */
    private static class RelojFalso extends Clock {
        private final Instant inicio = Instant.parse("2026-10-10T12:00:00Z");
        private Instant ahora = inicio;

        void avanzar(Duration d) { ahora = ahora.plus(d); }
        Duration transcurrido() { return Duration.between(inicio, ahora); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }
}
