package co.edu.uco.kudos.aplicacion.puerto.entrada;


import co.edu.uco.kudos.aplicacion.dto.FiltroAuditoria;
import co.edu.uco.kudos.aplicacion.dto.PaginaAuditoria;

public interface ConsultarAuditoriaPuerto {

    PaginaAuditoria consultar(FiltroAuditoria filtro, Integer pagina, Integer tamano);
}
