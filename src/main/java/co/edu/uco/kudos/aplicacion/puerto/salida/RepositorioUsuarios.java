package co.edu.uco.kudos.aplicacion.puerto.salida;

import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioUsuarios {

    Optional<Usuario> buscarPorId(UUID id);
}
