package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.aplicacion.dto.AdjuntoDatos;
import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.KudoDetalleDatos;
import co.edu.uco.kudos.aplicacion.dto.KudoResumenVista;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultasKudos {

    List<KudoResumenVista> feed(CursorFeed despuesDe, int limite);

    Optional<KudoDetalleDatos> detalle(UUID id);

    Optional<AdjuntoDatos> adjunto(UUID kudoId, UUID adjuntoId);
}
