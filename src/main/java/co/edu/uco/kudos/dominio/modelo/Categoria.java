package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;

import java.util.Objects;
import java.util.UUID;

public final class Categoria {

    public static final int LONGITUD_MAXIMA_NOMBRE = 100;

    private final UUID id;
    private final String nombre;
    private final boolean activa;

    public Categoria(UUID id, String nombre, boolean activa) {
        this.id = Objects.requireNonNull(id, "id");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.activa = activa;
    }

    public static Categoria crear(String nombre) {
        return new Categoria(UUID.randomUUID(), validarNombre(nombre), true);
    }

    public Categoria renombrar(String nuevoNombre) {
        return new Categoria(id, validarNombre(nuevoNombre), activa);
    }

    public Categoria conEstado(boolean nuevaActiva) {
        return new Categoria(id, nombre, nuevaActiva);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new CampoObligatorioExcepcion("nombre");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new ReglaNegocioExcepcion("NOMBRE_DEMASIADO_LARGO", "nombre",
                    "El nombre de la categoría no puede superar " + LONGITUD_MAXIMA_NOMBRE + " caracteres");
        }
        return limpio;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActiva() {
        return activa;
    }
}
