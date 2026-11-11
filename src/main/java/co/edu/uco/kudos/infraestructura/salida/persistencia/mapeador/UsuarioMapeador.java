package co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador;

import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.UsuarioEntidad;

public final class UsuarioMapeador {

    private UsuarioMapeador() {
    }

    public static Usuario aDominio(UsuarioEntidad entidad) {
        return new Usuario(entidad.getId(), entidad.getNombreCompleto(), entidad.getCorreo(), entidad.getCelular(),
                entidad.isCorreoVerificado(), entidad.isCelularVerificado(), entidad.getRol(), entidad.getEstado(),
                entidad.getHashContrasena(), entidad.getIntentosFallidos(), entidad.getBloqueadoHasta(),
                entidad.getFacultadId(), entidad.getProgramaId());
    }

    public static UsuarioEntidad aEntidad(Usuario usuario) {
        return new UsuarioEntidad(usuario.getId(), usuario.getNombreCompleto(), usuario.getCorreo(),
                usuario.getCelular(), usuario.isCorreoVerificado(), usuario.isCelularVerificado(), usuario.getRol(),
                usuario.getEstado(), usuario.getHashContrasena(), usuario.getIntentosFallidos(),
                usuario.getBloqueadoHasta(), usuario.getFacultadId(), usuario.getProgramaId());
    }
}
