package co.edu.uco.kudos.infraestructura.entrada.rest;

import co.edu.uco.kudos.aplicacion.dto.IniciarSesionComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.IniciarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RefrescarSesionPuerto;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.IniciarSesionSolicitud;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.RefrescarSesionSolicitud;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.SesionRespuesta;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacionControlador {

    private final IniciarSesionPuerto iniciarSesion;
    private final RefrescarSesionPuerto refrescarSesion;

    public AutenticacionControlador(IniciarSesionPuerto iniciarSesion, RefrescarSesionPuerto refrescarSesion) {
        this.iniciarSesion = iniciarSesion;
        this.refrescarSesion = refrescarSesion;
    }

    @PostMapping(path = "/iniciar-sesion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SesionRespuesta> iniciarSesion(@RequestBody IniciarSesionSolicitud solicitud) {
        IniciarSesionComando comando = new IniciarSesionComando(solicitud.correo(), solicitud.contrasena());
        return sinCache(SesionRespuesta.desde(iniciarSesion.iniciarSesion(comando)));
    }

    @PostMapping(path = "/refrescar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SesionRespuesta> refrescar(@RequestBody RefrescarSesionSolicitud solicitud) {
        return sinCache(SesionRespuesta.desde(refrescarSesion.refrescar(solicitud.tokenRefresco())));
    }

    private static ResponseEntity<SesionRespuesta> sinCache(SesionRespuesta respuesta) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuesta);
    }
}
