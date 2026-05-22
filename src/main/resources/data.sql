-- ============================================================
-- data.sql  –  datos iniciales para PostgreSQL (Supabase)
-- Se usa ON CONFLICT DO NOTHING para evitar duplicados
-- cada vez que reinicia la aplicación (ddl-auto=update)
-- ============================================================

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

INSERT INTO usuario (cedula, codigo, nombre_completo, primer_nombre, primer_apellido, contrasena, rol_id, creditos_aprobados, programa_id) VALUES
-- Estudiante bloqueado: 40/56 créditos → etapa 1 bloqueada
('1098765432', '20261001', 'Juan Perez',     'Juan',  'Perez',    '123456', 1, 40,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante habilitado: 56/56 créditos → terminación aprobada → etapa 2 habilitada
('1098765435', '20261005', 'Laura Gomez',    'Laura', 'Gomez',    '123456', 1, 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante demo: solicitud pendiente de pago
('1098765436', '20261006', 'Pedro Martinez', 'Pedro', 'Martinez', '123456', 1, 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Estudiante demo: solicitud rechazada
('1098765437', '20261007', 'Carlos Rueda',   'Carlos','Rueda',    '123456', 1, 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
-- Director del mismo programa
('1098765433', '20261002', 'Maria Director', 'Maria', 'Director', '123456', 2, NULL,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas')),
('1098765434', '20261003', 'Admin User',     'Admin', 'User',     '123456', 3, 30,
    (SELECT id FROM programa_academico WHERE nombre = 'Especialización en Estructuras'))
ON CONFLICT (cedula) DO NOTHING;

-- Estudiantes adicionales para TIC
INSERT INTO usuario (cedula, codigo, nombre_completo, primer_nombre, primer_apellido, contrasena, rol_id, creditos_aprobados, programa_id) VALUES
('1098765440', '20261010', 'Ana Torres', 'Ana', 'Torres', '123456', 1, 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Tecnologías de la Información y la Comunicación aplicadas a la Educación'))
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
-- Usuarios para el proceso de Paz y Salvo
-- ============================================================

INSERT INTO usuario (cedula, codigo, nombre_completo, primer_nombre, primer_apellido, contrasena, email, rol_id, programa_id) VALUES
('2000000001', 'DIR001', 'Carlos Director Grado', 'Carlos', 'Director', '123456', 'director.posgrado@test.com', 2,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'))
ON CONFLICT (cedula) DO NOTHING;

INSERT INTO usuario (cedula, codigo, nombre_completo, primer_nombre, primer_apellido, contrasena, email, rol_id, programa_id) VALUES
('3000000001', 'DEP001', 'Biblioteca Central',     'Biblioteca', 'Central',    '123456', 'kevarias.2195@gmail.com', 5, NULL),
('3000000002', 'DEP002', 'División Financiera',    'División',   'Financiera', '123456', 'financiera@test.com',     5, NULL),
('3000000003', 'DEP003', 'Admisiones y Registro',  'Admisiones', 'Registro',   '123456', 'admisiones@test.com',     5, NULL)
ON CONFLICT (cedula) DO NOTHING;

INSERT INTO usuario (cedula, codigo, nombre_completo, primer_nombre, primer_apellido, contrasena, email, rol_id, creditos_aprobados, programa_id) VALUES
('2000000010', 'EST010', 'Andrea Prueba Grado', 'Andrea', 'Prueba', '123456', 'andrea.grado@test.com', 1, 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'))
ON CONFLICT (cedula) DO NOTHING;

INSERT INTO usuario (cedula, codigo, nombre_completo, primer_nombre, primer_apellido, contrasena, email, rol_id, creditos_aprobados, programa_id) VALUES
('2000000011', 'EST011', 'Kevin Estudiante', 'Kevin', 'Estudiante', '123456', 'kevin.est@test.com', 1, 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'))
ON CONFLICT (cedula) DO NOTHING;

INSERT INTO usuario (cedula, codigo, nombre_completo, primer_nombre, primer_apellido, contrasena, email, rol_id, programa_id) VALUES
('1098765434', '20261003', 'Coordinador Posgrados', 'Coordinador', 'Posgrados', '123456', 'posgrados@test.com', 4,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'))
ON CONFLICT (cedula) DO UPDATE SET rol_id = 4;

INSERT INTO solicitud (cedula, tipo, estado, fecha_solicitud, costo, observaciones) VALUES
('2000000010', 'TERMINACION_MATERIAS', 'APROBADA', '2026-04-15', 150000, 'Aprobada por el director.')
ON CONFLICT DO NOTHING;
