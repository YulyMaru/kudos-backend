package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.dto.CalificarComando;
import co.edu.uco.kudos.aplicacion.dto.ResumenCalificacion;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CalificarContenidoPuerto;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.CalificacionSolicitud;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calificaciones")
public class CalificacionControlador {

    private final CalificarContenidoPuerto calificar;

    public CalificacionControlador(CalificarContenidoPuerto calificar) {
        this.calificar = calificar;
    }

    @PutMapping
    public ResumenCalificacion calificar(@AuthenticationPrincipal Jwt jwt, @RequestBody CalificacionSolicitud solicitud) {
        return calificar.calificar(new CalificarComando(UsuarioAutenticado.id(jwt), solicitud.tipoContenido(),
                solicitud.contenidoId(), solicitud.valor()));
    }
}
