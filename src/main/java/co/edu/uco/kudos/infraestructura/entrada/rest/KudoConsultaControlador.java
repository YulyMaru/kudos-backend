package co.edu.uco.kudos.infraestructura.entrada.rest;

import co.edu.uco.kudos.aplicacion.dto.KudoDetalleVista;
import co.edu.uco.kudos.aplicacion.dto.PaginaFeed;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarFeedPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarKudoPuerto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kudos")
public class KudoConsultaControlador {

    private final ConsultarFeedPuerto consultarFeed;
    private final ConsultarKudoPuerto consultarKudo;

    public KudoConsultaControlador(ConsultarFeedPuerto consultarFeed, ConsultarKudoPuerto consultarKudo) {
        this.consultarFeed = consultarFeed;
        this.consultarKudo = consultarKudo;
    }

    @GetMapping
    public PaginaFeed feed(@RequestParam(name = "cursor", required = false) String cursor,
                           @RequestParam(name = "limite", required = false) Integer limite) {
        return consultarFeed.consultar(cursor, limite);
    }

    @GetMapping("/{id}")
    public KudoDetalleVista detalle(@PathVariable("id") UUID id) {
        return consultarKudo.consultar(id);
    }
}
