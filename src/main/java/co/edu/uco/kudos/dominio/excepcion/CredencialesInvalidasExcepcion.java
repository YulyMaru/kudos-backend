package co.edu.uco.kudos.dominio.excepcion;

public class CredencialesInvalidasExcepcion extends RuntimeException {

    public CredencialesInvalidasExcepcion() {
        super("Correo o contraseña incorrectos");
    }
}
