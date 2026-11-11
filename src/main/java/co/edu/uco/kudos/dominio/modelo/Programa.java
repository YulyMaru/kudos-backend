package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;

import java.util.Objects;
import java.util.UUID;

public record Programa(UUID id, UUID facultadId, String nombre, boolean activo) {

    public Programa {
        Objects.requireNonNull(id, "id");
        if (facultadId == null) {
            throw new CampoObligatorioExcepcion("facultadId");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new CampoObligatorioExcepcion("nombre");
        }
        nombre = nombre.trim();
    }

    public static Programa crear(UUID facultadId, String nombre) {
        return new Programa(UUID.randomUUID(), facultadId, nombre, true);
    }
}
