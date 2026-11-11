package co.edu.uco.kudos.infraestructura.entrada.rest;

import co.edu.uco.kudos.aplicacion.dto.ArchivoAdjuntoComando;
import co.edu.uco.kudos.aplicacion.dto.KudoPublicadoResultado;
import co.edu.uco.kudos.aplicacion.dto.PublicarKudoComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.PublicarKudoPuerto;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.KudoPublicadoRespuesta;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.PublicarKudoSolicitud;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kudos")
public class KudoControlador {

    private final PublicarKudoPuerto publicarKudo;

    public KudoControlador(PublicarKudoPuerto publicarKudo) {
        this.publicarKudo = publicarKudo;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KudoPublicadoRespuesta> publicar(@AuthenticationPrincipal Jwt jwt,
                                                           @RequestBody PublicarKudoSolicitud solicitud) {
        PublicarKudoComando comando = new PublicarKudoComando(autorDe(jwt), solicitud.destinatarioId(),
                solicitud.categoriaId(), solicitud.contenido(), List.of());
        return creado(publicarKudo.publicar(comando));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KudoPublicadoRespuesta> publicarConAdjuntos(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "destinatarioId", required = false) UUID destinatarioId,
            @RequestParam(name = "categoriaId", required = false) UUID categoriaId,
            @RequestParam(name = "contenido", required = false) String contenido,
            @RequestParam(name = "adjuntos", required = false) List<MultipartFile> adjuntos) {
        List<ArchivoAdjuntoComando> archivos = adjuntos == null ? List.of() : adjuntos.stream()
                .filter(KudoControlador::fueEnviado)
                .map(KudoControlador::aComando)
                .toList();
        PublicarKudoComando comando = new PublicarKudoComando(autorDe(jwt), destinatarioId, categoriaId,
                contenido, archivos);
        return creado(publicarKudo.publicar(comando));
    }

    private static ResponseEntity<KudoPublicadoRespuesta> creado(KudoPublicadoResultado resultado) {
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(resultado.idKudo())
                .toUri();
        return ResponseEntity.created(ubicacion)
                .body(new KudoPublicadoRespuesta(resultado.idKudo(), resultado.fechaPublicacion()));
    }

    private static boolean fueEnviado(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        return !archivo.isEmpty() || (nombre != null && !nombre.isBlank());
    }

    private static ArchivoAdjuntoComando aComando(MultipartFile archivo) {
        try {
            return new ArchivoAdjuntoComando(archivo.getOriginalFilename(), archivo.getBytes());
        } catch (IOException error) {
            throw new UncheckedIOException("No fue posible leer el archivo adjunto", error);
        }
    }

    private static UUID autorDe(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token sin identificador de usuario");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identificador de usuario inválido en el token");
        }
    }
}
