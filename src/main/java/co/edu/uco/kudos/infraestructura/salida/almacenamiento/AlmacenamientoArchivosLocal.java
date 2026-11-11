package co.edu.uco.kudos.infraestructura.salida.almacenamiento;

import co.edu.uco.kudos.aplicacion.puerto.salida.AlmacenamientoArchivos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class AlmacenamientoArchivosLocal implements AlmacenamientoArchivos {

    private final Path directorioBase;

    public AlmacenamientoArchivosLocal(Path directorioBase) {
        this.directorioBase = Objects.requireNonNull(directorioBase).toAbsolutePath().normalize();
    }

    @Override
    public void guardar(String clave, String tipoContenido, byte[] contenido) {
        Path destino = resolver(clave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        } catch (IOException error) {
            throw new UncheckedIOException("No fue posible guardar el archivo " + clave, error);
        }
    }

    @Override
    public void eliminar(String clave) {
        try {
            Files.deleteIfExists(resolver(clave));
        } catch (IOException error) {
            throw new UncheckedIOException("No fue posible eliminar el archivo " + clave, error);
        }
    }

    private Path resolver(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La clave del archivo es obligatoria");
        }
        Path destino = directorioBase.resolve(clave).normalize();
        if (!destino.startsWith(directorioBase) || destino.equals(directorioBase)) {
            throw new IllegalArgumentException("Clave de archivo no permitida: " + clave);
        }
        return destino;
    }
}
