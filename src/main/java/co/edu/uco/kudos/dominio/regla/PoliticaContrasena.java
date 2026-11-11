package co.edu.uco.kudos.dominio.regla;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;

import java.nio.charset.StandardCharsets;

public final class PoliticaContrasena {

    public static final int LONGITUD_MINIMA = 12;
    public static final int BYTES_MAXIMOS = 72;

    private PoliticaContrasena() {
    }

    public static void validar(String contrasena) {
        if (contrasena == null || contrasena.isEmpty()) {
            throw new CampoObligatorioExcepcion("contrasena");
        }
        if (contrasena.length() < LONGITUD_MINIMA) {
            throw new ReglaNegocioExcepcion("CONTRASENA_DEBIL", "contrasena",
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
            throw new ReglaNegocioExcepcion("CONTRASENA_DEMASIADO_LARGA", "contrasena",
                    "La contraseña no puede superar " + BYTES_MAXIMOS + " bytes");
        }
        if (contrasena.isBlank()) {
            throw new ReglaNegocioExcepcion("CONTRASENA_DEBIL", "contrasena",
                    "La contraseña no puede estar formada solo por espacios");
        }
    }
}
