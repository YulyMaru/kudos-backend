package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.BuscarUsuariosPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasUsuarios;
import co.edu.uco.kudos.dominio.excepcion.ParametroInvalidoExcepcion;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class BuscarUsuariosCasoUso implements BuscarUsuariosPuerto {

    public static final int LONGITUD_MINIMA_BUSQUEDA = 2;
    public static final int LONGITUD_MAXIMA_BUSQUEDA = 100;
    public static final int LIMITE_POR_DEFECTO = 10;
    public static final int LIMITE_MAXIMO = 20;

    private final ConsultasUsuarios consultas;

    public BuscarUsuariosCasoUso(ConsultasUsuarios consultas) {
        this.consultas = Objects.requireNonNull(consultas);
    }

    @Override
    public List<PersonaVista> buscar(String texto, Integer limite, UUID solicitanteId) {
        String busqueda = texto == null ? "" : texto.trim();
        if (busqueda.length() < LONGITUD_MINIMA_BUSQUEDA || busqueda.length() > LONGITUD_MAXIMA_BUSQUEDA) {
            throw new ParametroInvalidoExcepcion("busqueda", "La búsqueda debe tener entre "
                    + LONGITUD_MINIMA_BUSQUEDA + " y " + LONGITUD_MAXIMA_BUSQUEDA + " caracteres");
        }
        int tamano = limite == null ? LIMITE_POR_DEFECTO : limite;
        if (tamano < 1 || tamano > LIMITE_MAXIMO) {
            throw new ParametroInvalidoExcepcion("limite", "El límite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
        return consultas.buscarActivos(busqueda, tamano, solicitanteId);
    }
}
