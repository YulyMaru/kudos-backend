package co.edu.uco.kudos.aplicacion.puerto.entrada;


public interface RestablecerContrasenaPuerto {

    void solicitar(String correo);

    void confirmar(String token, String nuevaContrasena);
}
