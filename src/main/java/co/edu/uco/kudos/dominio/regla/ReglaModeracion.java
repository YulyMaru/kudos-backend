package co.edu.uco.kudos.dominio.regla;

import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.util.Set;

public final class ReglaModeracion {

    public static final Set<Rol> ROLES_MODERADORES = Set.of(Rol.RECTOR, Rol.COORDINADOR_ACADEMICO);

    private ReglaModeracion() {
    }

    public static boolean puedeModerar(Usuario usuario) {
        return usuario != null && usuario.estaActivo() && ROLES_MODERADORES.contains(usuario.getRol());
    }

    public static void validarPuedeVetar(Usuario usuario) {
        if (!puedeModerar(usuario)) {
            throw new AccesoDenegadoExcepcion("Solo el Rector y el Coordinador Académico pueden vetar contenido");
        }
    }
}
