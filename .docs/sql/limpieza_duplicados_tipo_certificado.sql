-- ============================================================================
-- Limpieza de duplicados en tipo_certificado (MySQL / Railway)
--
-- Síntoma: HTTP 500 en POST /api/solicitudes/{id}/aprobar-posgrados (y en los
-- flujos de certificados) con
--   IncorrectResultSizeDataAccessException: Query did not return a unique
--   result: N results were returned
-- porque TipoCertificadoRepository.findByCodigo() devuelve un Optional y
-- encuentra N filas con el mismo `codigo`.
--
-- Causa: tipo_certificado.codigo no tenía UNIQUE, así que en data.sql tanto
-- INSERT IGNORE como ON DUPLICATE KEY UPDATE (que dependen de una clave única)
-- no hacían nada y cada ejecución del seed volvía a insertar las mismas filas.
--
-- Seguro respecto a FKs: solicitud_certificado.tipo_certificado guarda el
-- `codigo` (texto), no el id de esta tabla.
--
-- Ejecutar en orden, paso por paso.
-- ============================================================================

-- 1) Ver qué códigos están duplicados y si alguna copia tiene plantilla
--    (la plantilla HTML la edita el admin desde la UI y queda en UNA fila).
SELECT codigo,
       COUNT(*)                                                    AS veces,
       MIN(id)                                                     AS id_mas_antiguo,
       SUM(plantilla_html IS NOT NULL AND plantilla_html <> '')    AS copias_con_plantilla
FROM tipo_certificado
GROUP BY codigo
HAVING COUNT(*) > 1;

-- 2) Respaldo (solo si no existe ya).
CREATE TABLE IF NOT EXISTS tipo_certificado_respaldo AS
SELECT * FROM tipo_certificado;

-- 3) Borrar duplicados conservando UNA fila por código:
--    la que tiene plantilla HTML (si hay) y, si no, la de menor id.
START TRANSACTION;

DELETE t
FROM tipo_certificado t
JOIN (
    SELECT codigo,
           COALESCE(
               MIN(CASE WHEN plantilla_html IS NOT NULL AND plantilla_html <> '' THEN id END),
               MIN(id)
           ) AS conservar
    FROM tipo_certificado
    GROUP BY codigo
) k ON k.codigo = t.codigo AND t.id <> k.conservar;

-- Verificación: debe devolver exactamente 1 por código.
SELECT codigo, COUNT(*) AS veces FROM tipo_certificado GROUP BY codigo;

-- Si todo cuadra:
COMMIT;
-- Si algo no cuadra: ROLLBACK;

-- 4) Candado para que no vuelva a pasar. Mismo nombre que declara la entidad
--    TipoCertificado (@UniqueConstraint), así Hibernate (ddl-auto=update) no
--    crea una segunda restricción equivalente.
ALTER TABLE tipo_certificado
    ADD CONSTRAINT uq_tipo_certificado_codigo UNIQUE (codigo);

-- ============================================================================
-- Otras tablas del seed que pueden tener el mismo problema (no tienen UNIQUE
-- natural). Solo detectar; borrar con cuidado porque tienen tablas hijas:
--   SELECT cedula, tipo, estado, fecha_solicitud, COUNT(*) FROM solicitud
--     GROUP BY cedula, tipo, estado, fecha_solicitud HAVING COUNT(*) > 1;
--   SELECT cedula, COUNT(*) FROM estudiante GROUP BY cedula HAVING COUNT(*) > 1;
-- ============================================================================
