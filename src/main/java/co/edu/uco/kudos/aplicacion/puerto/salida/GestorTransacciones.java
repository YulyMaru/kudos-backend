package co.edu.uco.kudos.aplicacion.puerto.salida;

public interface GestorTransacciones {

    void enTransaccion(Runnable operacion);
}
