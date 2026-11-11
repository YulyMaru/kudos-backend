package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.dto.SolicitudPendienteVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.DecidirSolicitudPuerto;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.MotivoSolicitud;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/solicitudes")
public class SolicitudControlador {

    private final DecidirSolicitudPuerto decidirSolicitud;

    public SolicitudControlador(DecidirSolicitudPuerto decidirSolicitud) {
        this.decidirSolicitud = decidirSolicitud;
    }

    @GetMapping
    public List<SolicitudPendienteVista> pendientes(@AuthenticationPrincipal Jwt jwt) {
        return decidirSolicitud.pendientes(UsuarioAutenticado.id(jwt));
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<Void> aprobar(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        decidirSolicitud.aprobar(UsuarioAutenticado.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                         @RequestBody MotivoSolicitud solicitud) {
        decidirSolicitud.rechazar(UsuarioAutenticado.id(jwt), id, solicitud.motivo());
        return ResponseEntity.noContent().build();
    }
}
