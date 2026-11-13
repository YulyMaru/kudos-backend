ALTER TABLE outbox_notificacion DROP CONSTRAINT ck_outbox_canal;

UPDATE outbox_notificacion SET canal = 'SMS' WHERE canal = 'WHATSAPP';
UPDATE registro_auditoria SET canal = 'SMS' WHERE canal = 'WHATSAPP';

ALTER TABLE outbox_notificacion
    ADD CONSTRAINT ck_outbox_canal CHECK (canal IN ('EMAIL', 'SMS', 'PUSH'));
