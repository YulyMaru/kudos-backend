package co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador;

import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.RegistroAuditoriaEntidad;

public final class AuditoriaMapeador {

    private AuditoriaMapeador() {
    }

    public static RegistroAuditoriaEntidad aEntidad(EntradaAuditoria entrada) {
        return new RegistroAuditoriaEntidad(entrada.id(), entrada.fechaHora(), entrada.idCorrelacion(),
                entrada.actorId(), entrada.rolActor(), entrada.accion(), entrada.recursoTipo(), entrada.recursoId(),
                entrada.canal(), entrada.resultado(), entrada.detalle());
    }

    public static EntradaAuditoria aDominio(RegistroAuditoriaEntidad entidad) {
        return new EntradaAuditoria(entidad.getId(), entidad.getFechaHora(), entidad.getIdCorrelacion(),
                entidad.getActorId(), entidad.getRolActor(), entidad.getAccion(), entidad.getRecursoTipo(),
                entidad.getRecursoId(), entidad.getCanal(), entidad.getResultado(), entidad.getDetalle());
    }
}
