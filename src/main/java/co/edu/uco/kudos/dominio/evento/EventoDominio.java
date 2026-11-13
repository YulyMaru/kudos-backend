package co.edu.uco.kudos.dominio.evento;

import java.util.UUID;

public sealed interface EventoDominio permits KudoPublicado {

    UUID idEvento();

    UUID idCorrelacion();
}
