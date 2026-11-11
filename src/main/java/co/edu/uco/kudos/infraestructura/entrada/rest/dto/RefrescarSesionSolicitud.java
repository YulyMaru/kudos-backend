package co.edu.uco.kudos.infraestructura.entrada.rest.dto;

public record RefrescarSesionSolicitud(String tokenRefresco) {

    @Override
    public String toString() {
        return "RefrescarSesionSolicitud[tokenRefresco=****]";
    }
}
