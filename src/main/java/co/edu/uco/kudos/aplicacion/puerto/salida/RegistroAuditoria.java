package co.edu.uco.kudos.aplicacion.puerto.salida;

import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;

public interface RegistroAuditoria {

    void registrar(EntradaAuditoria entrada);
}
