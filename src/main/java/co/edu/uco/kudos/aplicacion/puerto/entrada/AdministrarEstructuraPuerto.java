package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Programa;

import java.util.List;
import java.util.UUID;

public interface AdministrarEstructuraPuerto {

    Facultad crearFacultad(UUID actorId, String nombre);

    Programa crearPrograma(UUID actorId, UUID facultadId, String nombre);

    List<Facultad> facultades();

    List<Programa> programas(UUID facultadId);
}
