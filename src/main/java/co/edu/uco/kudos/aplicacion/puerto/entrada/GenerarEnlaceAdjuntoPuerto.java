package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.aplicacion.dto.EnlaceAdjunto;

import java.util.UUID;

public interface GenerarEnlaceAdjuntoPuerto {

    EnlaceAdjunto generar(UUID kudoId, UUID adjuntoId, UUID usuarioId);
}
