package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.dominio.modelo.CodigoVerificacion;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioCodigos {

    void guardar(CodigoVerificacion codigo);

    void actualizar(CodigoVerificacion codigo);

    Optional<CodigoVerificacion> ultimo(UUID usuarioId, PropositoCodigo proposito);

    Optional<CodigoVerificacion> buscarPorHash(PropositoCodigo proposito, String hashCodigo);

    int contarEmitidosDesde(UUID usuarioId, PropositoCodigo proposito, Instant desde);

    void invalidarVigentes(UUID usuarioId, PropositoCodigo proposito, Instant fecha);
}
