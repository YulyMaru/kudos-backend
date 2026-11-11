package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.dominio.modelo.Programa;

import java.util.List;
import java.util.UUID;

public interface RepositorioVinculos {

    void vincular(UUID usuarioId, UUID programaId);

    boolean desvincular(UUID usuarioId, UUID programaId);

    List<Programa> programasDe(UUID usuarioId);

    List<PersonaVista> miembros(UUID programaId);
}
