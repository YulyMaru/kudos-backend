package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;

public interface VerificarContactoPuerto {

    void verificar(String correo, PropositoCodigo proposito, String codigo);

    void reenviar(String correo, PropositoCodigo proposito);
}
