package co.edu.uco.kudos.aplicacion.puerto.salida;

import java.util.Optional;

public interface DetectorTipoArchivo {

    Optional<String> detectar(byte[] contenido);
}
