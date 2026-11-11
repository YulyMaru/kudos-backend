package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.dominio.modelo.Programa;

import java.util.List;
import java.util.UUID;

public interface GestionarVinculosPuerto {

    void vincular(UUID actorId, UUID programaId, UUID usuarioId);

    void desvincular(UUID actorId, UUID programaId, UUID usuarioId);

    List<PersonaVista> miembros(UUID actorId, UUID programaId);

    List<Programa> programasDe(UUID usuarioId);
}
