package co.edu.uco.kudos.infraestructura.salida.almacenamiento;

import co.edu.uco.kudos.aplicacion.puerto.salida.EnlacesArchivos;

import java.time.Duration;
import java.util.Optional;

public class EnlacesArchivosLocales implements EnlacesArchivos {

    @Override
    public Optional<String> generarEnlaceTemporal(String clave, Duration vigencia) {
        return Optional.empty();
    }
}
