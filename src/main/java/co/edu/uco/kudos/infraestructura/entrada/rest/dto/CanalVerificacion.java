package co.edu.uco.kudos.infraestructura.entrada.rest.dto;


import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;

public enum CanalVerificacion {
    CORREO(PropositoCodigo.VERIFICAR_CORREO),
    CELULAR(PropositoCodigo.VERIFICAR_CELULAR);

    private final PropositoCodigo proposito;

    CanalVerificacion(PropositoCodigo proposito) {
        this.proposito = proposito;
    }

    public PropositoCodigo proposito() {
        return proposito;
    }
}
