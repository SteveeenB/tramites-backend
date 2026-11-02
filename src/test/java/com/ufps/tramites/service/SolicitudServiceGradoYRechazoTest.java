package com.ufps.tramites.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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

/**
 * FIX TP-195 (Kevin Arias, 02/11/2026): se integran desde la rama "Test" los
 * casos de SolicitudServiceTest que no cubren los tests actuales
 * (SolicitudServiceTerminacionTest ya prueba créditos y aprobación del
 * Director). Este test cubre:
 *   1. Prerrequisitos de la solicitud de grado (terminación APROBADA y sin
 *      otra solicitud de grado activa).
 *   2. Rechazo de una solicitud: exige motivo y deja trazabilidad.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudServiceGradoYRechazoTest {

    private static final String CEDULA = "1090123456";

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
    @Mock private SincronizacionMatriculaService sincronizacionMatriculaService;
    @Mock private com.ufps.tramites.repository.AdminRepository adminRepository;
    @Mock private com.ufps.tramites.repository.CambioEstadoSolicitudRepository cambioEstadoRepository;

    @InjectMocks
    private SolicitudService solicitudService;

    private Usuario estudianteUsuario;

    @BeforeEach
    void setUp() {
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setTotalCreditos(48);

        estudianteUsuario = new Usuario();
        estudianteUsuario.setCedula(CEDULA);
        estudianteUsuario.setProgramaAcademico(programa);

        lenient().when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc(anyString(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(solicitudRepository.save(any(Solicitud.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── 1. Prerrequisitos de la solicitud de grado ──────────────────────

    @Test
    void grado_sinTerminacionPrevia_seBloquea() {
        assertThatThrownBy(() -> solicitudService.crearSolicitudGrado(
                estudianteUsuario, "Título", "Resumen", "INVESTIGACION", null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Terminación de Materias aprobada");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void grado_conTerminacionEnRevision_seBloquea() {
        when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc(CEDULA, "TERMINACION_MATERIAS"))
                .thenReturn(Optional.of(solicitud("TERMINACION_MATERIAS", "EN_REVISION")));

        assertThatThrownBy(() -> solicitudService.crearSolicitudGrado(
                estudianteUsuario, "Título", "Resumen", "INVESTIGACION", null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Terminación de Materias aprobada");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void grado_conOtraSolicitudDeGradoActiva_seBloquea() {
        when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc(CEDULA, "TERMINACION_MATERIAS"))
                .thenReturn(Optional.of(solicitud("TERMINACION_MATERIAS", "APROBADA")));
        when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc(CEDULA, "GRADO"))
                .thenReturn(Optional.of(solicitud("GRADO", "EN_REVISION")));

        assertThatThrownBy(() -> solicitudService.crearSolicitudGrado(
                estudianteUsuario, "Título", "Resumen", "INVESTIGACION", null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una solicitud de grado");

        verify(solicitudRepository, never()).save(any());
    }

    // ── 2. Rechazo ──────────────────────────────────────────────────────

    @Test
    void rechazar_conMotivo_cambiaEstadoYRegistraTrazabilidad() {
        Solicitud s = solicitud("TERMINACION_MATERIAS", "EN_REVISION");
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(s));

        var respuesta = solicitudService.rechazarSolicitud(1L, "Documentación incompleta", "88888888");

        assertThat(respuesta.get("estado")).isEqualTo("RECHAZADA");
        assertThat(s.getEstado()).isEqualTo("RECHAZADA");
        assertThat(s.getObservaciones()).isEqualTo("Documentación incompleta");
        assertThat(s.getDecision()).isEqualTo("RECHAZADA");
        assertThat(s.getObservacionesDirector()).isEqualTo("Documentación incompleta");
        assertThat(s.getCedulaDirector()).isEqualTo("88888888");
        assertThat(s.getFechaCierre()).isNotNull();
        verify(solicitudRepository).save(s);
        verify(notificacionSseService).notificarCambioEstado(s, "EN_REVISION");
    }

    @Test
    void rechazar_sinMotivo_lanzaExcepcionYNoGuarda() {
        Solicitud s = solicitud("TERMINACION_MATERIAS", "EN_REVISION");
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> solicitudService.rechazarSolicitud(1L, "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("motivo de rechazo");

        assertThat(s.getEstado()).isEqualTo("EN_REVISION");
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void rechazar_solicitudYaRechazada_lanzaExcepcion() {
        Solicitud s = solicitud("TERMINACION_MATERIAS", "RECHAZADA");
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> solicitudService.rechazarSolicitud(1L, "Otro motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("estado pendiente");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void rechazar_solicitudInexistente_lanzaIllegalArgument() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudService.rechazarSolicitud(99L, "Motivo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private Solicitud solicitud(String tipo, String estado) {
        Solicitud s = new Solicitud();
        s.setCedula(CEDULA);
        s.setTipo(tipo);
        s.setEstado(estado);
        return s;
    }
}
