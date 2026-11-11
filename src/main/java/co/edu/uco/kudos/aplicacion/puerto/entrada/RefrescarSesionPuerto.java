package co.edu.uco.kudos.aplicacion.puerto.entrada;

import co.edu.uco.kudos.aplicacion.dto.SesionResultado;

public interface RefrescarSesionPuerto {

    SesionResultado refrescar(String tokenRefresco);
}
