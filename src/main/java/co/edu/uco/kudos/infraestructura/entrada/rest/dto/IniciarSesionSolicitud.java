package co.edu.uco.kudos.infraestructura.entrada.rest.dto;

public record IniciarSesionSolicitud(String correo, String contrasena) {

    @Override
    public String toString() {
        return "IniciarSesionSolicitud[correo=" + correo + "]";
    }
}
