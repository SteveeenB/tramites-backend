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
('Especialización en Educación para la Atención a Población Afectada por el Conflicto Armado y en Problemática Fronteriza', 'ESPECIALIZACION', 28);

INSERT INTO usuario (cedula, codigo, nombre, contrasena, rol, creditos_aprobados, programa_id) VALUES
-- Estudiante bloqueado: 40/56 créditos → etapa 1 bloqueada
('1098765432', '20261001', 'Juan Perez',     '123456', 'ESTUDIANTE', 40,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante habilitado: 56/56 créditos → terminación aprobada → etapa 2 disponible
('1098765435', '20261005', 'Laura Gomez',    '123456', 'ESTUDIANTE', 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
('1098765433', '20261002', 'Maria Director', '123456', 'DIRECTOR',   48,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Educación Matemáticas')),
('1098765434', '20261003', 'Admin User',     '123456', 'ADMIN',      30,
    (SELECT id FROM programa_academico WHERE nombre = 'Especialización en Estructuras'));

-- Laura ya tiene su terminación de materias aprobada → etapa 2 habilitada
INSERT INTO solicitud (cedula, tipo, estado, fecha_solicitud, costo, observaciones) VALUES
('1098765435', 'TERMINACION_MATERIAS', 'APROBADA', '2026-04-10', 150000, 'Aprobada por el director.');
