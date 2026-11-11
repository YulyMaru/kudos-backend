package co.edu.uco.kudos.aplicacion.dto;

import java.time.Instant;

public record TokenAcceso(String valor, Instant expiraEn) {

    @Override
    public String toString() {
        return "TokenAcceso[expiraEn=" + expiraEn + "]";
    }
}
