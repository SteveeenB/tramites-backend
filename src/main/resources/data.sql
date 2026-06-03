-- ============================================================
-- data.sql  –  datos iniciales para PostgreSQL (Supabase)
-- Se usa ON CONFLICT DO NOTHING para evitar duplicados
-- cada vez que reinicia la aplicación (ddl-auto=update)
-- ============================================================

-- ── Roles (deben existir antes que los usuarios) ─────────────────
INSERT INTO roles (id, nombre) VALUES
  (1, 'ESTUDIANTE'),
  (2, 'DIRECTOR'),
  (3, 'ADMIN'),
  (4, 'POSGRADOS'),
  (5, 'DEPENDENCIA')
ON CONFLICT (id) DO NOTHING;

INSERT INTO programa_academico (nombre, tipo, total_creditos) VALUES
-- Doctorados
('Doctorado en Educación',                                                                                      'DOCTORADO',       80),
('Doctorado en Educación Matemática',                                                                           'DOCTORADO',       90),
-- Maestrías
('Maestría en Gerencia de Empresas',                                                                            'MAESTRIA',        56),
('Maestría en Estudios Sociales y Educación Para la Paz',                                                       'MAESTRIA',        47),
('Maestría en Ingeniería de Recursos Hidráulicos',                                                              'MAESTRIA',        48),
('Maestría en Tecnologías de la Información y la Comunicación aplicadas a la Educación',                        'MAESTRIA',        77),
('Maestría en Educación Matemáticas',                                                                           'MAESTRIA',        48),
('Maestría en Práctica Pedagógica',                                                                             'MAESTRIA',        41),
('Maestría en Ciencias Biológicas',                                                                             'MAESTRIA',        60),
('Maestría en Negocios Internacionales',                                                                        'MAESTRIA',        44),
('Maestría en Derecho Público: Gobierno, Justicia y Derechos Humanos',                                          'MAESTRIA',        48),
-- Especializaciones
('Especialización en Práctica Pedagógica',                                                                      'ESPECIALIZACION', 24),
('Especialización en Estructuras',                                                                              'ESPECIALIZACION', 30),
('Especialización en Logística y Negocios Internacionales',                                                     'ESPECIALIZACION', 24),
('Especialización en Educación, Emprendimiento y Economía Solidaria',                                           'ESPECIALIZACION', 32),
('Especialización en Educación para la Atención a Población Afectada por el Conflicto Armado y en Problemática Fronteriza', 'ESPECIALIZACION', 28)
ON CONFLICT (nombre) DO NOTHING;
 
