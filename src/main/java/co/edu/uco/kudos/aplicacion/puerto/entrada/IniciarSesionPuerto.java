package co.edu.uco.kudos.aplicacion.puerto.entrada;

import co.edu.uco.kudos.aplicacion.dto.IniciarSesionComando;
import co.edu.uco.kudos.aplicacion.dto.SesionResultado;

public interface IniciarSesionPuerto {

    SesionResultado iniciarSesion(IniciarSesionComando comando);
}
