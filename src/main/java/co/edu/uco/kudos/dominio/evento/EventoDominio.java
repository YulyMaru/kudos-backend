package co.edu.uco.kudos.dominio.evento;

import java.time.Instant;
import java.util.UUID;

public sealed interface EventoDominio permits KudoPublicado {

    UUID idEvento();

    UUID idCorrelacion();

    Instant ocurridoEn();
}
