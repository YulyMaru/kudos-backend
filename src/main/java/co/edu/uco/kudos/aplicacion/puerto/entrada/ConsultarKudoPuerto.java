package co.edu.uco.kudos.aplicacion.puerto.entrada;

import co.edu.uco.kudos.aplicacion.dto.KudoDetalleVista;

import java.util.UUID;

public interface ConsultarKudoPuerto {

    KudoDetalleVista consultar(UUID id);
}
