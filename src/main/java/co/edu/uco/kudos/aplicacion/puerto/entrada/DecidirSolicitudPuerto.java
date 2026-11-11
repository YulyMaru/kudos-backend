package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.aplicacion.dto.SolicitudPendienteVista;

import java.util.List;
import java.util.UUID;

public interface DecidirSolicitudPuerto {

    List<SolicitudPendienteVista> pendientes(UUID aprobadorId);

    void aprobar(UUID aprobadorId, UUID solicitudId);

    void rechazar(UUID aprobadorId, UUID solicitudId, String motivo);
}
