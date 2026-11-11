package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.dto.FiltroAuditoria;
import co.edu.uco.kudos.aplicacion.dto.PaginaAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarAuditoriaPuerto;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaControlador {

    private final ConsultarAuditoriaPuerto consultarAuditoria;

    public AuditoriaControlador(ConsultarAuditoriaPuerto consultarAuditoria) {
        this.consultarAuditoria = consultarAuditoria;
    }

    @GetMapping
    public PaginaAuditoria consultar(@RequestParam(name = "desde", required = false) Instant desde,
                                     @RequestParam(name = "hasta", required = false) Instant hasta,
                                     @RequestParam(name = "actorId", required = false) UUID actorId,
                                     @RequestParam(name = "accion", required = false) AccionAuditoria accion,
                                     @RequestParam(name = "recursoTipo", required = false) String recursoTipo,
                                     @RequestParam(name = "recursoId", required = false) UUID recursoId,
                                     @RequestParam(name = "idCorrelacion", required = false) UUID idCorrelacion,
                                     @RequestParam(name = "pagina", required = false) Integer pagina,
                                     @RequestParam(name = "tamano", required = false) Integer tamano) {
        return consultarAuditoria.consultar(new FiltroAuditoria(desde, hasta, actorId, accion, recursoTipo, recursoId,
                idCorrelacion), pagina, tamano);
    }
}
