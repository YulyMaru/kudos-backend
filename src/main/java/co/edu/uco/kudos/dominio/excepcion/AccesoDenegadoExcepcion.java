package co.edu.uco.kudos.dominio.excepcion;

public class AccesoDenegadoExcepcion extends RuntimeException {

    public AccesoDenegadoExcepcion(String mensaje) {
        super(mensaje);
    }
}
