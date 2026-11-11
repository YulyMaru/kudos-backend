package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.aplicacion.dto.FiltroAuditoria;
import co.edu.uco.kudos.aplicacion.dto.PaginaAuditoria;

public interface ConsultasAuditoria {

    PaginaAuditoria buscar(FiltroAuditoria filtro, int pagina, int tamano);
}
