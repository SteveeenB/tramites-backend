package com.ufps.tramites.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.ufps.tramites.integracion.matricula.MatriculaHttpClient;
import com.ufps.tramites.model.Estudiante;
import com.ufps.tramites.model.ProgramaAcademico;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.ProgramaAcademicoRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import com.ufps.tramites.service.SincronizacionMatriculaService.Fuente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * N9 (TP-148), parte con servicio simulado: el cliente HTTP real y la
 * contingencia de N6 contra un servidor HTTP local que imita a Matrícula
 * (contrato v1). A diferencia de MatriculaHttpClientTest, aquí hay sockets
 * y timeouts de verdad, así que el cold start de Render se reproduce con un
 * servidor que tarda más que el timeout de lectura.
 *
 * Los umbrales se escalan (300 ms de lectura, 1,5 s de presupuesto) para que
 * la prueba dure poco; la lógica es la misma que con los 5 s y 15 s de RNF06.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IntegracionMatriculaHttpTest {

    private static final String PREFIJO = "/posgrados-project";
    private static final String RUTA_SITUACION = PREFIJO + "/api/application/case/integracion/v1/estudiantes/";
    private static final String CEDULA = "1098765435";
    private static final long PRESUPUESTO_MS = 1_500;

    private static final String RESPUESTA_OK = """
            {
              "cedula": "1098765435", "codigo": "20261005",
              "nombres": "Laura", "apellidos": "Gomez",
              "email": "laura@example.com", "estado": "ACTIVO",
              "programa": { "id": 12, "codigo": "2203", "nombre": "Maestria en Gerencia de Empresas", "creditosRequeridos": 48 },
              "admision": { "cohorteId": 34, "cohorte": "Cohorte 2025-1", "semestre": "2025-1",
                            "fechaAdmision": "2025-01-20", "fechaIngreso": "2025-02-03" },
              "matricula": { "estado": "COMPLETADO", "valor": 4500000, "fechaVencimiento": "2025-02-01" },
              "creditos": { "requeridos": 48, "aprobados": null }
            }
            """;

    /** Cómo responde el Matrícula simulado a la consulta C1. */
    private enum Modo { OK, NO_ENCONTRADO, TOKEN_VENCIDO_UNA_VEZ, COLD_START, CUERPO_INVALIDO }

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ProgramaAcademicoRepository programaRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private HttpServer server;
    private volatile Modo modo = Modo.OK;
    private volatile boolean saludLenta;
    private final AtomicInteger logins = new AtomicInteger();
    private final AtomicInteger consultas = new AtomicInteger();
    private final List<String> autorizaciones = new CopyOnWriteArrayList<>();

    private SincronizacionMatriculaService servicio;
    private Usuario usuario;
    private ProgramaAcademico programa;
    private Estudiante perfil;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext(PREFIJO + "/auth/login", ex -> {
            logins.incrementAndGet();
            responder(ex, 200, "{\"accessToken\":\"token-" + logins.get() + "\",\"refreshToken\":\"r\"}");
        });
        server.createContext(PREFIJO + "/actuator/health", ex -> {
            if (saludLenta) dormir(2_000);
            responder(ex, 200, "{\"status\":\"UP\"}");
        });
        server.createContext(RUTA_SITUACION, this::situacion);
        server.start();

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + PREFIJO;
        MatriculaHttpClient cliente = new MatriculaHttpClient(url, "svc-tramites", "clave",
                200, 300, 2, PRESUPUESTO_MS);

        servicio = new SincronizacionMatriculaService(cliente, usuarioRepository, estudianteRepository,
                programaRepository, transactionManager, true, 24);

        programa = new ProgramaAcademico();
        programa.setNombre("Maestría en Gerencia de Empresas");
        programa.setTotalCreditos(56);
        usuario = new Usuario();
        usuario.setCedula(CEDULA);
        usuario.setProgramaAcademico(programa);
        perfil = new Estudiante();
        perfil.setUsuario(usuario);
        perfil.setCreditosAprobados(56);

        when(usuarioRepository.findByCedula(CEDULA)).thenReturn(Optional.of(usuario));
        when(estudianteRepository.findByUsuario(usuario)).thenReturn(Optional.of(perfil));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));
        when(programaRepository.findByCodigo(any())).thenReturn(Optional.empty());
        when(programaRepository.findAll()).thenReturn(List.of(programa));
        when(programaRepository.save(any(ProgramaAcademico.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    // ── Camino feliz ─────────────────────────────────────────────────────────

    @Test
    void caminoFeliz_loginTecnicoConsultaYCopiaActualizada() {
        assertThat(servicio.verificarParaIniciarTramite(usuario)).isEqualTo(Fuente.MATRICULA);

        assertThat(logins).hasValue(1);
        assertThat(autorizaciones).containsExactly("Bearer token-1");
        assertThat(perfil.getEstadoMatricula()).isEqualTo("ACTIVO");
        assertThat(perfil.getMatriculaSincronizadoEn()).isNotNull();
        assertThat(perfil.getPrograma()).isSameAs(programa);
        assertThat(programa.getCodigo()).isEqualTo("2203");
        assertThat(programa.getTotalCreditos()).isEqualTo(56); // se conservan los de Trámites
        assertThat(perfil.getCreditosAprobados()).isEqualTo(56);
    }

    // ── Flujos alternos de CU-05 ─────────────────────────────────────────────

    @Test
    void noFiguraEnMatricula_bloqueaAunqueHayaCopiaVigente() {
        modo = Modo.NO_ENCONTRADO;
        perfil.setMatriculaSincronizadoEn(LocalDateTime.now().minusHours(1));

        assertThatThrownBy(() -> servicio.verificarParaIniciarTramite(usuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("admitido y legalizado");
        assertThat(consultas).hasValue(1);
    }

    @Test
    void tokenVencido_renuevaElLoginUnaVezYContinua() {
        modo = Modo.TOKEN_VENCIDO_UNA_VEZ;

        assertThat(servicio.verificarParaIniciarTramite(usuario)).isEqualTo(Fuente.MATRICULA);
        assertThat(logins).hasValue(2);
        assertThat(autorizaciones).containsExactly("Bearer token-1", "Bearer token-2");
    }

    @Test
    void coldStart_conCopiaVigente_usaLaCopiaDentroDelPresupuesto() {
        modo = Modo.COLD_START;
        perfil.setMatriculaSincronizadoEn(LocalDateTime.now().minusHours(23));

        Instant inicio = Instant.now();
        assertThat(servicio.verificarParaIniciarTramite(usuario)).isEqualTo(Fuente.COPIA_VIGENTE);

        assertThat(Duration.between(inicio, Instant.now())).isLessThan(Duration.ofMillis(PRESUPUESTO_MS + 1_000));
        assertThat(consultas).hasValue(3); // primer intento + 2 reintentos
    }

    @Test
    void coldStart_sinCopiaVigente_bloqueaElInicioConMensajeClaro() {
        modo = Modo.COLD_START;
        perfil.setMatriculaSincronizadoEn(LocalDateTime.now().minusHours(25));

        Instant inicio = Instant.now();
        assertThatThrownBy(() -> servicio.verificarParaIniciarTramite(usuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está disponible");
        assertThat(Duration.between(inicio, Instant.now())).isLessThan(Duration.ofMillis(PRESUPUESTO_MS + 1_000));
    }

    @Test
    void respuestaFueraDeContrato_noSeGuardaYSeUsaLaCopia() {
        modo = Modo.CUERPO_INVALIDO;
        LocalDateTime copia = LocalDateTime.now().minusHours(2);
        perfil.setMatriculaSincronizadoEn(copia);

        assertThat(servicio.verificarParaIniciarTramite(usuario)).isEqualTo(Fuente.COPIA_VIGENTE);
        assertThat(perfil.getMatriculaSincronizadoEn()).isEqualTo(copia);
        assertThat(consultas).hasValue(1); // un 200 inválido no se reintenta
    }

    // ── Warm-up al iniciar sesión ────────────────────────────────────────────

    @Test
    void warmUp_conMatriculaArrancando_noConsultaC1() {
        saludLenta = true;

        servicio.precalentar(CEDULA);

        assertThat(consultas).hasValue(0);
    }

    @Test
    void warmUp_conMatriculaDespierta_refrescaLaCopia() {
        servicio.precalentar(CEDULA);

        assertThat(consultas).hasValue(1);
        assertThat(perfil.getMatriculaSincronizadoEn()).isNotNull();
    }

    // ── Matrícula simulado ───────────────────────────────────────────────────

    private void situacion(HttpExchange ex) throws IOException {
        int n = consultas.incrementAndGet();
        autorizaciones.add(ex.getRequestHeaders().getFirst("Authorization"));
        switch (modo) {
            case OK -> responder(ex, 200, RESPUESTA_OK);
            case NO_ENCONTRADO -> responder(ex, 404,
                    "{\"code\":\"ESTUDIANTE_NOT_FOUND\",\"message\":\"No existe\",\"param\":\"" + CEDULA + "\"}");
            case TOKEN_VENCIDO_UNA_VEZ -> responder(ex, n == 1 ? 401 : 200, n == 1 ? "{}" : RESPUESTA_OK);
            case COLD_START -> {
                dormir(2_000);
                responder(ex, 200, RESPUESTA_OK);
            }
            case CUERPO_INVALIDO -> responder(ex, 200, "{\"cedula\":\"" + CEDULA + "\"}");
        }
    }

    private static void responder(HttpExchange ex, int status, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        try {
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            // El cliente ya cortó la conexión por timeout (cold start): es lo esperado.
        }
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
