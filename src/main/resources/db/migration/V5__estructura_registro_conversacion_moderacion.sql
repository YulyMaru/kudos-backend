CREATE TABLE facultad (
    id        UUID         PRIMARY KEY,
    nombre    VARCHAR(150) NOT NULL,
    activa    BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_facultad_nombre ON facultad (lower(nombre));

CREATE TABLE programa (
    id          UUID         PRIMARY KEY,
    facultad_id UUID         NOT NULL REFERENCES facultad (id),
    nombre      VARCHAR(150) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_programa_nombre ON programa (facultad_id, lower(nombre));

ALTER TABLE usuario
    ADD COLUMN correo_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN facultad_id       UUID REFERENCES facultad (id),
    ADD COLUMN programa_id       UUID REFERENCES programa (id);

UPDATE usuario SET correo_verificado = TRUE WHERE estado = 'ACTIVO';

CREATE UNIQUE INDEX uk_usuario_coordinador_facultad_activo ON usuario (facultad_id)
    WHERE rol = 'COORDINADOR_FACULTAD' AND estado = 'ACTIVO';
CREATE UNIQUE INDEX uk_usuario_coordinador_programa_activo ON usuario (programa_id)
    WHERE rol = 'COORDINADOR_PROGRAMA' AND estado = 'ACTIVO';

CREATE TABLE vinculo_programa (
    usuario_id  UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    programa_id UUID        NOT NULL REFERENCES programa (id),
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, programa_id)
);

CREATE INDEX ix_vinculo_programa ON vinculo_programa (programa_id);

CREATE TABLE solicitud_registro (
    id                   UUID        PRIMARY KEY,
    usuario_id           UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol_solicitado       VARCHAR(30) NOT NULL,
    facultad_id          UUID REFERENCES facultad (id),
    programa_id          UUID REFERENCES programa (id),
    estado               VARCHAR(15) NOT NULL,
    decidida_por         UUID REFERENCES usuario (id),
    motivo_rechazo       TEXT,
    politica_aceptada_en TIMESTAMPTZ NOT NULL,
    creada_en            TIMESTAMPTZ NOT NULL,
    decidida_en          TIMESTAMPTZ,
    CONSTRAINT uk_solicitud_usuario UNIQUE (usuario_id),
    CONSTRAINT ck_solicitud_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA'))
);

CREATE INDEX ix_solicitud_pendiente ON solicitud_registro (rol_solicitado, creada_en) WHERE estado = 'PENDIENTE';

CREATE TABLE codigo_verificacion (
    id          UUID        PRIMARY KEY,
    usuario_id  UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    proposito   VARCHAR(30) NOT NULL,
    hash_codigo VARCHAR(64) NOT NULL,
    creado_en   TIMESTAMPTZ NOT NULL,
    expira_en   TIMESTAMPTZ NOT NULL,
    intentos    INTEGER     NOT NULL DEFAULT 0,
    usado_en    TIMESTAMPTZ,
    CONSTRAINT ck_codigo_proposito CHECK (proposito IN ('VERIFICAR_CORREO', 'VERIFICAR_CELULAR', 'RESTABLECER_CONTRASENA')),
    CONSTRAINT ck_codigo_intentos CHECK (intentos >= 0)
);

CREATE INDEX ix_codigo_usuario ON codigo_verificacion (usuario_id, proposito, creado_en DESC);
CREATE INDEX ix_codigo_hash ON codigo_verificacion (proposito, hash_codigo);

ALTER TABLE kudo ADD COLUMN vetado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX ix_kudo_feed_visible ON kudo (fecha_publicacion DESC, id DESC) WHERE vetado = FALSE;

CREATE TABLE replica (
    id                UUID        PRIMARY KEY,
    kudo_id           UUID        NOT NULL REFERENCES kudo (id) ON DELETE CASCADE,
    replica_padre_id  UUID REFERENCES replica (id) ON DELETE CASCADE,
    autor_id          UUID        NOT NULL REFERENCES usuario (id),
    contenido         TEXT        NOT NULL,
    fecha_publicacion TIMESTAMPTZ NOT NULL,
    vetado            BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_replica_contenido_no_vacio CHECK (length(btrim(contenido)) > 0),
    CONSTRAINT ck_replica_padre_distinto CHECK (replica_padre_id IS NULL OR replica_padre_id <> id)
);

CREATE INDEX ix_replica_nivel ON replica (kudo_id, replica_padre_id, fecha_publicacion, id);
CREATE INDEX ix_replica_padre ON replica (replica_padre_id);

CREATE TABLE calificacion (
    usuario_id     UUID        NOT NULL REFERENCES usuario (id),
    tipo_contenido VARCHAR(10) NOT NULL,
    contenido_id   UUID        NOT NULL,
    valor          SMALLINT    NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_calificacion PRIMARY KEY (usuario_id, contenido_id, tipo_contenido),
    CONSTRAINT ck_calificacion_tipo CHECK (tipo_contenido IN ('KUDO', 'REPLICA')),
    CONSTRAINT ck_calificacion_valor CHECK (valor BETWEEN 1 AND 5)
);

CREATE INDEX ix_calificacion_contenido ON calificacion (tipo_contenido, contenido_id);

CREATE TABLE reporte (
    id             UUID        PRIMARY KEY,
    tipo_contenido VARCHAR(10) NOT NULL,
    contenido_id   UUID        NOT NULL,
    reportante_id  UUID        NOT NULL REFERENCES usuario (id),
    motivo         TEXT        NOT NULL,
    estado         VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    creado_en      TIMESTAMPTZ NOT NULL,
    atendido_en    TIMESTAMPTZ,
    CONSTRAINT uk_reporte_unico UNIQUE (reportante_id, contenido_id, tipo_contenido),
    CONSTRAINT ck_reporte_tipo CHECK (tipo_contenido IN ('KUDO', 'REPLICA')),
    CONSTRAINT ck_reporte_estado CHECK (estado IN ('PENDIENTE', 'ATENDIDO'))
);

CREATE INDEX ix_reporte_pendiente ON reporte (creado_en DESC) WHERE estado = 'PENDIENTE';

CREATE TABLE veto (
    id             UUID        PRIMARY KEY,
    tipo_contenido VARCHAR(10) NOT NULL,
    contenido_id   UUID        NOT NULL,
    moderador_id   UUID        NOT NULL REFERENCES usuario (id),
    motivo         TEXT        NOT NULL,
    creado_en      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_veto_contenido UNIQUE (contenido_id, tipo_contenido),
    CONSTRAINT ck_veto_tipo CHECK (tipo_contenido IN ('KUDO', 'REPLICA'))
);
