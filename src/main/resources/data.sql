-- ============================================================
-- data.sql  –  datos iniciales para PostgreSQL (Supabase)
-- Se usa ON CONFLICT DO NOTHING para evitar duplicados
-- ============================================================

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

INSERT INTO usuario (cedula, codigo, nombre, contrasena, rol, creditos_aprobados, programa_id) VALUES
-- Estudiante bloqueado: 40/56 créditos → etapa 1 bloqueada
('1098765432', '20261001', 'Juan Perez',     '123456', 'ESTUDIANTE', 40,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante habilitado: 56/56 créditos → terminación aprobada → etapa 2 habilitada
('1098765435', '20261005', 'Laura Gomez',    '123456', 'ESTUDIANTE', 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante demo: solicitud pendiente de pago
('1098765436', '20261006', 'Pedro Martinez', '123456', 'ESTUDIANTE', 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante demo: solicitud rechazada
('1098765437', '20261007', 'Carlos Rueda',   '123456', 'ESTUDIANTE', 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante demo con GRADO aprobado → puede solicitar Aval Paz y Salvo
('1098765438', '20261008', 'Ana Torres',     '123456', 'ESTUDIANTE', 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante demo con Aval ya aprobado (para probar descarga de documento)
('1098765439', '20261009', 'Luis Mora',      '123456', 'ESTUDIANTE', 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Director del mismo programa para que la bandeja tenga datos
('1098765433', '20261002', 'Maria Director', '123456', 'DIRECTOR',   NULL,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
('1098765434', '20261003', 'Admin User',     '123456', 'ADMIN',      30,
    (SELECT id FROM programa_academico WHERE nombre = 'Especialización en Estructuras'))
ON CONFLICT (cedula) DO NOTHING;

INSERT INTO solicitud (cedula, tipo, estado, fecha_solicitud, costo, observaciones) VALUES
-- Laura: terminación aprobada → etapa 2 habilitada
('1098765435', 'TERMINACION_MATERIAS', 'APROBADA',       '2026-04-10', 150000, 'Aprobada por el director.'),
-- Pedro: pendiente de pago → aparece en bandeja como pendiente
('1098765436', 'TERMINACION_MATERIAS', 'PENDIENTE_PAGO', '2026-04-12', 150000, 'En espera de pago.'),
-- Carlos: rechazada → aparece en bandeja como rechazada
('1098765437', 'TERMINACION_MATERIAS', 'RECHAZADA',      '2026-04-08', 150000, 'No cumple requisitos adicionales del programa.'),
-- Ana: proceso completo hasta GRADO aprobado → puede solicitar Aval Paz y Salvo
('1098765438', 'TERMINACION_MATERIAS', 'APROBADA',       '2026-03-15', 150000, 'Aprobada por el director.'),
('1098765438', 'GRADO',               'APROBADA',        '2026-03-20', 250000, 'Derechos de grado aprobados.'),
-- Luis: tiene Aval Paz y Salvo ya aprobado (flujo completo demo)
('1098765439', 'TERMINACION_MATERIAS', 'APROBADA',       '2026-03-01', 150000, 'Aprobada por el director.'),
('1098765439', 'GRADO',               'APROBADA',        '2026-03-05', 250000, 'Derechos de grado aprobados.'),
('1098765439', 'AVAL_PAZ_SALVO',      'AVAL_APROBADO',   '2026-03-10', 0,      'Aval Paz y Salvo aprobado por el Director de Programa.')
ON CONFLICT DO NOTHING;
