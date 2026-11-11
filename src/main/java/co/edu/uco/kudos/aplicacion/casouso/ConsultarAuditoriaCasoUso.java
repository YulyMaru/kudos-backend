package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.FiltroAuditoria;
import co.edu.uco.kudos.aplicacion.dto.PaginaAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarAuditoriaPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasAuditoria;
import co.edu.uco.kudos.dominio.excepcion.ParametroInvalidoExcepcion;

import java.util.Objects;

public final class ConsultarAuditoriaCasoUso implements ConsultarAuditoriaPuerto {

    public static final int TAMANO_POR_DEFECTO = 50;
    public static final int TAMANO_MAXIMO = 200;

    private final ConsultasAuditoria consultas;

    public ConsultarAuditoriaCasoUso(ConsultasAuditoria consultas) {
        this.consultas = Objects.requireNonNull(consultas);
    }

    @Override
    public PaginaAuditoria consultar(FiltroAuditoria filtro, Integer pagina, Integer tamano) {
        FiltroAuditoria criterios = filtro == null ? new FiltroAuditoria(null, null, null, null, null, null, null) : filtro;
        int numero = pagina == null ? 0 : pagina;
        int cantidad = tamano == null ? TAMANO_POR_DEFECTO : tamano;
        if (numero < 0) {
            throw new ParametroInvalidoExcepcion("pagina", "La página no puede ser negativa");
        }
        if (cantidad < 1 || cantidad > TAMANO_MAXIMO) {
            throw new ParametroInvalidoExcepcion("tamano", "El tamaño debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        if (criterios.desde() != null && criterios.hasta() != null && criterios.desde().isAfter(criterios.hasta())) {
            throw new ParametroInvalidoExcepcion("desde", "La fecha inicial no puede ser posterior a la final");
        }
        return consultas.buscar(criterios, numero, cantidad);
    }
}
