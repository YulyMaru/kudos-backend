package co.edu.uco.kudos.infraestructura.salida.almacenamiento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlmacenamientoArchivosLocalTest {

    @TempDir
    Path directorio;

    @Test
    void guardaYEliminaUnArchivoDentroDelDirectorioBase() throws IOException {
        AlmacenamientoArchivosLocal almacenamiento = new AlmacenamientoArchivosLocal(directorio);
        byte[] contenido = {1, 2, 3};

        almacenamiento.guardar("kudos/123/archivo.png", "image/png", contenido);

        Path archivo = directorio.resolve("kudos/123/archivo.png");
        assertThat(Files.readAllBytes(archivo)).isEqualTo(contenido);

        almacenamiento.eliminar("kudos/123/archivo.png");

        assertThat(archivo).doesNotExist();
    }

    @Test
    void eliminarUnArchivoInexistenteNoFalla() {
        AlmacenamientoArchivosLocal almacenamiento = new AlmacenamientoArchivosLocal(directorio);

        almacenamiento.eliminar("kudos/no-existe.png");

        assertThat(directorio.resolve("kudos/no-existe.png")).doesNotExist();
    }

    @Test
    void rechazaClavesQueIntentanSalirDelDirectorioBase() {
        AlmacenamientoArchivosLocal almacenamiento = new AlmacenamientoArchivosLocal(directorio);

        assertThatThrownBy(() -> almacenamiento.guardar("../fuera.png", "image/png", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> almacenamiento.guardar("", "image/png", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(directorio.getParent().resolve("fuera.png")).doesNotExist();
    }
}
