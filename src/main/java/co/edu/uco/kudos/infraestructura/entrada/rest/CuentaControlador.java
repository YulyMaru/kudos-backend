package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.puerto.entrada.CerrarSesionPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RestablecerContrasenaPuerto;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.ConfirmarRestablecimientoSolicitud;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.CorreoSolicitud;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.MensajeRespuesta;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.RefrescarSesionSolicitud;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class CuentaControlador {

    private final CerrarSesionPuerto cerrarSesion;
    private final RestablecerContrasenaPuerto restablecerContrasena;

    public CuentaControlador(CerrarSesionPuerto cerrarSesion, RestablecerContrasenaPuerto restablecerContrasena) {
        this.cerrarSesion = cerrarSesion;
        this.restablecerContrasena = restablecerContrasena;
    }

    @PostMapping("/cerrar-sesion")
    public ResponseEntity<Void> cerrarSesion(@RequestBody RefrescarSesionSolicitud solicitud) {
        cerrarSesion.cerrar(solicitud.tokenRefresco());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restablecer/solicitar")
    public ResponseEntity<MensajeRespuesta> solicitarRestablecimiento(@RequestBody CorreoSolicitud solicitud) {
        restablecerContrasena.solicitar(solicitud.correo());
        return ResponseEntity.accepted().body(new MensajeRespuesta(
                "Si el correo pertenece a una cuenta activa, enviaremos un enlace para restablecer la contraseña"));
    }

    @PostMapping("/restablecer/confirmar")
    public ResponseEntity<MensajeRespuesta> confirmarRestablecimiento(
            @RequestBody ConfirmarRestablecimientoSolicitud solicitud) {
        restablecerContrasena.confirmar(solicitud.token(), solicitud.nuevaContrasena());
        return ResponseEntity.ok(new MensajeRespuesta("Contraseña actualizada. Inicia sesión de nuevo"));
    }
}
