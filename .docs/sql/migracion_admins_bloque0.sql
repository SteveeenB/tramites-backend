-- ============================================================
-- Bloque 0 del refactor de identidad (plan_roles_v2.md §4)
-- Crea la tabla `admins` paralela a `usuario`. Queda vacía:
-- los admins reales todavía viven en `usuario` y los logins
-- siguen pasando por ahí (la doble búsqueda del AuthController
-- consulta primero `admins` y cae a `usuario` cuando no hay
-- match).
--
-- IMPORTANTE: este script se ejecuta UNA VEZ contra Supabase.
-- Hibernate (ddl-auto=update) no recreará la tabla porque la
-- entidad Admin.java está calcada de esta DDL.
-- ============================================================

CREATE TABLE IF NOT EXISTS admins (
  id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  codigo            VARCHAR(20)  UNIQUE,
  primer_nombre     VARCHAR(50),
  segundo_nombre    VARCHAR(50),
  primer_apellido   VARCHAR(50),
  segundo_apellido  VARCHAR(50),
  nombre_completo   VARCHAR(150),
  email             VARCHAR(100) UNIQUE NOT NULL,
  password          VARCHAR(255) NOT NULL,
  es_super_admin    BOOLEAN      DEFAULT false,
  active            BOOLEAN      DEFAULT true,
  tipo              VARCHAR(30)
                    CHECK (tipo IN ('SUPER','POSGRADOS','DEPENDENCIA')),
  dependencia_id    BIGINT       REFERENCES dependencias(id),
  fecha_creacion    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admins_email  ON admins(email);
CREATE INDEX IF NOT EXISTS idx_admins_codigo ON admins(codigo);
CREATE INDEX IF NOT EXISTS idx_admins_tipo   ON admins(tipo);
