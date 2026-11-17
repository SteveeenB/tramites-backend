-- ============================================================================
-- 03_comprobacion.sql — Comprobar permisos y tablas de Trámites (solo lectura)
-- (TP-209 · acuerdo TP-208)
--
-- No modifica nada. Ejecutar conectado a la base compartida (defaultdb).
-- Enviar el resultado de las consultas 1 a 4 al PO de Trámites.
-- ============================================================================

-- 1. Permisos del usuario de Trámites (con el administrador).
--    Deben salir 18 líneas GRANT ... ON `defaultdb`.`tp_...` y una
--    GRANT USAGE ON *.*. No debe aparecer ningún permiso sobre `defaultdb`.*
--    ni sobre tablas sin prefijo tp_.
SHOW GRANTS FOR 'tramites_app'@'%';

-- 2. Tablas de Trámites: debe dar 17 (18 cuando el backend arranque por
--    primera vez y Flyway cree tp_flyway_schema_history).
SELECT COUNT(*) AS tablas_tramites
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'tp\_%';

-- 3. Tablas de Matrícula: debe dar el mismo número ANTES y DESPUÉS de correr
--    01 y 02 (53 en el esquema que revisamos en TP-208).
SELECT COUNT(*) AS tablas_matricula
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name NOT LIKE 'tp\_%';

-- 4. Llaves foráneas: ninguna debe cruzar entre tablas tp_* y tablas de
--    Matrícula. Debe devolver 0 filas.
SELECT table_name, constraint_name, referenced_table_name
  FROM information_schema.key_column_usage
 WHERE table_schema = DATABASE()
   AND referenced_table_name IS NOT NULL
   AND ((table_name LIKE 'tp\_%') <> (referenced_table_name LIKE 'tp\_%'));

-- 5. (Opcional, conectado como tramites_app.) Debe fallar con
--    "SELECT command denied": Trámites no puede leer tablas de Matrícula.
-- SELECT COUNT(*) FROM usuarios;
