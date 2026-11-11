package co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador;

import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCategorias;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador.CategoriaMapeador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.CategoriaRepositorioJpa;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioCategoriasAdaptador implements RepositorioCategorias {

    private final CategoriaRepositorioJpa repositorio;

    public RepositorioCategoriasAdaptador(CategoriaRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Categoria> buscarPorId(UUID id) {
        return repositorio.findById(id).map(CategoriaMapeador::aDominio);
    }
}
