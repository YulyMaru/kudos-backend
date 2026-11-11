package co.edu.uco.kudos.dominio.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CodigoVerificacion(UUID id, UUID usuarioId, PropositoCodigo proposito, String hashCodigo,
                                 Instant creadoEn, Instant expiraEn, int intentos, Instant usadoEn) {

    public static final int MAXIMO_INTENTOS = 5;

    public CodigoVerificacion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(proposito, "proposito");
        Objects.requireNonNull(hashCodigo, "hashCodigo");
        Objects.requireNonNull(creadoEn, "creadoEn");
        Objects.requireNonNull(expiraEn, "expiraEn");
    }

    public static CodigoVerificacion emitir(UUID usuarioId, PropositoCodigo proposito, String hashCodigo, Instant ahora,
                                            Duration vigencia) {
        return new CodigoVerificacion(UUID.randomUUID(), usuarioId, proposito, hashCodigo, ahora, ahora.plus(vigencia),
                0, null);
    }

    public boolean estaVigente(Instant ahora) {
        return usadoEn == null && ahora.isBefore(expiraEn) && intentos < MAXIMO_INTENTOS;
    }

    public CodigoVerificacion conIntentoFallido() {
        return new CodigoVerificacion(id, usuarioId, proposito, hashCodigo, creadoEn, expiraEn, intentos + 1, usadoEn);
    }

    public CodigoVerificacion usado(Instant ahora) {
        return new CodigoVerificacion(id, usuarioId, proposito, hashCodigo, creadoEn, expiraEn, intentos, ahora);
    }

    @Override
    public String toString() {
        return "CodigoVerificacion[id=" + id + ", proposito=" + proposito + ", intentos=" + intentos + "]";
    }
}
