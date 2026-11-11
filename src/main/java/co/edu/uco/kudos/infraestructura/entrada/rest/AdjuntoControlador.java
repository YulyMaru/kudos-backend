package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.dto.EnlaceAdjunto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.GenerarEnlaceAdjuntoPuerto;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AdjuntoControlador {

    private final GenerarEnlaceAdjuntoPuerto generarEnlace;

    public AdjuntoControlador(GenerarEnlaceAdjuntoPuerto generarEnlace) {
        this.generarEnlace = generarEnlace;
    }

    @GetMapping("/api/v1/kudos/{kudoId}/adjuntos/{adjuntoId}/enlace")
    public ResponseEntity<EnlaceAdjunto> enlace(@AuthenticationPrincipal Jwt jwt, @PathVariable("kudoId") UUID kudoId,
                                                @PathVariable("adjuntoId") UUID adjuntoId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(generarEnlace.generar(kudoId, adjuntoId, UsuarioAutenticado.id(jwt)));
    }
}
