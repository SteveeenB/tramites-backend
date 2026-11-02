package com.ufps.tramites.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.ufps.tramites.integracion.matricula.ConsultaMatricula;
import com.ufps.tramites.integracion.matricula.MatriculaClient;
import com.ufps.tramites.integracion.matricula.SituacionAcademica;
import com.ufps.tramites.model.Estudiante;
import com.ufps.tramites.model.ProgramaAcademico;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.ProgramaAcademicoRepository;
import com.ufps.tramites.repository.UsuarioRepository;

/**
 * Snapshot del estudiante desde el Módulo de Matrícula y contingencia ante
 * caídas (SC-03, N5 y N6; plan TP-144 §6.3 y §6.4, decisión 7.1.1 del PO).
 *
 * <ul>
 *   <li>Cada consulta C1 exitosa actualiza el perfil {@link Estudiante} y
 *       marca {@code matriculaSincronizadoEn}. {@link Usuario} no se toca para
 *       no romper el login por código.</li>
 *   <li>Si Matrícula no responde, se usa la copia mientras tenga menos de
 *       {@code matricula.snapshot-vigencia-horas}. Sin copia vigente se
 *       bloquea solo el inicio de trámites nuevos; los ya iniciados siguen.</li>
 *   <li>Al iniciar sesión un estudiante se despierta el servicio de Matrícula
 *       en segundo plano (Render free tarda en arrancar) y, si responde, se
 *       refresca la copia.</li>
 * </ul>
 *
 * Con {@code matricula.sincronizar=false} no se consulta ni se bloquea nada:
 * es el comportamiento previo, para no escribir datos simulados del modo mock
 * en una BD real.
 */
