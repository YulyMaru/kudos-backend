package co.edu.uco.kudos.aplicacion.puerto.salida;

public interface CodificadorContrasenas {

    String codificar(String contrasena);

    boolean coincide(String contrasena, String hash);
}
