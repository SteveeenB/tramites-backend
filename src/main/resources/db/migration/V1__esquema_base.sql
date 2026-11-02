-- ============================================================================
-- V1__esquema_base.sql — Línea base del esquema (TP-150 · SC02-T2)
--
-- Reproduce el esquema que hasta ahora generaba Hibernate con
-- ddl-auto=update a partir de las entidades de com.ufps.tramites.model.
--
-- * BD nueva (vacía): Flyway ejecuta este script y crea todo el esquema.
-- * BD existente (Railway): Flyway NO lo ejecuta; con baseline-on-migrate la
--   marca como versión 1 y desde ahí aplica solo las migraciones V2, V3, ...
--
-- Reglas a partir de ahora:
-- 1. Nunca editar este archivo ni una migración ya aplicada.
-- 2. Todo cambio de esquema (tabla, columna, índice, FK) va en un archivo
--    nuevo src/main/resources/db/migration/V<n>__<descripcion>.sql, en el
--    mismo PR que cambia la entidad.
-- 3. Los datos semilla de desarrollo siguen en data.sql (perfil h2); no van aquí.
-- ============================================================================

-- ── Catálogos ───────────────────────────────────────────────────────────────

create table roles (
    id     bigint       not null,
    nombre varchar(255),
    primary key (id)
) engine=InnoDB;

create table estados_estudiantes (
    id     bigint       not null auto_increment,
    nombre varchar(255) not null,
    primary key (id),
    constraint uq_estados_estudiantes_nombre unique (nombre)
) engine=InnoDB;

create table programa_academico (
    id             bigint       not null auto_increment,
    nombre         varchar(255),
    tipo           varchar(255),
    total_creditos integer,
    primary key (id),
    constraint uq_programa_academico_nombre unique (nombre)
) engine=InnoDB;

create table dependencias (
    id          bigint       not null auto_increment,
    nombre      varchar(255),
    descripcion varchar(255),
    activa      bit,
    primary key (id),
    constraint uq_dependencias_nombre unique (nombre)
) engine=InnoDB;

create table tipos_solicitud (
    id     bigint       not null auto_increment,
    codigo varchar(255) not null,
    nombre varchar(255),
    costo  float(53),
    activo bit,
    primary key (id),
    constraint uq_tipos_solicitud_codigo unique (codigo)
) engine=InnoDB;

create table tipo_certificado (
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

create table fechas_grado (
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

create table usuario (
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

create table admins (
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

create table estudiante (
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
    primary key (id),
    constraint uq_estudiante_usuario unique (usuario_id)
) engine=InnoDB;

-- ── Trámites ────────────────────────────────────────────────────────────────

create table solicitud (
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

create table historial_estado_solicitud (
    id                  bigint       not null auto_increment,
    solicitud_id        bigint       not null,
    estado_anterior     varchar(255),
    estado_nuevo        varchar(255) not null,
    fecha               datetime(6)  not null,
    actor_identificador varchar(255),
    actor_tipo          varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_hes_solicitud_id on historial_estado_solicitud (solicitud_id);
create index idx_hes_fecha        on historial_estado_solicitud (fecha);

create table documento_solicitud (
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

create table pagos (
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

create table paz_y_salvo (
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

create table solicitud_certificado (
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

create table notificaciones (
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

alter table usuario
    add constraint fk_usuario_rol      foreign key (rol_id)      references roles (id),
    add constraint fk_usuario_programa foreign key (programa_id) references programa_academico (id);

alter table admins
    add constraint fk_admins_dependencia foreign key (dependencia_id) references dependencias (id);

alter table estudiante
    add constraint fk_estudiante_estado   foreign key (estado_estudiante_id) references estados_estudiantes (id),
    add constraint fk_estudiante_programa foreign key (programa_id)          references programa_academico (id),
    add constraint fk_estudiante_usuario  foreign key (usuario_id)           references usuario (id);

alter table solicitud
    add constraint fk_solicitud_estudiante     foreign key (estudiante_id)      references estudiante (id),
    add constraint fk_solicitud_posgrados_admin foreign key (posgrados_admin_id) references admins (id);

alter table paz_y_salvo
    add constraint fk_pys_estudiante          foreign key (estudiante_id)          references estudiante (id),
    add constraint fk_pys_responsable_admin   foreign key (responsable_admin_id)   references admins (id),
    add constraint fk_pys_responsable_usuario foreign key (responsable_usuario_id) references usuario (id),
    add constraint fk_pys_dependencia         foreign key (dependencia_id)         references dependencias (id);

alter table solicitud_certificado
    add constraint fk_solcert_estudiante foreign key (estudiante_id) references estudiante (id);
