-- ============================================================================
-- V3__catalogos_base.sql — Catálogos obligatorios del Módulo de Trámites
-- (TP-153 · acuerdo TP-208, sección 7.1 punto 6)
--
-- Sin estos catálogos, una base nueva arranca pero no funciona: no hay roles para
-- iniciar sesión, programas para los estudiantes ni tipos de trámite. Hasta
-- ahora solo los cargaba data.sql, que corre únicamente en el perfil h2.
--
-- Idempotente: INSERT IGNORE sobre llaves únicas (id en roles, nombre o código
-- en las demás). En una base que ya tiene estos datos no cambia nada.
--
-- Valores tomados de data.sql (16/11/2026), sin datos demo: aquí no hay
-- usuarios, estudiantes, solicitudes ni fechas de grado. Las cuentas iniciales
-- se crean aparte y no van en el repositorio.
--
-- Solo comillas simples y nombres sin comillas: el servidor compartido corre
-- con sql_mode ANSI (ANSI_QUOTES). Nunca editar este archivo una vez aplicado.
-- ============================================================================

-- ── Roles (ids fijos: el código los usa por nombre y por id) ─────────────────
INSERT IGNORE INTO tp_roles (id, nombre) VALUES
    (1, 'ESTUDIANTE'),
    (2, 'DIRECTOR'),
    (3, 'ADMIN'),
    (4, 'POSGRADOS'),
    (5, 'DEPENDENCIA');

-- ── Estados del estudiante ───────────────────────────────────────────────────
INSERT IGNORE INTO tp_estados_estudiantes (id, nombre) VALUES
    (1, 'ACTIVO'),
    (2, 'PAGO_GRADO_PENDIENTE'),
    (3, 'GRADUADO');

-- ── Programas de posgrado, con su código en el Módulo de Matrícula (SC-03) ───
INSERT IGNORE INTO tp_programa_academico (codigo, nombre, tipo, total_creditos) VALUES
    -- Doctorados
    ('2301', 'Doctorado en Educación',                                                               'DOCTORADO',       80),
    ('2302', 'Doctorado en Educación Matemática',                                                    'DOCTORADO',       90),
    -- Maestrías
    ('2203', 'Maestría en Gerencia de Empresas',                                                     'MAESTRIA',        56),
    ('2205', 'Maestría en Estudios Sociales y Educación Para la Paz',                                'MAESTRIA',        47),
    ('2206', 'Maestría en Ingeniería de Recursos Hidráulicos',                                       'MAESTRIA',        48),
    ('2207', 'Maestría en Tecnologías de la Información y la Comunicación aplicadas a la Educación', 'MAESTRIA',        77),
    ('2202', 'Maestría en Educación Matemáticas',                                                    'MAESTRIA',        48),
    ('2204', 'Maestría en Práctica Pedagógica',                                                      'MAESTRIA',        41),
    ('2201', 'Maestría en Ciencias Biológicas',                                                      'MAESTRIA',        60),
    ('2208', 'Maestría en Negocios Internacionales',                                                 'MAESTRIA',        44),
    ('2209', 'Maestría en Derecho Público: Gobierno, Justicia y Derechos Humanos',                   'MAESTRIA',        48),
    -- Especializaciones
    ('2105', 'Especialización en Práctica Pedagógica',                                               'ESPECIALIZACION', 24),
    ('2102', 'Especialización en Estructuras',                                                       'ESPECIALIZACION', 30),
    ('2107', 'Especialización en Logística y Negocios Internacionales',                              'ESPECIALIZACION', 24),
    ('2101', 'Especialización en Educación, Emprendimiento y Economía Solidaria',                    'ESPECIALIZACION', 32),
    ('2106', 'Especialización en Educación para la Atención a Población Afectada por el Conflicto Armado y en Problemática Fronteriza', 'ESPECIALIZACION', 28);

-- ── Dependencias que emiten paz y salvo ──────────────────────────────────────
INSERT IGNORE INTO tp_dependencias (nombre, descripcion, activa) VALUES
    ('Biblioteca', 'Biblioteca Central UFPS',        true),
    ('Financiera', 'División Financiera UFPS',       true),
    ('Admisiones', 'Registro y Control Académico',   true),
    ('Posgrados',  'Oficina de Posgrados UFPS',      true);

-- ── Tipos de certificado ─────────────────────────────────────────────────────
-- precio_digital = precio base; costo_logistica_fisica = adicional por modalidad física
INSERT IGNORE INTO tp_tipo_certificado
        (codigo, label, descripcion, precio_digital, costo_logistica_fisica, direccion_oficina, tiempo_entrega_dias, activo)
    VALUES
    ('TERMINACION_MATERIAS', 'CERTIFICADO DE TERMINACIÓN DE MATERIAS',
        'Certificado oficial que acredita la culminación satisfactoria de todos los requisitos académicos del programa de posgrado.',
        150000, 0, NULL, 1, true),
    ('CONSTANCIA_REGISTRO_CALIFICADO', 'CONSTANCIA REGISTRO CALIFICADO DE UN PROGRAMA ACADEMICO',
        'Constancia oficial del registro calificado vigente del programa académico de posgrado.',
        8800, 3700, 'Bloque A - Oficina Admisiones y Registro', 2, true),
    ('CONSTANCIA_MATRICULA', 'CONSTANCIA MATRICULA ACADEMICA POSGRADO',
        'Constancia de matrícula académica vigente para el periodo actual.',
        6500, 3300, 'Bloque A - Oficina Admisiones y Registro', 2, true),
    ('CONSTANCIA_BUENA_CONDUCTA', 'CONSTANCIA BUENA CONDUCTA POSGRADO',
        'Constancia de comportamiento disciplinario y académico del estudiante.',
        7200, 3800, 'Bloque A - Oficina Admisiones y Registro', 3, true);

-- ── Tipos de solicitud y su costo ────────────────────────────────────────────
INSERT IGNORE INTO tp_tipos_solicitud (codigo, nombre, costo, activo) VALUES
    ('TERMINACION_MATERIAS', 'Terminación de Materias', 150000, true),
    ('GRADO',                'Solicitud de Grado',      250000, true);