@Service
public class SincronizacionMatriculaService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacionMatriculaService.class);

    /** Cómo se validó al estudiante antes de iniciar un trámite. */
    public enum Fuente {
        /** {@code matricula.sincronizar=false}: no se consultó. */
        SIN_VALIDAR,
        /** Matrícula respondió y la copia quedó actualizada. */
        MATRICULA,
        /** Matrícula no respondió y se usó la copia vigente. */
        COPIA_VIGENTE
    }

    private final MatriculaClient matriculaClient;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final TransactionTemplate transaccionNueva;
    private final Clock clock;
    private final boolean habilitada;
    private final Duration vigencia;

    @Autowired
    public SincronizacionMatriculaService(
            // S9352 es falso positivo: solo existe un adaptador a la vez (@ConditionalOnProperty matricula.modo)
            MatriculaClient matriculaClient, // NOSONAR
            UsuarioRepository usuarioRepository,
            EstudianteRepository estudianteRepository,
            ProgramaAcademicoRepository programaRepository,
            PlatformTransactionManager transactionManager,
            @Value("${matricula.sincronizar:false}") boolean habilitada,
            @Value("${matricula.snapshot-vigencia-horas:24}") long vigenciaHoras) {
        this(matriculaClient, usuarioRepository, estudianteRepository, programaRepository,
                transactionManager, Clock.systemDefaultZone(), habilitada, Duration.ofHours(vigenciaHoras));
    }

    SincronizacionMatriculaService(
            MatriculaClient matriculaClient,
            UsuarioRepository usuarioRepository,
            EstudianteRepository estudianteRepository,
            ProgramaAcademicoRepository programaRepository,
            PlatformTransactionManager transactionManager,
            Clock clock,
            boolean habilitada,
            Duration vigencia) {
        this.matriculaClient = matriculaClient;
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.programaRepository = programaRepository;
        // La copia se guarda aunque el trámite que la pidió falle después
        // (p. ej. CertificadoService es @Transactional y revierte todo).
        this.transaccionNueva = new TransactionTemplate(transactionManager);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
        this.habilitada = habilitada;
        this.vigencia = vigencia;
    }

    /**
     * Valida contra Matrícula antes de iniciar un trámite nuevo (terminación,
     * grado o certificado).
     *
     * @throws IllegalStateException con el mensaje para el estudiante si no
     *         figura en Matrícula, o si Matrícula no responde y no hay copia vigente
     */
    public Fuente verificarParaIniciarTramite(Usuario usuario) {
        if (!habilitada) {
            return Fuente.SIN_VALIDAR;
        }
        ConsultaMatricula consulta = consultar(usuario);
        switch (consulta.resultado()) {
            case ENCONTRADO:
                guardarCopia(usuario, consulta.situacion());
                return Fuente.MATRICULA;
            case NO_DISPONIBLE:
            case RESPUESTA_INVALIDA:
                Optional<Estudiante> perfil = estudianteRepository.findByUsuario(usuario);
                if (perfil.isPresent() && copiaVigente(perfil.get())) {
                    log.warn("[MATRICULA] {} para cedula={}; se usa la copia del {}",
                            consulta.resultado(), usuario.getCedula(), perfil.get().getMatriculaSincronizadoEn());
                    return Fuente.COPIA_VIGENTE;
                }
                log.warn("[MATRICULA] {} para cedula={} y sin copia vigente; se bloquea el trámite",
                        consulta.resultado(), usuario.getCedula());
                throw new IllegalStateException(consulta.mensaje());
            default:
                // NO_ENCONTRADO y CEDULA_INVALIDA: Matrícula respondió, la copia no aplica.
                throw new IllegalStateException(consulta.mensaje());
        }
    }

    /**
     * Despierta a Matrícula (C2) al iniciar sesión un estudiante y, si
     * responde, refresca la copia (C1). No bloquea el login ni lanza errores.
     */
    @Async
    public void precalentar(String cedula) {
        if (!habilitada || cedula == null) {
            return;
        }
        try {
            if (!matriculaClient.estaDisponible()) {
                log.info("[MATRICULA] warm-up: el servicio aún no responde (cedula={})", cedula);
                return;
            }
            usuarioRepository.findByCedula(cedula).ifPresent(usuario -> {
                ConsultaMatricula consulta = consultar(usuario);
                if (consulta.encontrado()) {
                    guardarCopia(usuario, consulta.situacion());
                }
            });
        } catch (RuntimeException e) {
            log.warn("[MATRICULA] warm-up falló para cedula={}: {}", cedula, e.toString());
        }
    }

    boolean copiaVigente(Estudiante perfil) {
        LocalDateTime sincronizado = perfil.getMatriculaSincronizadoEn();
        return sincronizado != null
                && sincronizado.isAfter(LocalDateTime.now(clock).minus(vigencia));
    }

    /** C1 y comprobación de que la respuesta es de la cédula del usuario autenticado. */
    private ConsultaMatricula consultar(Usuario usuario) {
        ConsultaMatricula consulta = matriculaClient.consultarSituacion(usuario.getCedula());
        if (consulta.encontrado() && !Objects.equals(usuario.getCedula(), consulta.situacion().cedula())) {
            log.error("[MATRICULA] respuesta de otra cédula: pedida={} recibida={}",
                    usuario.getCedula(), consulta.situacion().cedula());
            return new ConsultaMatricula(ConsultaMatricula.Resultado.RESPUESTA_INVALIDA, null,
                    "No fue posible validar la información académica. Intenta más tarde.");
        }
        return consulta;
    }

    private void guardarCopia(Usuario usuario, SituacionAcademica s) {
        transaccionNueva.executeWithoutResult(status -> {
            Estudiante perfil = estudianteRepository.findByUsuario(usuario).orElseGet(() -> nuevoPerfil(usuario));
            perfil.setCedula(s.cedula());
            perfil.setCodigo(s.codigo());
            // Matrícula manda nombres y apellidos completos en un solo campo.
            perfil.setNombre(s.nombres());
            perfil.setNombre2(null);
            perfil.setApellido(s.apellidos());
            perfil.setApellido2(null);
            perfil.setEmail(s.email());
            perfil.setCohorteId(s.cohorteId());
            perfil.setFechaIngreso(s.fechaIngreso());
            perfil.setEstadoMatricula(s.estado());
            resolverPrograma(s).ifPresent(programa -> {
                perfil.setPrograma(programa);
                if (usuario.getProgramaAcademico() != null
                        && !Objects.equals(usuario.getProgramaAcademico().getId(), programa.getId())) {
                    log.warn("[MATRICULA] cedula={} figura en {} según Matrícula y en {} según Trámites",
                            s.cedula(), programa.getNombre(), usuario.getProgramaAcademico().getNombre());
                }
            });
            // creditosAprobados no se toma de Matrícula: son precargados (7.1.4).
            perfil.setMatriculaSincronizadoEn(LocalDateTime.now(clock));
            estudianteRepository.save(perfil);
        });
    }

    /**
     * Busca el programa por su código en Matrícula. Si aún no tiene código,
     * lo adopta del programa con el mismo nombre (los del seed no lo traen).
     * Los créditos requeridos los define Matrícula: si difieren, gana ella.
     */
    private Optional<ProgramaAcademico> resolverPrograma(SituacionAcademica s) {
        if (s.programaCodigo() == null || s.programaCodigo().isBlank()) {
            return Optional.empty();
        }
        Optional<ProgramaAcademico> encontrado = programaRepository.findByCodigo(s.programaCodigo())
                .or(() -> programaRepository.findFirstByNombreIgnoreCase(s.programaNombre())
                        .filter(p -> p.getCodigo() == null)
                        .map(p -> {
                            log.info("[MATRICULA] programa '{}' queda con codigo={}", p.getNombre(), s.programaCodigo());
                            p.setCodigo(s.programaCodigo());
                            return p;
                        }));
        if (encontrado.isEmpty()) {
            log.warn("[MATRICULA] programa codigo={} nombre='{}' no existe en Trámites",
                    s.programaCodigo(), s.programaNombre());
            return Optional.empty();
        }
        ProgramaAcademico programa = encontrado.get();
        if (s.creditosRequeridos() > 0 && !Objects.equals(programa.getTotalCreditos(), s.creditosRequeridos())) {
            log.warn("[MATRICULA] programa '{}': créditos requeridos {} → {} (los define Matrícula)",
                    programa.getNombre(), programa.getTotalCreditos(), s.creditosRequeridos());
            programa.setTotalCreditos(s.creditosRequeridos());
        }
        return Optional.of(programaRepository.save(programa));
    }

    private static Estudiante nuevoPerfil(Usuario usuario) {
        Estudiante perfil = new Estudiante();
        perfil.setUsuario(usuario);
        perfil.setEsPosgrado(true);
        perfil.setMigrado(false);
        return perfil;
    }
}
