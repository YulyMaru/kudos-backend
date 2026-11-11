package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.GestionarVinculosPuerto;
import co.edu.uco.kudos.dominio.modelo.Programa;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class VinculoControlador {

    private final GestionarVinculosPuerto vinculos;

    public VinculoControlador(GestionarVinculosPuerto vinculos) {
        this.vinculos = vinculos;
    }

    @GetMapping("/api/v1/programas/{programaId}/miembros")
    public List<PersonaVista> miembros(@AuthenticationPrincipal Jwt jwt, @PathVariable("programaId") UUID programaId) {
        return vinculos.miembros(UsuarioAutenticado.id(jwt), programaId);
    }

    @PutMapping("/api/v1/programas/{programaId}/miembros/{usuarioId}")
    public ResponseEntity<Void> vincular(@AuthenticationPrincipal Jwt jwt, @PathVariable("programaId") UUID programaId,
                                         @PathVariable("usuarioId") UUID usuarioId) {
        vinculos.vincular(UsuarioAutenticado.id(jwt), programaId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/programas/{programaId}/miembros/{usuarioId}")
    public ResponseEntity<Void> desvincular(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable("programaId") UUID programaId,
                                            @PathVariable("usuarioId") UUID usuarioId) {
        vinculos.desvincular(UsuarioAutenticado.id(jwt), programaId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/usuarios/{usuarioId}/programas")
    public List<Programa> programasDe(@PathVariable("usuarioId") UUID usuarioId) {
        return vinculos.programasDe(usuarioId);
    }
}
