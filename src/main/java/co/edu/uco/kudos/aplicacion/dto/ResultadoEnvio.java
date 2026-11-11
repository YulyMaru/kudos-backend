package co.edu.uco.kudos.aplicacion.dto;

public record ResultadoEnvio(boolean entregado, String motivo) {

    public static ResultadoEnvio exitoso() {
        return new ResultadoEnvio(true, null);
    }

    public static ResultadoEnvio noAplica(String motivo) {
        return new ResultadoEnvio(false, motivo);
    }
}
