package co.edu.uco.kudos.infraestructura.entrada.rest.dto;


public record VerificarSolicitud(String correo, CanalVerificacion canal, String codigo) {

    @Override
    public String toString() {
        return "VerificarSolicitud[correo=" + correo + ", canal=" + canal + "]";
    }
}
