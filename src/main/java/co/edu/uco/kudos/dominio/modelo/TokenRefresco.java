package co.edu.uco.kudos.dominio.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TokenRefresco(UUID id, UUID usuarioId, String hashToken, Instant creadoEn, Instant expiraEn,
                            Instant revocadoEn, UUID reemplazadoPor) {

    public TokenRefresco {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(hashToken, "hashToken");
        Objects.requireNonNull(creadoEn, "creadoEn");
        Objects.requireNonNull(expiraEn, "expiraEn");
    }

    public static TokenRefresco emitir(UUID usuarioId, String hashToken, Instant ahora, Duration vigencia) {
        return new TokenRefresco(UUID.randomUUID(), usuarioId, hashToken, ahora, ahora.plus(vigencia), null, null);
    }

    public boolean estaRevocado() {
        return revocadoEn != null;
    }

    public boolean estaExpirado(Instant ahora) {
        return !ahora.isBefore(expiraEn);
    }

    @Override
    public String toString() {
        return "TokenRefresco[id=" + id + ", usuarioId=" + usuarioId + ", expiraEn=" + expiraEn
                + ", revocado=" + estaRevocado() + "]";
    }
}