INSERT INTO usuario (cedula, codigo, nombre_completo, contrasena, rol_id, programa_id) VALUES
-- Estudiante bloqueado: 40/56 créditos → perfil en tabla estudiante
('1098765432', '20261001', 'Juan Perez',     '123456',
    (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE'),
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante habilitado: 56/56 créditos
('1098765435', '20261005', 'Laura Gomez',    '123456',
    (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE'),
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante demo: solicitud pendiente de pago
('1098765436', '20261006', 'Pedro Martinez', '123456',
    (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE'),
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante demo: solicitud rechazada
('1098765437', '20261007', 'Carlos Rueda',   '123456',
    (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE'),
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Director del mismo programa para que la bandeja tenga datos
('1098765433', '20261002', 'Maria Director', '123456',
    (SELECT id FROM roles WHERE nombre = 'DIRECTOR'),
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Coordinador de Posgrados (gestión y configuración)
('1098765434', '20261003', 'Coordinador Posgrados', '123456',
    (SELECT id FROM roles WHERE nombre = 'POSGRADOS'),
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'))
ON CONFLICT (cedula) DO NOTHING;
 
INSERT INTO solicitud (cedula, tipo, estado, fecha_solicitud, costo, observaciones) VALUES
-- Laura: terminación aprobada → etapa 2 habilitada
('1098765435', 'TERMINACION_MATERIAS', 'APROBADA',       '2026-04-10', 150000, 'Aprobada por el director.'),
-- Pedro: pendiente de pago → aparece en bandeja como pendiente
('1098765436', 'TERMINACION_MATERIAS', 'PENDIENTE_PAGO', '2026-04-12', 150000, 'En espera de pago.'),
-- Carlos: rechazada → aparece en bandeja como rechazada
('1098765437', 'TERMINACION_MATERIAS', 'RECHAZADA',      '2026-04-08', 150000, 'No cumple requisitos adicionales del programa.')
ON CONFLICT DO NOTHING;
 
-- ============================================================
-- Catálogo de Dependencias (entidad Dependencia configurable)
-- ============================================================

INSERT INTO dependencias (nombre, descripcion, activa) VALUES
('Biblioteca',  'Biblioteca Central UFPS',                 true),
('Financiera',  'División Financiera UFPS',                true),
('Admisiones',  'Registro y Control Académico',            true),
('Posgrados',   'Oficina de Posgrados UFPS',               true)
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- Usuarios para el proceso de Paz y Salvo
-- ============================================================

-- Director de programa (con correo para probar paz y salvo)
INSERT INTO usuario (cedula, codigo, nombre_completo, contrasena, rol_id, correo, programa_id) VALUES
('2000000001', 'DIR001', 'Carlos Director Grado', '123456',
    (SELECT id FROM roles WHERE nombre = 'DIRECTOR'),
    'director.posgrado@test.com',
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'))
ON CONFLICT (cedula) DO NOTHING;

-- ============================================================
-- Admins (POSGRADOS, DEPENDENCIA, SUPER) — viven en `admins`
-- tras el refactor del modelo híbrido de identidad
-- (ver tramites-frontend/src/docs/plan_roles_v2.md).
--
-- Las contraseñas son el hash BCrypt de "123456" — el mismo
-- documentado en security/AUTH.md.
-- ============================================================
INSERT INTO admins (codigo, nombre_completo, email, password, tipo, es_super_admin, dependencia_id)
VALUES
  ('ADMIN1', 'Administrador',         'admin@ufps.edu.co',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'SUPER',       true,  NULL),
  ('POS001', 'Oficina Posgrados',     'posgrados@ufps.edu.co',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'POSGRADOS',   false, NULL),
  ('DEP001', 'Biblioteca Central',    'kevarias.2195@gmail.com',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'DEPENDENCIA', false, (SELECT id FROM dependencias WHERE nombre = 'Biblioteca')),
  ('DEP002', 'División Financiera',   'financiera@test.com',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'DEPENDENCIA', false, (SELECT id FROM dependencias WHERE nombre = 'Financiera')),
  ('DEP003', 'Admisiones y Registro', 'admisiones@test.com',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'DEPENDENCIA', false, (SELECT id FROM dependencias WHERE nombre = 'Admisiones'))
ON CONFLICT (codigo) DO NOTHING;

-- Estudiante con créditos completos y terminación aprobada (para probar solicitud de grado y paz y salvo)
INSERT INTO usuario (cedula, codigo, nombre_completo, contrasena, rol_id, correo, programa_id) VALUES
('2000000010', 'EST010', 'Andrea Prueba Grado', '123456',
    (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE'),
    'andrea.grado@test.com',
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'))
ON CONFLICT (cedula) DO NOTHING;
 
-- Terminación de materias aprobada para Andrea (requisito previo para solicitar grado)
INSERT INTO solicitud (cedula, tipo, estado, fecha_solicitud, costo, observaciones) VALUES
('2000000010', 'TERMINACION_MATERIAS', 'APROBADA', '2026-04-15', 150000, 'Aprobada por el director.')
ON CONFLICT DO NOTHING;
 
-- ── Tipos de certificado ──────────────────────────────────────────────
-- precio_digital      = precio base del documento
-- costo_logistica_fisica = delta adicional cuando se elige modalidad física (impresión + sello + manejo)

-- Tipo especial para el certificado de terminación de materias (tramite de grado)
-- No se vende directamente: lo genera el sistema al aprobar el trámite.
-- Se registra aquí para que el admin pueda configurar su plantilla HTML.
INSERT INTO tipo_certificado (codigo, label, descripcion, precio_digital, costo_logistica_fisica,
                              tiempo_entrega_dias, activo)
VALUES ('TERMINACION_MATERIAS', 'CERTIFICADO DE TERMINACIÓN DE MATERIAS',
        'Certificado oficial que acredita la culminación satisfactoria de todos los requisitos académicos del programa de posgrado.',
        150000, 0, 1, true)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO tipo_certificado (codigo, label, descripcion, precio_digital, costo_logistica_fisica,
                              direccion_oficina, tiempo_entrega_dias, activo) VALUES
('CONSTANCIA_REGISTRO_CALIFICADO', 'CONSTANCIA REGISTRO CALIFICADO DE UN PROGRAMA ACADEMICO',
    'Constancia oficial del registro calificado vigente del programa académico de posgrado.',
    8800, 3700, 'Bloque A - Oficina Admisiones y Registro', 2, true),
('CONSTANCIA_MATRICULA', 'CONSTANCIA MATRICULA ACADEMICA POSGRADO',
    'Constancia de matrícula académica vigente para el periodo actual.',
    6500, 3300, 'Bloque A - Oficina Admisiones y Registro', 2, true),
('CONSTANCIA_BUENA_CONDUCTA', 'CONSTANCIA BUENA CONDUCTA POSGRADO',
    'Constancia de comportamiento disciplinario y académico del estudiante.',
    7200, 3800, 'Bloque A - Oficina Admisiones y Registro', 3, true)
ON CONFLICT (codigo) DO UPDATE SET
    label                  = EXCLUDED.label,
    descripcion            = EXCLUDED.descripcion,
    precio_digital         = EXCLUDED.precio_digital,
    costo_logistica_fisica = EXCLUDED.costo_logistica_fisica,
    direccion_oficina      = EXCLUDED.direccion_oficina,
    tiempo_entrega_dias    = EXCLUDED.tiempo_entrega_dias;

-- ============================================================
-- Perfiles de Estudiante (debe ir DESPUÉS de usuario por el FK usuario_id)
-- creditosAprobados y estadoGrado viven aquí, no en usuario
-- ============================================================

INSERT INTO estudiante (nombre, apellido, cedula, codigo, email, es_posgrado, migrado,
    creditos_aprobados, programa_id, usuario_id)
VALUES
    ('Juan',   'Perez',    '1098765432', '20261001', 'raulbaezjxd123@gmail.com',      true, false, 40,
     (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'),
     (SELECT id FROM usuario WHERE cedula = '1098765432')),
    ('Laura',  'Gomez',    '1098765435', '20261005', 'laura@example.com',     true, false, 56,
     (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'),
     (SELECT id FROM usuario WHERE cedula = '1098765435')),
    ('Pedro',  'Martinez', '1098765436', '20261006', 'pedro@example.com',     true, false, 56,
     (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'),
     (SELECT id FROM usuario WHERE cedula = '1098765436')),
    ('Carlos', 'Rueda',    '1098765437', '20261007', 'carlos@example.com',    true, false, 56,
     (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'),
     (SELECT id FROM usuario WHERE cedula = '1098765437')),
    ('Andrea', 'Prueba',   '2000000010', 'EST010',   'andrea.grado@test.com', true, false, 56,
     (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'),
     (SELECT id FROM usuario WHERE cedula = '2000000010'))
ON CONFLICT DO NOTHING;
