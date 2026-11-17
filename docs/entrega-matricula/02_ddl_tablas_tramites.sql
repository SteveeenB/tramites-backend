-- ============================================================================
-- 02_ddl_tablas_tramites.sql — Tablas del Módulo de Trámites en la BD compartida
-- (TP-209 · acuerdo TP-208, docs/BD_COMPARTIDA_MATRICULA.md)
--
-- Crea las 17 tablas tp_* de Trámites, sus índices y llaves foráneas. No crea,
-- altera ni borra ninguna tabla de Matrícula, y ninguna llave foránea apunta a
-- tablas de Matrícula.
--
-- Cómo ejecutarlo:
--   * DESPUÉS de 01_permisos_usuario_tramites.sql.
--   * Conectado a la base compartida (defaultdb) con el usuario tramites_app
--     (así se prueban sus permisos de paso). También sirve con el administrador.
--   * UNA sola vez. Si se ejecuta de nuevo, falla en la primera tabla
--     ("Table 'tp_roles' already exists") sin cambiar nada.
--   * No editarlo: es copia exacta de src/main/resources/db/migration/
--     V1__esquema_base.sql. El backend valida que las tablas coincidan.
--
-- Compatible con el sql_mode ANSI del servidor (solo comillas simples, sin
-- comillas dobles) y con sql_require_primary_key (las 17 tablas tienen llave
-- primaria). Charset y collation: los de la base (utf8mb4_0900_ai_ci).
--
-- Al arrancar, el backend de Trámites usa FLYWAY_BASELINE_VERSION=1: Flyway crea
-- tp_flyway_schema_history, registra este esquema como versión 1 y no lo
-- vuelve a ejecutar.
-- ============================================================================

-- ── Catálogos ───────────────────────────────────────────────────────────────

create table tp_roles (
    id     bigint       not null,
    nombre varchar(255),
    primary key (id)
) engine=InnoDB;

create table tp_estados_estudiantes (
    id     bigint       not null auto_increment,
    nombre varchar(255) not null,
    primary key (id),
    constraint uq_estados_estudiantes_nombre unique (nombre)
) engine=InnoDB;

create table tp_programa_academico (
    id             bigint       not null auto_increment,
    -- Código del programa en Matrícula (SC-03). Mismo tipo que
    -- .docs/sql/migracion_sc03_snapshot_matricula.sql.
    codigo         varchar(50),
    nombre         varchar(255),
    tipo           varchar(255),
    total_creditos integer,
    primary key (id),
    constraint uq_programa_academico_nombre unique (nombre),
    constraint uk_programa_academico_codigo unique (codigo)
) engine=InnoDB;

create table tp_dependencias (
    id          bigint       not null auto_increment,
    nombre      varchar(255),
    descripcion varchar(255),
    activa      bit,
    primary key (id),
    constraint uq_dependencias_nombre unique (nombre)
) engine=InnoDB;

create table tp_tipos_solicitud (
    id     bigint       not null auto_increment,
    codigo varchar(255) not null,
    nombre varchar(255),
    costo  float(53),
    activo bit,
    primary key (id),
    constraint uq_tipos_solicitud_codigo unique (codigo)
) engine=InnoDB;

create table tp_tipo_certificado (
    id                     bigint       not null auto_increment,
    codigo                 varchar(255),
    label                  varchar(255),
    descripcion            varchar(255),
    precio_digital         float(53),
    costo_logistica_fisica float(53),
    direccion_oficina      varchar(255),
    tiempo_entrega_dias    integer,
    activo                 bit,
    plantilla_html         text,
    primary key (id),
    constraint uq_tipo_certificado_codigo unique (codigo)
) engine=InnoDB;

create table tp_fechas_grado (
    id        bigint       not null auto_increment,
    fecha     date         not null,
    modalidad varchar(255) not null,
    hora      varchar(255) not null,
    lugar     varchar(255) not null,
    activa    bit          not null,
    primary key (id),
    constraint uq_fecha_grado unique (fecha, modalidad, hora)
) engine=InnoDB;

-- ── Personas ────────────────────────────────────────────────────────────────

