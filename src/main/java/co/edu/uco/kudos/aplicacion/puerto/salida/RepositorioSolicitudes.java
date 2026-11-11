package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.aplicacion.dto.SolicitudPendienteVista;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioSolicitudes {

    void guardar(SolicitudRegistro solicitud);

    void actualizar(SolicitudRegistro solicitud);

    Optional<SolicitudRegistro> buscarPorId(UUID id);

    Optional<SolicitudRegistro> buscarPorUsuario(UUID usuarioId);

    List<SolicitudPendienteVista> pendientes(List<Rol> rolesSolicitados, UUID programaId, UUID facultadId, int limite);
}
