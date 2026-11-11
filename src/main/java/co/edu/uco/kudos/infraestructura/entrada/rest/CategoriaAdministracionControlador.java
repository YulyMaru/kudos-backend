package co.edu.uco.kudos.infraestructura.entrada.rest;


import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.AdministrarCategoriasPuerto;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.EstadoCategoriaSolicitud;
import co.edu.uco.kudos.infraestructura.entrada.rest.dto.NombreSolicitud;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaAdministracionControlador {

    private final AdministrarCategoriasPuerto categorias;

    public CategoriaAdministracionControlador(AdministrarCategoriasPuerto categorias) {
        this.categorias = categorias;
    }

    @PostMapping
    public ResponseEntity<CategoriaVista> crear(@AuthenticationPrincipal Jwt jwt, @RequestBody NombreSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categorias.crear(UsuarioAutenticado.id(jwt), solicitud.nombre()));
    }

    @PutMapping("/{id}")
    public CategoriaVista renombrar(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                    @RequestBody NombreSolicitud solicitud) {
        return categorias.renombrar(UsuarioAutenticado.id(jwt), id, solicitud.nombre());
    }

    @PatchMapping("/{id}/estado")
    public CategoriaVista cambiarEstado(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                        @RequestBody EstadoCategoriaSolicitud solicitud) {
        if (solicitud.activa() == null) {
            throw new CampoObligatorioExcepcion("activa");
        }
        return categorias.cambiarEstado(UsuarioAutenticado.id(jwt), id, solicitud.activa());
    }
}