create table tp_usuario (
    id              bigint       not null auto_increment,
    cedula          varchar(255),
    codigo          varchar(255),
    primer_nombre   varchar(255),
    primer_apellido varchar(255),
    nombre_completo varchar(255),
    email           varchar(255),
    correo          varchar(255),
    contrasena      varchar(255),
    google_id       varchar(255),
    foto_url        varchar(255),
    moodle_id       varchar(255),
    telefono        varchar(255),
    rol_id          bigint,
    programa_id     bigint,
    primary key (id),
    constraint uq_usuario_cedula unique (cedula),
    constraint uq_usuario_codigo unique (codigo)
) engine=InnoDB;

create table tp_admins (
    id               bigint       not null auto_increment,
    codigo           varchar(255),
    primer_nombre    varchar(255),
    segundo_nombre   varchar(255),
    primer_apellido  varchar(255),
    segundo_apellido varchar(255),
    nombre_completo  varchar(255),
    email            varchar(255) not null,
    password         varchar(255) not null,
    es_super_admin   bit,
    active           bit,
    tipo             varchar(255),
    dependencia_id   bigint,
    fecha_creacion   datetime(6),
    primary key (id),
    constraint uq_admins_codigo unique (codigo),
    constraint uq_admins_email unique (email)
) engine=InnoDB;

create table tp_estudiante (
    id                   bigint       not null auto_increment,
    nombre               varchar(255),
    nombre2              varchar(255),
    apellido             varchar(255),
    apellido2            varchar(255),
    cedula               varchar(255),
    codigo               varchar(255),
    email                varchar(255),
    telefono             varchar(255),
    fecha_nacimiento     date,
    fecha_ingreso        date,
    moodle_id            varchar(255),
    es_posgrado          bit,
    migrado              bit,
    creditos_aprobados   integer,
    estado_estudiante_id bigint,
    -- Columna heredada (solo lectura en la entidad); la reemplaza estado_estudiante_id.
    estado_grado         varchar(255),
    programa_id          bigint,
    cohorte_id           bigint,
    pensum_id            bigint,
    usuario_id           bigint,
    -- Copia del estado de matrícula traída desde Matrícula (SC-03).
    estado_matricula          varchar(50),
    matricula_sincronizado_en datetime(6),
    primary key (id),
    constraint uq_estudiante_usuario unique (usuario_id)
) engine=InnoDB;

-- ── Trámites ────────────────────────────────────────────────────────────────

create table tp_solicitud (
    id                       bigint       not null auto_increment,
    estudiante_id            bigint,
    cedula                   varchar(255),
    tipo                     varchar(255),
    estado                   varchar(255),
    fecha_solicitud          date,
    costo                    float(53),
    observaciones            varchar(255),
    decision                 varchar(255),
    observaciones_director   varchar(255),
    fecha_decision           datetime(6),
    cedula_director          varchar(255),
    fecha_en_revision        datetime(6),
    titulo_proyecto          varchar(255),
    tipo_proyecto            varchar(255),
    resumen_proyecto         text,
    validacion_posgrados     varchar(255),
    observaciones_posgrados  varchar(255),
    fecha_validacion         datetime(6),
    posgrados_admin_id       bigint,
    acta_generada            bit,
    radicado                 varchar(255),
    estado_pago_grado        varchar(255),
    fecha_grado              date,
    modalidad_grado          varchar(255),
    pago_modalidad_realizado bit,
    fecha_cierre             datetime(6),
    primary key (id),
    constraint uq_solicitud_radicado unique (radicado)
) engine=InnoDB;

