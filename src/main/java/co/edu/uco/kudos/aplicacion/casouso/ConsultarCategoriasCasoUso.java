package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarCategoriasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasCategorias;

import java.util.List;
import java.util.Objects;

public final class ConsultarCategoriasCasoUso implements ConsultarCategoriasPuerto {

    private final ConsultasCategorias consultas;

    public ConsultarCategoriasCasoUso(ConsultasCategorias consultas) {
        this.consultas = Objects.requireNonNull(consultas);
    }

    @Override
    public List<CategoriaVista> consultarActivas() {
        return consultas.activas();
    }
}
