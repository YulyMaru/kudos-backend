CREATE TABLE usuario (
    id                 UUID         PRIMARY KEY,
    nombre_completo    VARCHAR(200) NOT NULL,
    correo             VARCHAR(254) NOT NULL,
    celular            VARCHAR(20),
    celular_verificado BOOLEAN      NOT NULL DEFAULT FALSE,
    rol                VARCHAR(30)  NOT NULL,
    estado             VARCHAR(20)  NOT NULL,
    creado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO',
                                             'COORDINADOR_FACULTAD', 'COORDINADOR_PROGRAMA', 'DOCENTE', 'ESTUDIANTE')),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('PENDIENTE', 'ACTIVO', 'BLOQUEADO', 'INACTIVO'))
);

CREATE UNIQUE INDEX uk_usuario_correo ON usuario (lower(correo));
CREATE UNIQUE INDEX uk_usuario_celular ON usuario (celular) WHERE celular IS NOT NULL;
CREATE UNIQUE INDEX uk_usuario_rector_activo ON usuario (rol) WHERE rol = 'RECTOR' AND estado = 'ACTIVO';
CREATE UNIQUE INDEX uk_usuario_coordinador_academico_activo ON usuario (rol)
    WHERE rol = 'COORDINADOR_ACADEMICO' AND estado = 'ACTIVO';

CREATE TABLE categoria (
    id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre    VARCHAR(100) NOT NULL,
    activa    BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_categoria_nombre ON categoria (lower(nombre));

CREATE TABLE kudo (
    id                UUID        PRIMARY KEY,
    autor_id          UUID        NOT NULL REFERENCES usuario (id),
    destinatario_id   UUID        NOT NULL REFERENCES usuario (id),
    categoria_id      UUID        NOT NULL REFERENCES categoria (id),
    contenido         TEXT        NOT NULL,
    fecha_publicacion TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_kudo_no_auto_reconocimiento CHECK (autor_id <> destinatario_id),
    CONSTRAINT ck_kudo_contenido_no_vacio CHECK (length(btrim(contenido)) > 0)
);

CREATE INDEX ix_kudo_feed ON kudo (fecha_publicacion DESC, id DESC);
CREATE INDEX ix_kudo_destinatario ON kudo (destinatario_id, fecha_publicacion DESC);
CREATE INDEX ix_kudo_autor ON kudo (autor_id, fecha_publicacion DESC);

CREATE TABLE adjunto (
    id              UUID         PRIMARY KEY,
    kudo_id         UUID         NOT NULL REFERENCES kudo (id) ON DELETE CASCADE,
    nombre_original VARCHAR(255) NOT NULL,
    tipo_contenido  VARCHAR(100) NOT NULL,
    tamano_bytes    BIGINT       NOT NULL,
    hash_sha256     VARCHAR(64)  NOT NULL,
    clave_objeto    VARCHAR(500) NOT NULL,
    CONSTRAINT uk_adjunto_clave_objeto UNIQUE (clave_objeto),
    CONSTRAINT ck_adjunto_tamano CHECK (tamano_bytes > 0),
    CONSTRAINT ck_adjunto_hash CHECK (char_length(hash_sha256) = 64)
);

CREATE INDEX ix_adjunto_kudo ON adjunto (kudo_id);

CREATE TABLE outbox_notificacion (
    id              UUID        PRIMARY KEY,
    id_evento       UUID        NOT NULL,
    id_correlacion  UUID        NOT NULL,
    canal           VARCHAR(20) NOT NULL,
    destinatario    VARCHAR(254),
    payload         JSONB       NOT NULL,
    estado          VARCHAR(20) NOT NULL,
    intentos        INTEGER     NOT NULL DEFAULT 0,
    proximo_intento TIMESTAMPTZ,
    ultimo_error    TEXT,
    creado_en       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_outbox_evento_canal UNIQUE (id_evento, canal),
    CONSTRAINT ck_outbox_canal CHECK (canal IN ('EMAIL', 'WHATSAPP', 'PUSH')),
    CONSTRAINT ck_outbox_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'REINTENTANDO', 'FALLIDO', 'NO_APLICA')),
    CONSTRAINT ck_outbox_intentos CHECK (intentos >= 0)
);

CREATE INDEX ix_outbox_por_procesar ON outbox_notificacion (proximo_intento)
    WHERE estado IN ('PENDIENTE', 'REINTENTANDO');
CREATE INDEX ix_outbox_correlacion ON outbox_notificacion (id_correlacion);

CREATE TABLE registro_auditoria (
    id             UUID         PRIMARY KEY,
    fecha_hora     TIMESTAMPTZ  NOT NULL,
    id_correlacion UUID,
    actor_id       UUID,
    rol_actor      VARCHAR(30),
    accion         VARCHAR(40)  NOT NULL,
    recurso_tipo   VARCHAR(40),
    recurso_id     UUID,
    canal          VARCHAR(20),
    resultado      VARCHAR(20)  NOT NULL,
    detalle        TEXT,
    ip_origen      VARCHAR(45),
    trace_id       VARCHAR(64),
    CONSTRAINT ck_auditoria_resultado CHECK (resultado IN ('EXITOSO', 'FALLIDO', 'NO_APLICA'))
);

CREATE INDEX ix_auditoria_correlacion ON registro_auditoria (id_correlacion);
CREATE INDEX ix_auditoria_fecha ON registro_auditoria (fecha_hora DESC);
CREATE INDEX ix_auditoria_actor ON registro_auditoria (actor_id, fecha_hora DESC);

CREATE FUNCTION fn_auditoria_solo_insercion() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'registro_auditoria es de solo insercion: operacion % no permitida', TG_OP;
END;
$$;

CREATE TRIGGER tr_auditoria_sin_modificaciones
    BEFORE UPDATE OR DELETE ON registro_auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria_solo_insercion();

CREATE TRIGGER tr_auditoria_sin_truncate
    BEFORE TRUNCATE ON registro_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION fn_auditoria_solo_insercion();