create table tp_historial_estado_solicitud (
    id                  bigint       not null auto_increment,
    solicitud_id        bigint       not null,
    estado_anterior     varchar(255),
    estado_nuevo        varchar(255) not null,
    fecha               datetime(6)  not null,
    actor_identificador varchar(255),
    actor_tipo          varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_hes_solicitud_id on tp_historial_estado_solicitud (solicitud_id);
create index idx_hes_fecha        on tp_historial_estado_solicitud (fecha);

create table tp_documento_solicitud (
    id                bigint       not null auto_increment,
    solicitud_id      bigint,
    tipo              varchar(255),
    nombre_original   varchar(255),
    nombre_almacenado varchar(255),
    content_type      varchar(255),
    tamano            bigint,
    fecha_subida      datetime(6),
    primary key (id)
) engine=InnoDB;

create table tp_pagos (
    id                   bigint       not null auto_increment,
    referencia           varchar(255) not null,
    solicitud_id         bigint       not null,
    cedula_estudiante    varchar(255),
    tipo_pago            varchar(255),
    monto_centavos       bigint       not null,
    estado               varchar(255) not null,
    wompi_transaction_id varchar(255),
    redirect_url         varchar(255),
    fecha_creacion       datetime(6),
    fecha_actualizacion  datetime(6),
    primary key (id),
    constraint uq_pagos_referencia unique (referencia)
) engine=InnoDB;

create table tp_paz_y_salvo (
    id                     bigint       not null auto_increment,
    solicitud_id           bigint,
    estudiante_id          bigint,
    cedula_estudiante      varchar(255),
    responsable_admin_id   bigint,
    responsable_usuario_id bigint,
    tipo_dependencia       varchar(255),
    dependencia_id         bigint,
    estado                 varchar(255),
    observaciones          varchar(255),
    fecha_solicitud        datetime(6),
    fecha_respuesta        datetime(6),
    primary key (id)
) engine=InnoDB;

create table tp_solicitud_certificado (
    id                     bigint       not null auto_increment,
    estudiante_id          bigint,
    cedula                 varchar(255),
    tipo_certificado       varchar(255),
    modalidad_envio        varchar(255),
    estado                 varchar(255),
    fecha_solicitud        date,
    fecha_vencimiento_pago date,
    fecha_pago             datetime(6),
    fecha_generacion       datetime(6),
    fecha_entrega          datetime(6),
    costo                  float(53),
    observaciones          varchar(255),
    destinatario           varchar(255),
    url_pdf                varchar(255),
    hash_pdf               varchar(255),
    primary key (id)
) engine=InnoDB;

create table tp_notificaciones (
    id                  bigint       not null auto_increment,
    destinatario_cedula varchar(255) not null,
    tipo                varchar(255) not null,
    titulo              varchar(255) not null,
    mensaje             text         not null,
    enlace              varchar(255),
    leida               bit          not null,
    fecha_creacion      datetime(6)  not null,
    primary key (id)
) engine=InnoDB;

-- ── Llaves foráneas (las mismas relaciones @ManyToOne / @OneToOne) ───────────

alter table tp_usuario
    add constraint fk_usuario_rol      foreign key (rol_id)      references tp_roles (id),
    add constraint fk_usuario_programa foreign key (programa_id) references tp_programa_academico (id);

alter table tp_admins
    add constraint fk_admins_dependencia foreign key (dependencia_id) references tp_dependencias (id);

alter table tp_estudiante
    add constraint fk_estudiante_estado   foreign key (estado_estudiante_id) references tp_estados_estudiantes (id),
    add constraint fk_estudiante_programa foreign key (programa_id)          references tp_programa_academico (id),
    add constraint fk_estudiante_usuario  foreign key (usuario_id)           references tp_usuario (id);

alter table tp_solicitud
    add constraint fk_solicitud_estudiante     foreign key (estudiante_id)      references tp_estudiante (id),
    add constraint fk_solicitud_posgrados_admin foreign key (posgrados_admin_id) references tp_admins (id);

alter table tp_paz_y_salvo
    add constraint fk_pys_estudiante          foreign key (estudiante_id)          references tp_estudiante (id),
    add constraint fk_pys_responsable_admin   foreign key (responsable_admin_id)   references tp_admins (id),
    add constraint fk_pys_responsable_usuario foreign key (responsable_usuario_id) references tp_usuario (id),
    add constraint fk_pys_dependencia         foreign key (dependencia_id)         references tp_dependencias (id);

alter table tp_solicitud_certificado
    add constraint fk_solcert_estudiante foreign key (estudiante_id) references tp_estudiante (id);

-- ── Comprobación: debe devolver 17 ──────────────────────────────────────────
SELECT COUNT(*) AS tablas_tramites
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'tp\_%';
