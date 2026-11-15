-- ============================================================
-- SC-03 · Código de cada programa en el Módulo de Matrícula
--
-- La respuesta de Matrícula identifica el programa por su código
-- (programas.codigo). Sin este mapeo, la sincronización solo podía
-- adoptar el código comparando nombres, y dos programas tienen
-- nombres distintos en los dos módulos:
--   «Maestría en Tecnologías de la Información y la Comunicación
--    aplicadas a la Educación» = 2207 «Maestria en TIC aplicadas a la Educacion»
--   «Maestría en Educación Matemáticas» = 2202 «Maestria en Educacion Matematica»
--
-- Códigos tomados del catálogo de programas de Matrícula (DML_Final,
-- 2 nov 2026). Sus programas 2001, 2002, 2103 y 2104 no existen en
-- Trámites y no se crean aquí.
--
-- Requiere migracion_sc03_snapshot_matricula.sql (columna codigo).
-- MySQL 8+. Idempotente: solo asigna códigos a programas que no tienen.
-- ============================================================

UPDATE programa_academico SET codigo = '2301' WHERE nombre = 'Doctorado en Educación' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2302' WHERE nombre = 'Doctorado en Educación Matemática' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2203' WHERE nombre = 'Maestría en Gerencia de Empresas' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2205' WHERE nombre = 'Maestría en Estudios Sociales y Educación Para la Paz' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2206' WHERE nombre = 'Maestría en Ingeniería de Recursos Hidráulicos' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2207' WHERE nombre = 'Maestría en Tecnologías de la Información y la Comunicación aplicadas a la Educación' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2202' WHERE nombre = 'Maestría en Educación Matemáticas' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2204' WHERE nombre = 'Maestría en Práctica Pedagógica' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2201' WHERE nombre = 'Maestría en Ciencias Biológicas' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2208' WHERE nombre = 'Maestría en Negocios Internacionales' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2209' WHERE nombre = 'Maestría en Derecho Público: Gobierno, Justicia y Derechos Humanos' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2105' WHERE nombre = 'Especialización en Práctica Pedagógica' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2102' WHERE nombre = 'Especialización en Estructuras' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2107' WHERE nombre = 'Especialización en Logística y Negocios Internacionales' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2101' WHERE nombre = 'Especialización en Educación, Emprendimiento y Economía Solidaria' AND codigo IS NULL;
UPDATE programa_academico SET codigo = '2106' WHERE nombre = 'Especialización en Educación para la Atención a Población Afectada por el Conflicto Armado y en Problemática Fronteriza' AND codigo IS NULL;

-- Verificación: deben quedar 0 programas sin código.
SELECT nombre FROM programa_academico WHERE codigo IS NULL;
