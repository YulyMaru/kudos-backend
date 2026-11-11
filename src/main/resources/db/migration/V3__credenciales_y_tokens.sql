ALTER TABLE usuario
    ADD COLUMN hash_contrasena   VARCHAR(100),
    ADD COLUMN intentos_fallidos INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_hasta   TIMESTAMPTZ,
    ADD CONSTRAINT ck_usuario_intentos_fallidos CHECK (intentos_fallidos >= 0);

CREATE TABLE token_refresco (
    id              UUID        PRIMARY KEY,
    usuario_id      UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    hash_token      VARCHAR(64) NOT NULL,
    creado_en       TIMESTAMPTZ NOT NULL,
    expira_en       TIMESTAMPTZ NOT NULL,
    revocado_en     TIMESTAMPTZ,
    reemplazado_por UUID,
    CONSTRAINT uk_token_refresco_hash UNIQUE (hash_token),
    CONSTRAINT ck_token_refresco_vigencia CHECK (expira_en > creado_en)
);

CREATE INDEX ix_token_refresco_usuario_vigentes ON token_refresco (usuario_id) WHERE revocado_en IS NULL;
