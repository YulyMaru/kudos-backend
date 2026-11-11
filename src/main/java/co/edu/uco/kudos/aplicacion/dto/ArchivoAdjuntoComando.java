package co.edu.uco.kudos.aplicacion.dto;

import java.util.Arrays;
import java.util.Objects;

public record ArchivoAdjuntoComando(String nombreOriginal, byte[] contenido) {

    public ArchivoAdjuntoComando {
        contenido = contenido == null ? new byte[0] : contenido.clone();
    }

    @Override
    public byte[] contenido() {
        return contenido.clone();
    }

    public long tamanoBytes() {
        return contenido.length;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof ArchivoAdjuntoComando(String nombre, byte[] bytes)
                && Objects.equals(nombreOriginal, nombre)
                && Arrays.equals(contenido, bytes);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(nombreOriginal) + Arrays.hashCode(contenido);
    }

    @Override
    public String toString() {
        return "ArchivoAdjuntoComando[nombreOriginal=" + nombreOriginal + ", tamanoBytes=" + contenido.length + "]";
    }
}
