package co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador;

import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador.UsuarioMapeador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.UsuarioRepositorioJpa;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioUsuariosAdaptador implements RepositorioUsuarios {

    private final UsuarioRepositorioJpa repositorio;

    public RepositorioUsuariosAdaptador(UsuarioRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return repositorio.findById(id).map(UsuarioMapeador::aDominio);
    }
}
