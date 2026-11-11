package co.edu.uco.kudos.aplicacion.puerto.salida;

import co.edu.uco.kudos.aplicacion.dto.PersonaVista;

import java.util.List;
import java.util.UUID;

public interface ConsultasUsuarios {

    List<PersonaVista> buscarActivos(String texto, int limite, UUID excluir);
}
