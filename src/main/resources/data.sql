-- ============================================================
-- data.sql  –  datos iniciales para PostgreSQL (Supabase)
-- Se usa ON CONFLICT DO NOTHING para evitar duplicados
-- cada vez que reinicia la aplicación (ddl-auto=update)
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
('1098765437', 'TERMINACION_MATERIAS', 'RECHAZADA',      '2026-04-08', 150000, 'No cumple requisitos adicionales del programa.')
ON CONFLICT DO NOTHING;
 
-- ============================================================
-- Usuarios para el proceso de Paz y Salvo
-- ============================================================
 
-- Director de programa (nuevo, con correo para probar paz y salvo)
INSERT INTO usuario (cedula, codigo, nombre, contrasena, rol, correo, programa_id) VALUES
('2000000001', 'DIR001', 'Carlos Director Grado', '123456', 'DIRECTOR', 'director.posgrado@test.com',
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'))
ON CONFLICT (cedula) DO NOTHING;
 
-- Dependencias (rol DEPENDENCIA)
INSERT INTO usuario (cedula, codigo, nombre, contrasena, rol, correo, programa_id) VALUES
('3000000001', 'DEP001', 'Biblioteca Central', '123456', 'DEPENDENCIA', 'kevarias.2195@gmail.com', NULL),
('3000000002', 'DEP002', 'División Financiera', '123456', 'DEPENDENCIA', 'financiera@test.com', NULL),
('3000000003', 'DEP003', 'Admisiones y Registro', '123456', 'DEPENDENCIA', 'admisiones@test.com', NULL)
ON CONFLICT (cedula) DO NOTHING;
 
-- Estudiante con créditos completos y terminación aprobada (para probar solicitud de grado y paz y salvo)
INSERT INTO usuario (cedula, codigo, nombre, contrasena, rol, correo, creditos_aprobados, programa_id) VALUES
('2000000010', 'EST010', 'Andrea Prueba Grado', '123456', 'ESTUDIANTE', 'andrea.grado@test.com', 56,
    (SELECT id FROM programa_academico WHERE nombre = 'Maestría en Gerencia de Empresas'))
ON CONFLICT (cedula) DO NOTHING;
 
-- Terminación de materias aprobada para Andrea (requisito previo para solicitar grado)
INSERT INTO solicitud (cedula, tipo, estado, fecha_solicitud, costo, observaciones) VALUES
('2000000010', 'TERMINACION_MATERIAS', 'APROBADA', '2026-04-15', 150000, 'Aprobada por el director.')
ON CONFLICT DO NOTHING;
 