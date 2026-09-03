package com.ufps.tramites.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ufps.tramites.model.Admin;
import com.ufps.tramites.model.Estudiante;
import com.ufps.tramites.model.ProgramaAcademico;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.DocumentoSolicitudRepository;
import com.ufps.tramites.repository.EstadoEstudianteRepository;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.repository.TipoSolicitudRepository;
import com.ufps.tramites.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests de la lógica de negocio del flujo de Terminación de Materias:
 *   1. Requisito de créditos (100%) al crear la solicitud.
 *   2. Máquina de estados del director (aprobarSolicitudConDirector).
 *   3. Máquina de estados de posgrados (aprobarPosgrados).
 *
 * Solo cubre reglas que están implementadas hoy en SolicitudService; los
 * bugs de autorización (IDOR, doble aprobación, race conditions) requieren
 * pruebas a nivel de controller y están fuera del alcance de este test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudServiceTerminacionTest {

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private EstadoEstudianteRepository estadoEstudianteRepository;
    @Mock private NotificacionSseService notificacionSseService;
    @Mock private NotificacionService notificacionService;
    @Mock private ActaPdfGeneratorService actaPdfGeneratorService;
    @Mock private DocumentoService documentoService;
    @Mock private DocumentoSolicitudRepository documentoSolicitudRepository;
    @Mock private PazYSalvoService pazYSalvoService;
    @Mock private CertificadoPdfService certificadoPdfService;
    @Mock private TipoCertificadoRepository tipoCertificadoRepository;
    @Mock private TipoSolicitudRepository tipoSolicitudRepository;
    @Mock private PlantillaCertificadoService plantillaCertificadoService;
    @Mock private CorreoCertificadoService correoService;
    @Mock private CorreoSolicitudService correoSolicitudService;

    @InjectMocks
    private SolicitudService solicitudService;

    private Usuario estudianteUsuario;
    private Estudiante estudiantePerfil;
    private ProgramaAcademico programa;

    @BeforeEach
    void setUp() {
        programa = new ProgramaAcademico();
        programa.setNombre("Maestría en Ingeniería");
        programa.setTotalCreditos(48);

        estudianteUsuario = new Usuario();
        estudianteUsuario.setCedula("1090123456");
        estudianteUsuario.setProgramaAcademico(programa);

        estudiantePerfil = new Estudiante();
        estudiantePerfil.setCedula("1090123456");
        estudiantePerfil.setPrograma(programa);

        // Stubs comunes — evitan NullPointerException en ramas no exploradas.
        lenient().when(estudianteRepository.findByUsuario(any(Usuario.class)))
                .thenReturn(Optional.of(estudiantePerfil));
        lenient().when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc(anyString(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(tipoSolicitudRepository.findByCodigo(anyString()))
                .thenReturn(Optional.empty());
        lenient().when(usuarioRepository.findByProgramaAcademicoIdAndRol_Nombre(any(), anyString()))
                .thenReturn(List.of());
        lenient().when(solicitudRepository.save(any(Solicitud.class)))
                .thenAnswer(inv -> {
                    Solicitud s = inv.getArgument(0);
                    if (s.getId() == null) {
                        // reflection-set via package field would break; sim id via helper.
                        // en test unitario nos basta con retornar el mismo objeto.
                    }
                    return s;
                });
    }

    // ── 1. Créditos ─────────────────────────────────────────────────────

    @Test
    void crear_conCreditosCompletos_creaSolicitudEnEstadoPendientePago() {
        estudiantePerfil.setCreditosAprobados(48);

        var respuesta = solicitudService.crearSolicitudTerminacion(estudianteUsuario);

        assertThat(respuesta.get("estado")).isEqualTo("PENDIENTE_PAGO");
        assertThat(respuesta.get("tipo")).isEqualTo("TERMINACION_MATERIAS");
    }

    @Test
    void crear_conCreditosInsuficientes_lanzaExcepcion() {
        estudiantePerfil.setCreditosAprobados(30);

        assertThatThrownBy(() -> solicitudService.crearSolicitudTerminacion(estudianteUsuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("30/48");
    }

    @Test
    void crear_conCreditosApenasIguales_esPermitido() {
        estudiantePerfil.setCreditosAprobados(48);

        var respuesta = solicitudService.crearSolicitudTerminacion(estudianteUsuario);

        assertThat(respuesta.get("estado")).isEqualTo("PENDIENTE_PAGO");
    }

    @Test
    void crear_conCreditosSuperiores_esPermitido() {
        estudiantePerfil.setCreditosAprobados(60);

        var respuesta = solicitudService.crearSolicitudTerminacion(estudianteUsuario);

        assertThat(respuesta.get("estado")).isEqualTo("PENDIENTE_PAGO");
    }

    @Test
    void crear_sinPerfilEstudiante_bloqueaPorDefecto() {
        // Si el usuario no tiene ficha en `estudiante`, se toman 0 créditos.
        when(estudianteRepository.findByUsuario(any(Usuario.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudService.crearSolicitudTerminacion(estudianteUsuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requisitos académicos");
    }

    @Test
    void crear_conSolicitudActivaExistente_bloqueaDuplicado() {
        estudiantePerfil.setCreditosAprobados(48);
        Solicitud previa = new Solicitud();
        previa.setEstado("PENDIENTE_PAGO");
        when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc(
                anyString(), anyString())).thenReturn(Optional.of(previa));

        assertThatThrownBy(() -> solicitudService.crearSolicitudTerminacion(estudianteUsuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void crear_conSolicitudRechazadaPrevia_permiteReintento() {
        estudiantePerfil.setCreditosAprobados(48);
        Solicitud previa = new Solicitud();
        previa.setEstado("RECHAZADA");
        when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc(
                anyString(), anyString())).thenReturn(Optional.of(previa));

        var respuesta = solicitudService.crearSolicitudTerminacion(estudianteUsuario);

        assertThat(respuesta.get("estado")).isEqualTo("PENDIENTE_PAGO");
    }

    // ── 2. Máquina de estados: aprobación director ─────────────────────

    @Test
    void aprobarDirector_desdePendientePago_pasaAAprobadaDirector() {
        Solicitud s = solicitudEnEstado("PENDIENTE_PAGO", "TERMINACION_MATERIAS", 10L);
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(s));

        var respuesta = solicitudService.aprobarSolicitudConDirector(10L, "dir");

        assertThat(respuesta.get("estado")).isEqualTo("APROBADA_DIRECTOR");
    }

    @Test
    void aprobarDirector_desdeEnRevision_pasaAAprobadaDirector() {
        Solicitud s = solicitudEnEstado("EN_REVISION", "TERMINACION_MATERIAS", 11L);
        when(solicitudRepository.findById(11L)).thenReturn(Optional.of(s));

        var respuesta = solicitudService.aprobarSolicitudConDirector(11L, "dir");

        assertThat(respuesta.get("estado")).isEqualTo("APROBADA_DIRECTOR");
    }

    @Test
    void aprobarDirector_desdeAprobadaDirector_lanzaExcepcion() {
        // Evita doble aprobación del director sobre la misma solicitud.
        Solicitud s = solicitudEnEstado("APROBADA_DIRECTOR", "TERMINACION_MATERIAS", 12L);
        when(solicitudRepository.findById(12L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> solicitudService.aprobarSolicitudConDirector(12L, "dir"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aprobarDirector_desdeRechazada_lanzaExcepcion() {
        Solicitud s = solicitudEnEstado("RECHAZADA", "TERMINACION_MATERIAS", 13L);
        when(solicitudRepository.findById(13L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> solicitudService.aprobarSolicitudConDirector(13L, "dir"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aprobarDirector_desdeAprobada_lanzaExcepcion() {
        Solicitud s = solicitudEnEstado("APROBADA", "TERMINACION_MATERIAS", 14L);
        when(solicitudRepository.findById(14L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> solicitudService.aprobarSolicitudConDirector(14L, "dir"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── 3. Máquina de estados: aprobación posgrados ────────────────────

    @Test
    void aprobarPosgrados_desdeAprobadaDirector_pasaAAprobadaYGeneraActa() throws Exception {
        Solicitud s = solicitudEnEstado("APROBADA_DIRECTOR", "TERMINACION_MATERIAS", 20L);
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(s));
        when(tipoCertificadoRepository.findByCodigo(anyString())).thenReturn(Optional.empty());
        when(certificadoPdfService.generar(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), any()))
                .thenReturn("pdf".getBytes());

        Admin admin = new Admin();
        solicitudService.aprobarPosgrados(20L, admin);

        assertThat(s.getEstado()).isEqualTo("APROBADA");
        assertThat(s.isActaGenerada()).isTrue();
    }

    @Test
    void aprobarPosgrados_desdeEstadoIncorrecto_lanzaExcepcion() {
        Solicitud s = solicitudEnEstado("PENDIENTE_PAGO", "TERMINACION_MATERIAS", 21L);
        when(solicitudRepository.findById(21L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> solicitudService.aprobarPosgrados(21L, new Admin()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aprobada por director");
    }

    @Test
    void aprobarPosgrados_conTipoIncorrecto_lanzaExcepcion() {
        Solicitud s = solicitudEnEstado("APROBADA_DIRECTOR", "GRADO", 22L);
        when(solicitudRepository.findById(22L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> solicitudService.aprobarPosgrados(22L, new Admin()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("solo aplica para solicitudes de terminación");
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private Solicitud solicitudEnEstado(String estado, String tipo, Long id) {
        Solicitud s = new Solicitud();
        s.setCedula("1090123456");
        s.setEstudiante(estudiantePerfil);
        s.setTipo(tipo);
        s.setEstado(estado);
        try {
            java.lang.reflect.Field f = Solicitud.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return s;
    }
}
