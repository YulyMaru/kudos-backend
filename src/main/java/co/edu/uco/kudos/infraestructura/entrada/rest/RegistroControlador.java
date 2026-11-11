package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.dto.RegistrarUsuarioComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.RegistrarUsuarioPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.VerificarContactoPuerto;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.CanalVerificacion;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.MensajeRespuesta;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.ReenviarCodigoSolicitud;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.RegistroSolicitud;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.VerificarSolicitud;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/registro")
public class RegistroControlador {

    private final RegistrarUsuarioPuerto registrarUsuario;
    private final VerificarContactoPuerto verificarContacto;

    public RegistroControlador(RegistrarUsuarioPuerto registrarUsuario, VerificarContactoPuerto verificarContacto) {
        this.registrarUsuario = registrarUsuario;
        this.verificarContacto = verificarContacto;
    }

    @PostMapping
    public ResponseEntity<MensajeRespuesta> registrar(@RequestBody RegistroSolicitud solicitud) {
        registrarUsuario.registrar(new RegistrarUsuarioComando(solicitud.nombreCompleto(), solicitud.correo(),
                solicitud.celular(), solicitud.rol(), solicitud.facultadId(), solicitud.programaId(),
                solicitud.contrasena(), Boolean.TRUE.equals(solicitud.aceptaPolitica())));
        return ResponseEntity.accepted().body(new MensajeRespuesta(
                "Solicitud registrada. Revisa tu correo y tu WhatsApp para verificar tus datos"));
    }

    @PostMapping("/verificar")
    public ResponseEntity<MensajeRespuesta> verificar(@RequestBody VerificarSolicitud solicitud) {
        verificarContacto.verificar(solicitud.correo(), proposito(solicitud.canal()), solicitud.codigo());
        return ResponseEntity.ok(new MensajeRespuesta("Dato verificado correctamente"));
    }

    @PostMapping("/reenviar-codigo")
    public ResponseEntity<MensajeRespuesta> reenviar(@RequestBody ReenviarCodigoSolicitud solicitud) {
        verificarContacto.reenviar(solicitud.correo(), proposito(solicitud.canal()));
        return ResponseEntity.accepted().body(new MensajeRespuesta(
                "Si la cuenta existe y el dato no está verificado, enviamos un nuevo código"));
    }

    private static PropositoCodigo proposito(CanalVerificacion canal) {
        return canal == null ? null : canal.proposito();
    }
}
