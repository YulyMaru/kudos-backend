package co.edu.uco.kudos.aplicacion.dto;

import java.util.List;

public record PaginaFeed(List<KudoResumenVista> kudos, String siguienteCursor) {

    public PaginaFeed {
        kudos = List.copyOf(kudos);
    }
}
