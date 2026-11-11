package co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio;

import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.KudoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KudoRepositorioJpa extends JpaRepository<KudoEntidad, UUID> {
}
