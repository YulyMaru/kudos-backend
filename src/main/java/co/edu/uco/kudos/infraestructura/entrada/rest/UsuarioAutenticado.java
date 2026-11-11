package co.edu.uco.kudos.infraestructura.entrada.rest;


import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    static UUID id(Jwt jwt) {
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
