package co.edu.uco.kudos.dominio.evento;

import java.util.UUID;

public record KudoPublicado(UUID idEvento, UUID idKudo, UUID autorId, UUID destinatarioId,
                            UUID categoriaId) implements EventoDominio {

    @Override
    public UUID idCorrelacion() {
        return idKudo;
    }
}
