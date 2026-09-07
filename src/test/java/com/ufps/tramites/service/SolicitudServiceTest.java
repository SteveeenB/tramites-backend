package com.ufps.tramites.service;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ufps.tramites.model.Estudiante;
import com.ufps.tramites.model.ProgramaAcademico;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.DocumentoSolicitudRepository;
import com.ufps.tramites.repository.EstadoEstudianteRepository;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.SolicitudRepository;
import com.ufps.tramites.repository.TipoCertificadoRepository;
import com.ufps.tramites.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

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
    @Mock private PlantillaCertificadoService plantillaCertificadoService;
    @Mock private CorreoCertificadoService correoService;

    @InjectMocks
    private SolicitudService solicitudService;

    private Usuario estudianteUsuario;
    private ProgramaAcademico programa;

    @BeforeEach
    void setUp() {
        programa = new ProgramaAcademico();
        programa.setTotalCreditos(40);

        estudianteUsuario = new Usuario();
        estudianteUsuario.setCedula("123456789");
        estudianteUsuario.setProgramaAcademico(programa);
    }

    // ── créditos insuficientes bloquean terminación ─────────────────────────

    @Test
    void creditosInsuficientes_bloqueanSolicitudDeTerminacion() {
        Estudiante perfil = new Estudiante();
        perfil.setCreditosAprobados(20); // requiere 40

        when(estudianteRepository.findByUsuario(estudianteUsuario)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> solicitudService.crearSolicitudTerminacion(estudianteUsuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No cumple los requisitos académicos")
                .hasMessageContaining("20/40");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void creditosSuficientes_permiteCrearSolicitudDeTerminacion() {
        Estudiante perfil = new Estudiante();
        perfil.setCreditosAprobados(40);

        when(estudianteRepository.findByUsuario(estudianteUsuario)).thenReturn(Optional.of(perfil));
        when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc("123456789", "TERMINACION_MATERIAS"))
                .thenReturn(Optional.empty());
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = solicitudService.crearSolicitudTerminacion(estudianteUsuario);

        assertThat(respuesta.get("estado")).isEqualTo("PENDIENTE_PAGO");
        // se guarda dos veces: al crear y al asignar el radicado generado
        verify(solicitudRepository, times(2)).save(any(Solicitud.class));
    }

    // ── prerequisito de terminación aprobada para grado ─────────────────────

    @Test
    void sinTerminacionPrevia_bloqueaSolicitudDeGrado() {
        when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc("123456789", "TERMINACION_MATERIAS"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudService.crearSolicitudGrado(
                estudianteUsuario, "Título", "Resumen", "INVESTIGACION", null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Terminación de Materias aprobada");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void terminacionNoAprobada_bloqueaSolicitudDeGrado() {
        Solicitud terminacionEnRevision = new Solicitud();
        terminacionEnRevision.setCedula("123456789");
        terminacionEnRevision.setTipo("TERMINACION_MATERIAS");
        terminacionEnRevision.setEstado("EN_REVISION");

        when(solicitudRepository.findFirstByCedulaAndTipoOrderByIdDesc("123456789", "TERMINACION_MATERIAS"))
                .thenReturn(Optional.of(terminacionEnRevision));

        assertThatThrownBy(() -> solicitudService.crearSolicitudGrado(
                estudianteUsuario, "Título", "Resumen", "INVESTIGACION", null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Terminación de Materias aprobada");

        verify(solicitudRepository, never()).save(any());
    }

    // ── flujo aprobar / rechazar ─────────────────────────────────────────────

    @Test
    void aprobarSolicitudTerminacion_pasaAEstadoAprobadaDirector() {
        Solicitud solicitud = new Solicitud();
        solicitud.setCedula("123456789");
        solicitud.setTipo("TERMINACION_MATERIAS");
        solicitud.setEstado("EN_REVISION");
        solicitud.setFechaSolicitud(LocalDate.now());

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = solicitudService.aprobarSolicitudConDirector(1L, "director-cedula");

        assertThat(solicitud.getEstado()).isEqualTo("APROBADA_DIRECTOR");
        assertThat(respuesta.get("estado")).isEqualTo("APROBADA_DIRECTOR");
        verify(notificacionSseService).notificarCambioEstado(solicitud, "EN_REVISION");
    }

    @Test
    void aprobarSolicitudEnEstadoInvalido_lanzaExcepcion() {
        Solicitud solicitud = new Solicitud();
        solicitud.setTipo("TERMINACION_MATERIAS");
        solicitud.setEstado("RECHAZADA");

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudService.aprobarSolicitudConDirector(1L, "director-cedula"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("estado pendiente");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void rechazarSolicitud_requiereMotivoYCambiaEstado() {
        Solicitud solicitud = new Solicitud();
        solicitud.setCedula("123456789");
        solicitud.setTipo("TERMINACION_MATERIAS");
        solicitud.setEstado("EN_REVISION");

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = solicitudService.rechazarSolicitud(1L, "Documentación incompleta");

        assertThat(solicitud.getEstado()).isEqualTo("RECHAZADA");
        assertThat(solicitud.getObservaciones()).isEqualTo("Documentación incompleta");
        assertThat(respuesta.get("estado")).isEqualTo("RECHAZADA");
    }

    @Test
    void rechazarSolicitud_sinMotivo_lanzaExcepcion() {
        Solicitud solicitud = new Solicitud();
        solicitud.setTipo("TERMINACION_MATERIAS");
        solicitud.setEstado("EN_REVISION");

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudService.rechazarSolicitud(1L, "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("motivo de rechazo");

        verify(solicitudRepository, never()).save(any());
    }
}
