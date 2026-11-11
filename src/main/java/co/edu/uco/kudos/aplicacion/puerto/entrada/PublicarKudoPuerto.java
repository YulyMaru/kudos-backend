package co.edu.uco.kudos.aplicacion.puerto.entrada;

import co.edu.uco.kudos.aplicacion.dto.KudoPublicadoResultado;
import co.edu.uco.kudos.aplicacion.dto.PublicarKudoComando;

public interface PublicarKudoPuerto {

    KudoPublicadoResultado publicar(PublicarKudoComando comando);
}
