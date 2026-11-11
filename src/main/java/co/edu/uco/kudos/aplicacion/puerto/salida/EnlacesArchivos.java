package co.edu.uco.kudos.aplicacion.puerto.salida;

import java.time.Duration;
import java.util.Optional;

public interface EnlacesArchivos {

    Optional<String> generarEnlaceTemporal(String clave, Duration vigencia);
}
