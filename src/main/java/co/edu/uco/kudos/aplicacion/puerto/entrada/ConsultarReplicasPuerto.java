package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.aplicacion.dto.PaginaReplicas;

import java.util.UUID;

public interface ConsultarReplicasPuerto {

    PaginaReplicas consultar(UUID kudoId, UUID replicaPadreId, String cursor, Integer limite);
}
