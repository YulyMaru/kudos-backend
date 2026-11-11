package co.edu.uco.kudos.aplicacion.puerto.entrada;

import co.edu.uco.kudos.aplicacion.dto.PersonaVista;

import java.util.List;
import java.util.UUID;

public interface BuscarUsuariosPuerto {

    List<PersonaVista> buscar(String texto, Integer limite, UUID solicitanteId);
}
