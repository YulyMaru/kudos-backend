package co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador;

import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioTokensRefresco;
import co.edu.uco.kudos.dominio.modelo.TokenRefresco;
import co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador.TokenRefrescoMapeador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.TokenRefrescoRepositorioJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioTokensRefrescoAdaptador implements RepositorioTokensRefresco {

    private final TokenRefrescoRepositorioJpa repositorio;

    public RepositorioTokensRefrescoAdaptador(TokenRefrescoRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(TokenRefresco token) {
        repositorio.save(TokenRefrescoMapeador.aEntidad(token));
    }

    @Override
    public Optional<TokenRefresco> buscarPorHash(String hashToken) {
        return repositorio.findByHashToken(hashToken).map(TokenRefrescoMapeador::aDominio);
    }

    @Override
    @Transactional
    public boolean revocarSiVigente(UUID id, Instant fecha, UUID reemplazadoPor) {
        return repositorio.revocarSiVigente(id, fecha, reemplazadoPor) == 1;
    }

    @Override
    @Transactional
    public void revocarTodosDeUsuario(UUID usuarioId, Instant fecha) {
        repositorio.revocarTodosDeUsuario(usuarioId, fecha);
    }
}
