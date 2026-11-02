package com.ufps.tramites.integracion.matricula;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador HTTP del Módulo de Matrícula (TP-146), según el contrato v0.1 de
 * TP-143 y los umbrales de RNF06:
 * <ul>
 *   <li>Tiempo de espera: 2 s de conexión y 5 s de respuesta.</li>
 *   <li>Reintentos: máximo 2, solo ante 5xx o tiempo agotado; nunca ante 4xx.</li>
 *   <li>Credencial: login con la cuenta técnica (rol SERVICIO_TRAMITES) en
 *       POST /auth/login; el token se guarda en memoria 110 min (Matrícula lo
 *       emite por 120).</li>
 *   <li>401/403: renueva la credencial una vez y reintenta; si vuelve a fallar
 *       se trata como indisponibilidad y se registra como error de configuración.</li>
 *   <li>Contingencia: no se inicia un intento que pueda pasarse del presupuesto
 *       total (15 s), así el usuario siempre recibe respuesta a tiempo.</li>
 *   <li>Trazabilidad: cada llamada queda en el log con ruta, cédula, intento,
 *       código HTTP y duración (la fecha y hora las pone el patrón del log).</li>
 * </ul>
 * Nunca lanza excepción hacia el llamador; ver {@link ConsultaMatricula}.
 */
@Component
@ConditionalOnProperty(name = "matricula.modo", havingValue = "http")
public class MatriculaHttpClient implements MatriculaClient {

    private static final Logger log = LoggerFactory.getLogger(MatriculaHttpClient.class);

    static final String RUTA_SITUACION = "/api/application/case/integracion/v1/estudiantes/{cedula}";
    static final String RUTA_SALUD = "/actuator/health";
    static final String RUTA_LOGIN = "/auth/login";
    static final String ROL_TECNICO = "SERVICIO_TRAMITES";
    static final Duration VIGENCIA_TOKEN = Duration.ofMinutes(110);

    private static final int SIN_RESPUESTA = -1;

    private final RestClient restClient;
    private final String usuario;
    private final String password;
    private String token;
    private Instant tokenVence;
    private final Duration duracionMaximaIntento;
    private final int maxReintentos;
    private final Duration presupuesto;
    private final Clock clock;
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Autowired
    public MatriculaHttpClient(
            @Value("${matricula.url:}") String url,
            @Value("${matricula.usuario:}") String usuario,
            @Value("${matricula.password:}") String password,
            @Value("${matricula.timeout-conexion-ms:2000}") long timeoutConexionMs,
            @Value("${matricula.timeout-respuesta-ms:5000}") long timeoutRespuestaMs,
            @Value("${matricula.max-reintentos:2}") int maxReintentos,
            @Value("${matricula.presupuesto-ms:15000}") long presupuestoMs) {
        this(RestClient.builder().requestFactory(fabrica(timeoutConexionMs, timeoutRespuestaMs)),
                url, usuario, password,
                Duration.ofMillis(timeoutConexionMs + timeoutRespuestaMs),
                maxReintentos, Duration.ofMillis(presupuestoMs), Clock.systemUTC());
    }

