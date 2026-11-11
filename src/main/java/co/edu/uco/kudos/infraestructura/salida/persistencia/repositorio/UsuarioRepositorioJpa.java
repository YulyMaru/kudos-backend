package co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio;

import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.UsuarioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositorioJpa extends JpaRepository<UsuarioEntidad, UUID> {

    Optional<UsuarioEntidad> findByCorreoIgnoreCase(String correo);

    boolean existsByRol(Rol rol);

    boolean existsByCorreoIgnoreCaseOrCelular(String correo, String celular);

    Optional<UsuarioEntidad> findFirstByRolAndEstado(Rol rol, EstadoUsuario estado);

    Optional<UsuarioEntidad> findFirstByRolAndEstadoAndFacultadId(Rol rol, EstadoUsuario estado, UUID facultadId);

    Optional<UsuarioEntidad> findFirstByRolAndEstadoAndProgramaId(Rol rol, EstadoUsuario estado, UUID programaId);
}
