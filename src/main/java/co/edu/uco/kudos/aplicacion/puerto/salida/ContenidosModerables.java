package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.aplicacion.dto.EstadoContenido;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;

import java.util.Optional;
import java.util.UUID;

public interface ContenidosModerables {

    Optional<EstadoContenido> buscar(TipoContenido tipo, UUID id);

    void marcarVetado(TipoContenido tipo, UUID id);
}
