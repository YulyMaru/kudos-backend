package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;

import java.util.Objects;
import java.util.UUID;

public record Facultad(UUID id, String nombre, boolean activa) {

    public Facultad {
        Objects.requireNonNull(id, "id");
        if (nombre == null || nombre.isBlank()) {
            throw new CampoObligatorioExcepcion("nombre");
        }
        nombre = nombre.trim();
    }

    public static Facultad crear(String nombre) {
        return new Facultad(UUID.randomUUID(), nombre, true);
    }
}
