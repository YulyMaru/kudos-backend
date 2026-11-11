package co.edu.uco.kudos.aplicacion.dto;

import java.util.List;
import java.util.UUID;

public record PublicarKudoComando(UUID autorId, UUID destinatarioId, UUID categoriaId, String contenido,
                                  List<ArchivoAdjuntoComando> adjuntos) {

    public PublicarKudoComando {
        adjuntos = adjuntos == null ? List.of() : List.copyOf(adjuntos);
    }
}
