package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.dto.PaginaReplicas;
import co.edu.uco.kudos.aplicacion.dto.PublicarReplicaComando;
import co.edu.uco.kudos.aplicacion.dto.ReplicaPublicadaResultado;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarReplicasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.PublicarReplicaPuerto;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.ReplicaSolicitud;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kudos/{kudoId}/replicas")
public class ConversacionControlador {

    private final PublicarReplicaPuerto publicarReplica;
    private final ConsultarReplicasPuerto consultarReplicas;

    public ConversacionControlador(PublicarReplicaPuerto publicarReplica, ConsultarReplicasPuerto consultarReplicas) {
        this.publicarReplica = publicarReplica;
        this.consultarReplicas = consultarReplicas;
    }

    @PostMapping
    public ResponseEntity<ReplicaPublicadaResultado> publicar(@AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable("kudoId") UUID kudoId,
                                                              @RequestBody ReplicaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(publicarReplica.publicar(new PublicarReplicaComando(
                UsuarioAutenticado.id(jwt), kudoId, solicitud.replicaPadreId(), solicitud.contenido())));
    }

    @GetMapping
    public PaginaReplicas consultar(@PathVariable("kudoId") UUID kudoId,
                                    @RequestParam(name = "padre", required = false) UUID replicaPadreId,
                                    @RequestParam(name = "cursor", required = false) String cursor,
                                    @RequestParam(name = "limite", required = false) Integer limite) {
        return consultarReplicas.consultar(kudoId, replicaPadreId, cursor, limite);
    }
}
