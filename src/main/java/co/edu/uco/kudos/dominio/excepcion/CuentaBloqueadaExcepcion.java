package co.edu.uco.kudos.dominio.excepcion;

import java.time.Instant;

public class CuentaBloqueadaExcepcion extends RuntimeException {

    private final Instant bloqueadaHasta;

    public CuentaBloqueadaExcepcion(Instant bloqueadaHasta) {
        super("La cuenta está bloqueada temporalmente por intentos fallidos. Intenta de nuevo más tarde");
        this.bloqueadaHasta = bloqueadaHasta;
    }

    public Instant getBloqueadaHasta() {
        return bloqueadaHasta;
    }
}
