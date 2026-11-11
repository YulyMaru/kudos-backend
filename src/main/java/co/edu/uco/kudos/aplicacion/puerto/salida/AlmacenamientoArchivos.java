package co.edu.uco.kudos.aplicacion.puerto.salida;

public interface AlmacenamientoArchivos {

    void guardar(String clave, String tipoContenido, byte[] contenido);

    void eliminar(String clave);
}
