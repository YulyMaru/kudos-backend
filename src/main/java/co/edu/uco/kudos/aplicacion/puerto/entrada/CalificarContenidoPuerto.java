package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.aplicacion.dto.CalificarComando;
import co.edu.uco.kudos.aplicacion.dto.ResumenCalificacion;

public interface CalificarContenidoPuerto {

    ResumenCalificacion calificar(CalificarComando comando);
}
