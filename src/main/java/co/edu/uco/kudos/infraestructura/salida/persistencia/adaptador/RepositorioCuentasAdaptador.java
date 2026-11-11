package co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador;

import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCuentas;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.UsuarioEntidad;
import co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador.UsuarioMapeador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.UsuarioRepositorioJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioCuentasAdaptador implements RepositorioCuentas {

    private final UsuarioRepositorioJpa repositorio;

    public RepositorioCuentasAdaptador(UsuarioRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return repositorio.findByCorreoIgnoreCase(correo).map(UsuarioMapeador::aDominio);
    }

    @Override
    public boolean existeConRol(Rol rol) {
        return repositorio.existsByRol(rol);
    }

    @Override
    public boolean existePorCorreoOCelular(String correo, String celular) {
        return repositorio.existsByCorreoIgnoreCaseOrCelular(correo, celular);
    }

    @Override
    public Optional<Usuario> buscarActivoConRol(Rol rol, UUID facultadId, UUID programaId) {
        Optional<UsuarioEntidad> encontrado = switch (rol) {
            case COORDINADOR_FACULTAD -> repositorio.findFirstByRolAndEstadoAndFacultadId(rol, EstadoUsuario.ACTIVO,
                    facultadId);
            case COORDINADOR_PROGRAMA -> repositorio.findFirstByRolAndEstadoAndProgramaId(rol, EstadoUsuario.ACTIVO,
                    programaId);
            default -> repositorio.findFirstByRolAndEstado(rol, EstadoUsuario.ACTIVO);
        };
        return encontrado.map(UsuarioMapeador::aDominio);
    }

    @Override
    @Transactional
    public void guardar(Usuario usuario) {
        UsuarioEntidad datos = UsuarioMapeador.aEntidad(usuario);
        repositorio.findById(usuario.getId())
                .ifPresentOrElse(existente -> existente.actualizarDesde(datos), () -> repositorio.save(datos));
        repositorio.flush();
    }
}
