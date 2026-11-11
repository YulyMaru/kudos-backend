package co.edu.uco.kudos.aplicacion.dto;

public record IniciarSesionComando(String correo, String contrasena) {

    @Override
    public String toString() {
        return "IniciarSesionComando[correo=" + correo + "]";
    }
}
