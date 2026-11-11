package co.edu.uco.kudos.dominio.excepcion;

public class ParametroInvalidoExcepcion extends ReglaNegocioExcepcion {

    public ParametroInvalidoExcepcion(String campo, String mensaje) {
        super("PARAMETRO_INVALIDO", campo, mensaje);
    }
}
