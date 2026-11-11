package co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio;

import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.RegistroAuditoriaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistroAuditoriaRepositorioJpa extends JpaRepository<RegistroAuditoriaEntidad, UUID> {

    List<RegistroAuditoriaEntidad> findByIdCorrelacionOrderByFechaHoraAsc(UUID idCorrelacion);
}
