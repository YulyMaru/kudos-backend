package co.edu.uco.kudos.dominio.evento;

import java.time.Instant;
import java.util.UUID;

public record KudoPublicado(UUID idEvento, UUID idKudo, UUID autorId, UUID destinatarioId,
                            UUID categoriaId, Instant ocurridoEn) implements EventoDominio {

    @Override
    public UUID idCorrelacion() {
        return idKudo;
    }
}