    /** Para pruebas: permite enlazar MockRestServiceServer y controlar el reloj. */
    MatriculaHttpClient(RestClient.Builder builder, String url, String usuario, String password,
                        Duration duracionMaximaIntento, int maxReintentos,
                        Duration presupuesto, Clock clock) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "matricula.modo=http requiere MATRICULA_URL (ej. https://host/posgrados-project)");
        }
        this.restClient = builder.baseUrl(url).build();
        this.usuario = usuario == null ? "" : usuario;
        this.password = password == null ? "" : password;
        this.duracionMaximaIntento = duracionMaximaIntento;
        this.maxReintentos = maxReintentos;
        this.presupuesto = presupuesto;
        this.clock = clock;
        if (this.usuario.isBlank() || this.password.isBlank()) {
            log.warn("[MATRICULA] MATRICULA_USUARIO o MATRICULA_PASSWORD vacíos: las llamadas irán sin credencial de servicio");
        }
    }

    private static SimpleClientHttpRequestFactory fabrica(long conexionMs, long respuestaMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(conexionMs));
        fabrica.setReadTimeout(Duration.ofMillis(respuestaMs));
        return fabrica;
    }

    @Override
    public ConsultaMatricula consultarSituacion(String cedula) {
        if (!MatriculaClient.cedulaValida(cedula)) {
            log.warn("[MATRICULA] consulta no enviada: cédula con formato inválido");
            return ConsultaMatricula.cedulaInvalida();
        }

        Instant inicio = clock.instant();
        int reintentos = 0;
        boolean credencialRenovada = false;

        for (int intento = 1; ; intento++) {
            Respuesta r = llamar(cedula, intento);
            int status = r.status();

            if (status == SIN_RESPUESTA || status >= 500) {
                if (reintentos < maxReintentos && hayPresupuesto(inicio)) {
                    reintentos++;
                    continue;
                }
                log.error("[MATRICULA] servicio no disponible para cedula={} tras {} intento(s)", cedula, intento);
                return ConsultaMatricula.noDisponible();
            }
            if (status == 401 || status == 403) {
                if (!credencialRenovada && hayPresupuesto(inicio)) {
                    credencialRenovada = true;
                    renovarCredencial();
                    continue;
                }
                log.error("[MATRICULA] credencial de servicio rechazada ({}): error de configuración, revisar MATRICULA_USUARIO, MATRICULA_PASSWORD y que la cuenta tenga el rol Servicio Tramites", status);
                return ConsultaMatricula.noDisponible();
            }
            if (status == 404) {
                return ConsultaMatricula.noEncontrado();
            }
            if (status == 400) {
                return ConsultaMatricula.cedulaInvalida();
            }
            if (status == 200) {
                return traducir(cedula, r.cuerpo());
            }
            log.error("[MATRICULA] código {} fuera del contrato para cedula={}", status, cedula);
            return ConsultaMatricula.respuestaInvalida();
        }
    }

    @Override
    public boolean estaDisponible() {
        long t0 = System.nanoTime();
        try {
            int status = restClient.get()
                    .uri(RUTA_SALUD)
                    .exchange((req, res) -> res.getStatusCode().value());
            log.info("[MATRICULA] GET {} status={} duracionMs={}", RUTA_SALUD, status, msDesde(t0));
            return status == 200;
        } catch (RestClientException e) {
            log.warn("[MATRICULA] GET {} sin respuesta duracionMs={} causa={}", RUTA_SALUD, msDesde(t0), e.getMessage());
            return false;
        }
    }

    private Respuesta llamar(String cedula, int intento) {
        long t0 = System.nanoTime();
        try {
            Respuesta r = restClient.get()
                    .uri(RUTA_SITUACION, cedula)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(this::autenticar)
                    .exchange((req, res) -> new Respuesta(
                            res.getStatusCode().value(),
                            StreamUtils.copyToByteArray(res.getBody())));
            log.info("[MATRICULA] GET {} cedula={} intento={} status={} duracionMs={}",
                    RUTA_SITUACION, cedula, intento, r.status(), msDesde(t0));
            return r;
        } catch (RestClientException e) {
            // ResourceAccessException: timeout de conexión o de respuesta, conexión rechazada, DNS.
            log.warn("[MATRICULA] GET {} cedula={} intento={} status=SIN_RESPUESTA duracionMs={} causa={}",
                    RUTA_SITUACION, cedula, intento, msDesde(t0), e.getMessage());
            return new Respuesta(SIN_RESPUESTA, null);
        }
    }

    private void autenticar(HttpHeaders headers) {
        String vigente = tokenVigente();
        if (vigente != null) {
            headers.setBearerAuth(vigente);
        }
    }

    private synchronized String tokenVigente() {
        if (token == null || !clock.instant().isBefore(tokenVence)) {
            iniciarSesion();
        }
        return token;
    }

    /** Si el login falla, token queda en null: la consulta sale sin credencial y Matrícula responde 403. */
    private void iniciarSesion() {
        token = null;
        if (usuario.isBlank() || password.isBlank()) {
            return;
        }
        long t0 = System.nanoTime();
        try {
            Respuesta r = restClient.post()
                    .uri(RUTA_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", usuario, "password", password, "requestedRole", ROL_TECNICO))
                    .exchange((req, res) -> new Respuesta(
                            res.getStatusCode().value(),
                            StreamUtils.copyToByteArray(res.getBody())));
            log.info("[MATRICULA] POST {} status={} duracionMs={}", RUTA_LOGIN, r.status(), msDesde(t0));
            if (r.status() != 200) {
                return;
            }
            String accessToken = jsonMapper.readTree(r.cuerpo()).path("accessToken").asString("");
            if (accessToken.isBlank()) {
                log.error("[MATRICULA] login técnico sin accessToken en la respuesta");
                return;
            }
            token = accessToken;
            tokenVence = clock.instant().plus(VIGENCIA_TOKEN);
        } catch (RestClientException | JacksonException e) {
            log.warn("[MATRICULA] POST {} falló duracionMs={} causa={}", RUTA_LOGIN, msDesde(t0), e.getMessage());
        }
    }

    /** Descarta el token para que la siguiente llamada vuelva a hacer login. */
    private synchronized void renovarCredencial() {
        log.warn("[MATRICULA] credencial rechazada, se renueva el token y se reintenta una vez");
        token = null;
    }

    /** Solo se inicia otro intento si, en el peor caso, termina dentro del presupuesto. */
    private boolean hayPresupuesto(Instant inicio) {
        Duration transcurrido = Duration.between(inicio, clock.instant());
        return transcurrido.plus(duracionMaximaIntento).compareTo(presupuesto) <= 0;
    }

    private ConsultaMatricula traducir(String cedulaConsultada, byte[] cuerpo) {
        MatriculaRespuesta m;
        try {
            m = jsonMapper.readValue(cuerpo, MatriculaRespuesta.class);
        } catch (JacksonException e) {
            log.error("[MATRICULA] respuesta 200 no es JSON válido: {}", e.getOriginalMessage());
            return ConsultaMatricula.respuestaInvalida();
        }

        List<String> faltantes = camposFaltantes(m);
        if (!faltantes.isEmpty()) {
            log.error("[MATRICULA] respuesta incumple el contrato, faltan: {}", faltantes);
            return ConsultaMatricula.respuestaInvalida();
        }
        if (!cedulaConsultada.equals(m.cedula())) {
            log.error("[MATRICULA] respuesta incumple el contrato: se pidió cedula={} y llegó cedula={}",
                    cedulaConsultada, m.cedula());
            return ConsultaMatricula.respuestaInvalida();
        }

        try {
            return ConsultaMatricula.encontrado(new SituacionAcademica(
                    m.cedula(), m.codigo(), m.nombres(), m.apellidos(), m.email(), m.estado(),
                    m.programa().id(), m.programa().codigo(), m.programa().nombre(),
                    m.admision().cohorteId(), m.admision().cohorte(), m.admision().semestre(),
                    fecha(m.admision().fechaAdmision()), fecha(m.admision().fechaIngreso()),
                    m.matricula().estado(), m.matricula().valor(), fecha(m.matricula().fechaVencimiento()),
                    m.creditos().requeridos(), m.creditos().aprobados()));
        } catch (DateTimeParseException e) {
            log.error("[MATRICULA] respuesta incumple el contrato: fecha no ISO 8601 '{}'", e.getParsedString());
            return ConsultaMatricula.respuestaInvalida();
        }
    }

    /** Campos obligatorios del contrato v0.1. {@code creditos.aprobados} es obligatorio pero admite null (B1). */
    private static List<String> camposFaltantes(MatriculaRespuesta m) {
        List<String> f = new ArrayList<>();
        if (m == null) {
            f.add("cuerpo");
            return f;
        }
        requerido(f, "cedula", m.cedula());
        requerido(f, "codigo", m.codigo());
        requerido(f, "nombres", m.nombres());
        requerido(f, "apellidos", m.apellidos());
        requerido(f, "estado", m.estado());
        if (m.programa() == null) {
            f.add("programa");
        } else {
            requerido(f, "programa.id", m.programa().id());
            requerido(f, "programa.codigo", m.programa().codigo());
            requerido(f, "programa.nombre", m.programa().nombre());
            requerido(f, "programa.creditosRequeridos", m.programa().creditosRequeridos());
        }
        if (m.admision() == null) {
            f.add("admision");
        } else {
            requerido(f, "admision.cohorteId", m.admision().cohorteId());
            requerido(f, "admision.fechaIngreso", m.admision().fechaIngreso());
        }
        if (m.matricula() == null) {
            f.add("matricula");
        } else {
            requerido(f, "matricula.estado", m.matricula().estado());
        }
        if (m.creditos() == null) {
            f.add("creditos");
        } else {
            requerido(f, "creditos.requeridos", m.creditos().requeridos());
        }
        return f;
    }

    private static void requerido(List<String> faltantes, String campo, Object valor) {
        if (valor == null || (valor instanceof String s && s.isBlank())) {
            faltantes.add(campo);
        }
    }

    private static LocalDate fecha(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
    }

    private static long msDesde(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private record Respuesta(int status, byte[] cuerpo) {

        @Override
        public boolean equals(Object o) {
            return o instanceof Respuesta r && status == r.status && Arrays.equals(cuerpo, r.cuerpo);
        }

        @Override
        public int hashCode() {
            return 31 * Integer.hashCode(status) + Arrays.hashCode(cuerpo);
        }

        @Override
        public String toString() {
            return "Respuesta[status=" + status + ", bytes=" + (cuerpo == null ? 0 : cuerpo.length) + "]";
        }
    }
}
