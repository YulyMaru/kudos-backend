package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.util.Set;
import java.util.UUID;

final class Actores {

    private Actores() {
    }

    static Usuario activo(RepositorioUsuarios usuarios, UUID actorId) {
        if (actorId == null) {
            throw new AccesoDenegadoExcepcion("Se requiere un usuario autenticado");
        }
        return usuarios.buscarPorId(actorId)
                .filter(Usuario::estaActivo)
                .orElseThrow(() -> new AccesoDenegadoExcepcion("El usuario no tiene una cuenta activa"));
    }

    static Usuario conRol(RepositorioUsuarios usuarios, UUID actorId, Set<Rol> rolesPermitidos, String accion) {
        Usuario actor = activo(usuarios, actorId);
        if (!rolesPermitidos.contains(actor.getRol())) {
            throw new AccesoDenegadoExcepcion("Su rol no le permite " + accion);
        }
        return actor;
    }
}
