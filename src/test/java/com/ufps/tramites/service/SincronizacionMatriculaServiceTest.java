package com.ufps.tramites.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import com.ufps.tramites.integracion.matricula.ConsultaMatricula;
import com.ufps.tramites.integracion.matricula.ConsultaMatricula.Resultado;
import com.ufps.tramites.integracion.matricula.MatriculaClient;
import com.ufps.tramites.integracion.matricula.MatriculaMockClient;
import com.ufps.tramites.integracion.matricula.SituacionAcademica;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Copia del estudiante y contingencia ante caídas de Matrícula (SC-03, N5/N6).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SincronizacionMatriculaServiceTest {

    private static final String CEDULA = "1098765435";
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 10, 0);
    private static final Clock RELOJ = Clock.fixed(
            AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @Mock private MatriculaClient matriculaClient;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ProgramaAcademicoRepository programaRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private Usuario usuario;
    private ProgramaAcademico programa;

    @BeforeEach
    void setUp() {
        programa = new ProgramaAcademico();
        programa.setNombre("Maestría en Gerencia de Empresas");
        programa.setTotalCreditos(56);

        usuario = new Usuario();
        usuario.setCedula(CEDULA);
        usuario.setProgramaAcademico(programa);

        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));
        when(programaRepository.save(any(ProgramaAcademico.class))).thenAnswer(i -> i.getArgument(0));
        when(programaRepository.findByCodigo(any())).thenReturn(Optional.empty());
        when(programaRepository.findAll()).thenReturn(List.of(programa));
    }

    private SincronizacionMatriculaService servicio(boolean habilitada) {
        return servicio(matriculaClient, habilitada);
    }

    private SincronizacionMatriculaService servicio(MatriculaClient cliente, boolean habilitada) {
        return new SincronizacionMatriculaService(cliente, usuarioRepository, estudianteRepository,
                programaRepository, transactionManager, RELOJ, habilitada, Duration.ofHours(24));
    }

    private static SituacionAcademica situacion(String cedula, int creditosRequeridos) {
        return new SituacionAcademica(cedula, "20261005", "Laura Marcela", "Gómez Ruiz",
                "laura@ufps.edu.co", "ACTIVO", 12L, "2203", "Maestria en Gerencia de Empresas",
                34L, "Cohorte 2025-1", "2025-1", LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 3),
                "COMPLETADO", new BigDecimal("4500000"), LocalDate.of(2025, 2, 1),
                creditosRequeridos, null);
    }

    private static ConsultaMatricula respuesta(Resultado resultado, SituacionAcademica s) {
        return new ConsultaMatricula(resultado, s, "mensaje " + resultado);
    }

    private Estudiante perfilSincronizadoHace(Duration hace) {
        Estudiante perfil = new Estudiante();
        perfil.setUsuario(usuario);
        perfil.setCedula(CEDULA);
        perfil.setCreditosAprobados(56);
        perfil.setMatriculaSincronizadoEn(hace == null ? null : AHORA.minus(hace));
        when(estudianteRepository.findByUsuario(usuario)).thenReturn(Optional.of(perfil));
        return perfil;
    }

    // ── N5: copia del estudiante ─────────────────────────────────────────────

    @Test
    void deshabilitada_noConsultaNiBloquea() {
        assertThat(servicio(false).verificarParaIniciarTramite(usuario)).isEqualTo(Fuente.SIN_VALIDAR);

        verify(matriculaClient, never()).consultarSituacion(any());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void encontrado_actualizaLaCopiaSinTocarLosCreditosAprobados() {
        Estudiante perfil = perfilSincronizadoHace(null);
        perfil.setNombre2("Marcela");
        when(matriculaClient.consultarSituacion(CEDULA))
                .thenReturn(respuesta(Resultado.ENCONTRADO, situacion(CEDULA, 56)));

        assertThat(servicio(true).verificarParaIniciarTramite(usuario)).isEqualTo(Fuente.MATRICULA);

        assertThat(perfil.getCodigo()).isEqualTo("20261005");
        assertThat(perfil.getNombre()).isEqualTo("Laura Marcela");
        assertThat(perfil.getNombre2()).isNull();
        assertThat(perfil.getApellido()).isEqualTo("Gómez Ruiz");
        assertThat(perfil.getEmail()).isEqualTo("laura@ufps.edu.co");
        assertThat(perfil.getCohorteId()).isEqualTo(34L);
        assertThat(perfil.getFechaIngreso()).isEqualTo(LocalDate.of(2025, 2, 3));
        assertThat(perfil.getEstadoMatricula()).isEqualTo("ACTIVO");
        assertThat(perfil.getPrograma()).isSameAs(programa);
        assertThat(perfil.getMatriculaSincronizadoEn()).isEqualTo(AHORA);
        assertThat(perfil.getCreditosAprobados()).isEqualTo(56);
        verify(estudianteRepository).save(perfil);
    }

    @Test
    void encontrado_programaSinCodigo_loAdoptaPorNombreYConservaLosCreditosDeTramites() {
        perfilSincronizadoHace(null);
        when(matriculaClient.consultarSituacion(CEDULA))
                .thenReturn(respuesta(Resultado.ENCONTRADO, situacion(CEDULA, 60)));

        servicio(true).verificarParaIniciarTramite(usuario);

        assertThat(programa.getCodigo()).isEqualTo("2203");
        assertThat(programa.getTotalCreditos()).isEqualTo(56);
        verify(programaRepository).save(programa);
    }

    @Test
    void mismoNombre_ignoraTildesMayusculasYEspacios() {
        assertThat(SincronizacionMatriculaService.mismoNombre(
                "Maestría en  Gerencia de Empresas", "MAESTRIA EN GERENCIA DE EMPRESAS ")).isTrue();
        assertThat(SincronizacionMatriculaService.mismoNombre(
                "Maestría en Gerencia de Empresas", "Maestria en Negocios Internacionales")).isFalse();
    }

    @Test
    void encontrado_programaConOtroCodigo_noLoReutilizaPorNombre() {
        Estudiante perfil = perfilSincronizadoHace(null);
        programa.setCodigo("OTRO");
        when(matriculaClient.consultarSituacion(CEDULA))
                .thenReturn(respuesta(Resultado.ENCONTRADO, situacion(CEDULA, 56)));

        assertThat(servicio(true).verificarParaIniciarTramite(usuario)).isEqualTo(Fuente.MATRICULA);

        assertThat(programa.getCodigo()).isEqualTo("OTRO");
        assertThat(perfil.getPrograma()).isNull();
    }

    @Test
    void encontrado_sinPerfil_creaUnoVinculadoAlUsuario() {
        when(estudianteRepository.findByUsuario(usuario)).thenReturn(Optional.empty());
        when(matriculaClient.consultarSituacion(CEDULA))
                .thenReturn(respuesta(Resultado.ENCONTRADO, situacion(CEDULA, 56)));

        servicio(true).verificarParaIniciarTramite(usuario);

        ArgumentCaptor<Estudiante> guardado = ArgumentCaptor.forClass(Estudiante.class);
        verify(estudianteRepository).save(guardado.capture());
        assertThat(guardado.getValue().getUsuario()).isSameAs(usuario);
        assertThat(guardado.getValue().getCedula()).isEqualTo(CEDULA);
        assertThat(guardado.getValue().getMatriculaSincronizadoEn()).isEqualTo(AHORA);
    }

    // ── N6: contingencia ─────────────────────────────────────────────────────

    @Test
    void noDisponible_conCopiaDeMenosDe24h_usaLaCopia() {
        perfilSincronizadoHace(Duration.ofHours(23));
        when(matriculaClient.consultarSituacion(CEDULA)).thenReturn(respuesta(Resultado.NO_DISPONIBLE, null));

        assertThat(servicio(true).verificarParaIniciarTramite(usuario)).isEqualTo(Fuente.COPIA_VIGENTE);
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void respuestaInvalida_conCopiaVigente_usaLaCopia() {
        perfilSincronizadoHace(Duration.ofMinutes(5));
        when(matriculaClient.consultarSituacion(CEDULA)).thenReturn(respuesta(Resultado.RESPUESTA_INVALIDA, null));

        assertThat(servicio(true).verificarParaIniciarTramite(usuario)).isEqualTo(Fuente.COPIA_VIGENTE);
    }

    @Test
    void noDisponible_conCopiaVencida_bloquea() {
        perfilSincronizadoHace(Duration.ofHours(25));
        when(matriculaClient.consultarSituacion(CEDULA)).thenReturn(respuesta(Resultado.NO_DISPONIBLE, null));

        assertThatThrownBy(() -> servicio(true).verificarParaIniciarTramite(usuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mensaje NO_DISPONIBLE");
    }

    @Test
    void noDisponible_sinCopia_bloquea() {
        perfilSincronizadoHace(null);
        when(matriculaClient.consultarSituacion(CEDULA)).thenReturn(respuesta(Resultado.NO_DISPONIBLE, null));

        assertThatThrownBy(() -> servicio(true).verificarParaIniciarTramite(usuario))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void noEncontrado_bloqueaAunqueHayaCopiaVigente() {
        perfilSincronizadoHace(Duration.ofHours(1));
        when(matriculaClient.consultarSituacion(CEDULA)).thenReturn(respuesta(Resultado.NO_ENCONTRADO, null));

        assertThatThrownBy(() -> servicio(true).verificarParaIniciarTramite(usuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mensaje NO_ENCONTRADO");
    }

    @Test
    void respuestaDeOtraCedula_noSeGuardaYSinCopiaBloquea() {
        perfilSincronizadoHace(null);
        when(matriculaClient.consultarSituacion(CEDULA))
                .thenReturn(respuesta(Resultado.ENCONTRADO, situacion("1111111111", 56)));

        assertThatThrownBy(() -> servicio(true).verificarParaIniciarTramite(usuario))
                .isInstanceOf(IllegalStateException.class);
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void mockCaido_respondeEnMenosDe15sYBloqueaSinCopia() {
        String cedulaCaida = "1090000503";
        usuario.setCedula(cedulaCaida);
        when(estudianteRepository.findByUsuario(usuario)).thenReturn(Optional.empty());

        Instant inicio = Instant.now();
        assertThatThrownBy(() -> servicio(new MatriculaMockClient(), true).verificarParaIniciarTramite(usuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está disponible");
        assertThat(Duration.between(inicio, Instant.now())).isLessThan(Duration.ofSeconds(15));
    }

    // ── N6: warm-up al iniciar sesión ────────────────────────────────────────

    @Test
    void precalentar_siMatriculaNoResponde_noConsultaC1() {
        when(matriculaClient.estaDisponible()).thenReturn(false);

        servicio(true).precalentar(CEDULA);

        verify(matriculaClient, never()).consultarSituacion(any());
    }

    @Test
    void precalentar_siMatriculaResponde_refrescaLaCopia() {
        Estudiante perfil = perfilSincronizadoHace(Duration.ofHours(30));
        when(matriculaClient.estaDisponible()).thenReturn(true);
        when(usuarioRepository.findByCedula(CEDULA)).thenReturn(Optional.of(usuario));
        when(matriculaClient.consultarSituacion(CEDULA))
                .thenReturn(respuesta(Resultado.ENCONTRADO, situacion(CEDULA, 56)));

        servicio(true).precalentar(CEDULA);

        assertThat(perfil.getMatriculaSincronizadoEn()).isEqualTo(AHORA);
    }

    @Test
    void precalentar_noPropagaErrores() {
        when(matriculaClient.estaDisponible()).thenThrow(new IllegalStateException("boom"));

        servicio(true).precalentar(CEDULA);

        verify(matriculaClient, never()).consultarSituacion(any());
    }

    @Test
    void precalentar_deshabilitada_noLlamaAMatricula() {
        servicio(false).precalentar(CEDULA);

        verify(matriculaClient, never()).estaDisponible();
    }
}
