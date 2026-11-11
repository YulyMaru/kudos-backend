package co.edu.uco.kudos.dominio.regla;

import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ReglaAprobacion {

    private ReglaAprobacion() {
    }

    public static Rol rolAprobador(Rol rolSolicitado) {
        return switch (rolSolicitado) {
            case ESTUDIANTE, DOCENTE -> Rol.COORDINADOR_PROGRAMA;
            case COORDINADOR_PROGRAMA -> Rol.COORDINADOR_FACULTAD;
            case COORDINADOR_FACULTAD -> Rol.COORDINADOR_ACADEMICO;
            case COORDINADOR_ACADEMICO -> Rol.RECTOR;
            case RECTOR, ADMINISTRADOR -> Rol.ADMINISTRADOR;
        };
    }

    public static List<Rol> rolesQueAprueba(Rol rolAprobador) {
        return Arrays.stream(Rol.values()).filter(rol -> rolAprobador(rol) == rolAprobador).toList();
    }

    public static void validarPuedeAprobar(Usuario aprobador, SolicitudRegistro solicitud,
                                           UUID facultadDelProgramaSolicitado) {
        if (aprobador == null || !aprobador.estaActivo()
                || rolAprobador(solicitud.rolSolicitado()) != aprobador.getRol()
                || !dentroDelAlcance(aprobador, solicitud, facultadDelProgramaSolicitado)) {
            throw new AccesoDenegadoExcepcion("No le compete decidir esta solicitud de registro");
        }
    }

    private static boolean dentroDelAlcance(Usuario aprobador, SolicitudRegistro solicitud, UUID facultadDelPrograma) {
        return switch (aprobador.getRol()) {
            case COORDINADOR_PROGRAMA -> Objects.equals(aprobador.getProgramaId(), solicitud.programaId());
            case COORDINADOR_FACULTAD -> Objects.equals(aprobador.getFacultadId(), facultadDelPrograma);
            default -> true;
        };
    }

    public static boolean esRolUnico(Rol rol) {
        return rol == Rol.RECTOR || rol == Rol.COORDINADOR_ACADEMICO || rol == Rol.COORDINADOR_FACULTAD
                || rol == Rol.COORDINADOR_PROGRAMA;
    }
}
