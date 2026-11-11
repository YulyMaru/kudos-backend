package co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio;

import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.CategoriaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepositorioJpa extends JpaRepository<CategoriaEntidad, UUID> {

    Optional<CategoriaEntidad> findByNombreIgnoreCase(String nombre);

    List<CategoriaEntidad> findByActivaTrueOrderByNombreAsc();
}
