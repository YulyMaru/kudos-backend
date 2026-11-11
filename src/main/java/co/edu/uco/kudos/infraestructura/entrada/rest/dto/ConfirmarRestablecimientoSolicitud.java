package co.edu.uco.kudos.infraestructura.entrada.rest.dto;


public record ConfirmarRestablecimientoSolicitud(String token, String nuevaContrasena) {

    @Override
    public String toString() {
        return "ConfirmarRestablecimientoSolicitud[token=****]";
    }
}
