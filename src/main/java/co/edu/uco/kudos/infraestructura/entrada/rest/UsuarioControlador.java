package co.edu.uco.kudos.infraestructura.entrada.rest;

import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.BuscarUsuariosPuerto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioControlador {

    private final BuscarUsuariosPuerto buscarUsuarios;

    public UsuarioControlador(BuscarUsuariosPuerto buscarUsuarios) {
        this.buscarUsuarios = buscarUsuarios;
    }

    @GetMapping
    public List<PersonaVista> buscar(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(name = "busqueda", required = false) String busqueda,
                                     @RequestParam(name = "limite", required = false) Integer limite) {
        return buscarUsuarios.buscar(busqueda, limite, solicitante(jwt));
    }

    private static UUID solicitante(Jwt jwt) {
        try {
            return jwt == null || jwt.getSubject() == null ? null : UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException error) {
            return null;
        }
    }
}
