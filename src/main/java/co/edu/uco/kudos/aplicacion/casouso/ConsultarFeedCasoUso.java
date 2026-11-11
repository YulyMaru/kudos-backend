package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.KudoResumenVista;
import co.edu.uco.kudos.aplicacion.dto.PaginaFeed;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarFeedPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasKudos;
import co.edu.uco.kudos.dominio.excepcion.ParametroInvalidoExcepcion;

import java.util.List;
import java.util.Objects;

public final class ConsultarFeedCasoUso implements ConsultarFeedPuerto {

    public static final int LIMITE_POR_DEFECTO = 20;
    public static final int LIMITE_MAXIMO = 50;

    private final ConsultasKudos consultas;

    public ConsultarFeedCasoUso(ConsultasKudos consultas) {
        this.consultas = Objects.requireNonNull(consultas);
    }

    @Override
    public PaginaFeed consultar(String cursor, Integer limite) {
        int tamano = limite == null ? LIMITE_POR_DEFECTO : limite;
        if (tamano < 1 || tamano > LIMITE_MAXIMO) {
            throw new ParametroInvalidoExcepcion("limite", "El límite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
        CursorFeed despuesDe = cursor == null || cursor.isBlank() ? null : CursorFeed.decodificar(cursor);
        List<KudoResumenVista> encontrados = consultas.feed(despuesDe, tamano + 1);
        if (encontrados.size() <= tamano) {
            return new PaginaFeed(encontrados, null);
        }
        List<KudoResumenVista> pagina = encontrados.subList(0, tamano);
        return new PaginaFeed(pagina, CursorFeed.de(pagina.getLast()).codificar());
    }
}
