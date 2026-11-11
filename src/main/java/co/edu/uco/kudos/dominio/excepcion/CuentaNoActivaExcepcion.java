package co.edu.uco.kudos.dominio.excepcion;

import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;

import java.util.List;

public class CuentaNoActivaExcepcion extends RuntimeException {

    private final EstadoUsuario estado;
    private final List<String> condicionesPendientes;

    public CuentaNoActivaExcepcion(EstadoUsuario estado) {
        this(estado, List.of());
    }

    public CuentaNoActivaExcepcion(EstadoUsuario estado, List<String> condicionesPendientes) {
        super(condicionesPendientes.isEmpty()
                ? "La cuenta no está activa (estado " + estado + ")"
                : "La cuenta aún no está activa. Pendiente: " + String.join(", ", condicionesPendientes));
        this.estado = estado;
        this.condicionesPendientes = List.copyOf(condicionesPendientes);
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public List<String> getCondicionesPendientes() {
        return condicionesPendientes;
    }
}
