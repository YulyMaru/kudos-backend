package co.edu.uco.kudos.aplicacion.puerto.salida;

import co.edu.uco.kudos.dominio.modelo.TokenRefresco;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioTokensRefresco {

    void guardar(TokenRefresco token);

    Optional<TokenRefresco> buscarPorHash(String hashToken);

    boolean revocarSiVigente(UUID id, Instant fecha, UUID reemplazadoPor);

    void revocarTodosDeUsuario(UUID usuarioId, Instant fecha);
}
