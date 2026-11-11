package co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador;

import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.CategoriaEntidad;

public final class CategoriaMapeador {

    private CategoriaMapeador() {
    }

    public static Categoria aDominio(CategoriaEntidad entidad) {
        return new Categoria(entidad.getId(), entidad.getNombre(), entidad.isActiva());
    }
}
