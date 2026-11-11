package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.puerto.entrada.AdministrarEstructuraPuerto;
import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.NombreSolicitud;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.ProgramaSolicitud;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class EstructuraControlador {

    private final AdministrarEstructuraPuerto estructura;

    public EstructuraControlador(AdministrarEstructuraPuerto estructura) {
        this.estructura = estructura;
    }

    @GetMapping("/api/v1/catalogo/facultades")
    public List<Facultad> facultades() {
        return estructura.facultades();
    }

    @GetMapping("/api/v1/catalogo/facultades/{id}/programas")
    public List<Programa> programas(@PathVariable("id") UUID facultadId) {
        return estructura.programas(facultadId);
    }

    @PostMapping("/api/v1/facultades")
    public ResponseEntity<Facultad> crearFacultad(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestBody NombreSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estructura.crearFacultad(UsuarioAutenticado.id(jwt), solicitud.nombre()));
    }

    @PostMapping("/api/v1/programas")
    public ResponseEntity<Programa> crearPrograma(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestBody ProgramaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estructura.crearPrograma(UsuarioAutenticado.id(jwt),
                solicitud.facultadId(), solicitud.nombre()));
    }
}
