-- ============================================================================
-- Fechas de grado iniciales (OPCIONAL, MySQL / Railway)
--
-- La tabla `fechas_grado` la crea Hibernate al arrancar (ddl-auto=update) y
-- empieza VACÍA en producción: hasta que la oficina de Posgrados publique
-- fechas desde la pantalla "Fechas de Grado", el estudiante no verá opciones en
-- el Paso 3. Si prefieres arrancar con algunas, ajusta las fechas (deben ser
-- futuras) y ejecuta este script una vez.
-- ============================================================================
INSERT IGNORE INTO fechas_grado (fecha, modalidad, hora, lugar, activa) VALUES
('2026-11-04', 'SECRETARIA', '8:00 AM',  'Secretaría de Posgrados',  1),
('2026-11-13', 'CEREMONIA',  '9:00 AM',  'Auditorio Principal UFPS', 1),
('2026-11-18', 'SECRETARIA', '8:00 AM',  'Secretaría de Posgrados',  1),
('2026-11-27', 'CEREMONIA',  '10:00 AM', 'Auditorio Principal UFPS', 1);
