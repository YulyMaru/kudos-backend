package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.dominio.modelo.Categoria;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioCategoriasAdministracion {

    Optional<Categoria> buscarPorId(UUID id);

    boolean existeNombre(String nombre, UUID excluirId);

    void guardar(Categoria categoria);
}
