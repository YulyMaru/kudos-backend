package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.ReplicaVista;
import co.edu.uco.kudos.aplicacion.dto.ReporteVista;

import java.util.List;
import java.util.UUID;

public interface ConsultasConversacion {

    List<ReplicaVista> replicas(UUID kudoId, UUID replicaPadreId, CursorFeed despuesDe, int limite);

    List<ReporteVista> reportesPendientes(int limite);
}
