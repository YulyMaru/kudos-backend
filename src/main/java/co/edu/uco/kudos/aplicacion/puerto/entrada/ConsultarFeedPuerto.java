package co.edu.uco.kudos.aplicacion.puerto.entrada;

import co.edu.uco.kudos.aplicacion.dto.PaginaFeed;

public interface ConsultarFeedPuerto {

    PaginaFeed consultar(String cursor, Integer limite);
}
