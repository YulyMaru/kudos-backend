package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.GestionarVinculosPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioEstructura;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioVinculos;
import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GestionarVinculosCasoUso implements GestionarVinculosPuerto {

    private final RepositorioUsuarios usuarios;
    private final RepositorioEstructura estructura;
    private final RepositorioVinculos vinculos;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public GestionarVinculosCasoUso(RepositorioUsuarios usuarios, RepositorioEstructura estructura,
                                    RepositorioVinculos vinculos, RegistroAuditoria auditoria,
                                    GestorTransacciones transacciones, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.estructura = Objects.requireNonNull(estructura);
        this.vinculos = Objects.requireNonNull(vinculos);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public void vincular(UUID actorId, UUID programaId, UUID usuarioId) {
        Usuario coordinador = coordinadorDelPrograma(actorId, programaId);
        Usuario miembro = miembroVinculable(usuarioId);
        Instant ahora = Instant.now(reloj);
        transacciones.enTransaccion(() -> {
            vinculos.vincular(miembro.getId(), programaId);
            auditoria.registrar(entrada(coordinador, miembro, programaId, ahora, AccionAuditoria.VINCULO_CREADO));
        });
    }

    @Override
    public void desvincular(UUID actorId, UUID programaId, UUID usuarioId) {
        Usuario coordinador = coordinadorDelPrograma(actorId, programaId);
        Usuario miembro = usuarios.buscarPorId(Objects.requireNonNull(usuarioId, "usuarioId"))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("usuario", usuarioId.toString()));
        Instant ahora = Instant.now(reloj);
        transacciones.enTransaccion(() -> {
            if (!vinculos.desvincular(miembro.getId(), programaId)) {
                throw new RecursoNoEncontradoExcepcion("vinculo", miembro.getId() + "/" + programaId);
            }
            auditoria.registrar(entrada(coordinador, miembro, programaId, ahora, AccionAuditoria.VINCULO_ELIMINADO));
        });
    }

    @Override
    public List<PersonaVista> miembros(UUID actorId, UUID programaId) {
        Usuario actor = Actores.activo(usuarios, actorId);
        Programa programa = programa(programaId);
        boolean permitido = switch (actor.getRol()) {
            case COORDINADOR_PROGRAMA -> programa.id().equals(actor.getProgramaId());
            case COORDINADOR_FACULTAD -> programa.facultadId().equals(actor.getFacultadId());
            case COORDINADOR_ACADEMICO, RECTOR, ADMINISTRADOR -> true;
            default -> false;
        };
        if (!permitido) {
            throw new AccesoDenegadoExcepcion("No puede consultar los miembros de este programa");
        }
        return vinculos.miembros(programa.id());
    }

    @Override
    public List<Programa> programasDe(UUID usuarioId) {
        return vinculos.programasDe(Objects.requireNonNull(usuarioId, "usuarioId"));
    }

    private Usuario coordinadorDelPrograma(UUID actorId, UUID programaId) {
        Usuario actor = Actores.activo(usuarios, actorId);
        Programa programa = programa(programaId);
        if (actor.getRol() != Rol.COORDINADOR_PROGRAMA || !programa.id().equals(actor.getProgramaId())) {
            throw new AccesoDenegadoExcepcion("Solo el coordinador del programa puede gestionar sus vínculos");
        }
        return actor;
    }

    private Usuario miembroVinculable(UUID usuarioId) {
        Usuario miembro = usuarios.buscarPorId(Objects.requireNonNull(usuarioId, "usuarioId"))
                .filter(Usuario::estaActivo)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("usuario", usuarioId.toString()));
        if (miembro.getRol() != Rol.ESTUDIANTE && miembro.getRol() != Rol.DOCENTE) {
            throw new ReglaNegocioExcepcion("ROL_NO_VINCULABLE", "usuarioId",
                    "Solo se pueden vincular estudiantes y docentes a un programa");
        }
        return miembro;
    }

    private Programa programa(UUID programaId) {
        return estructura.programa(Objects.requireNonNull(programaId, "programaId"))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("programa", programaId.toString()));
    }

    private static EntradaAuditoria entrada(Usuario coordinador, Usuario miembro, UUID programaId, Instant ahora,
                                            AccionAuditoria accion) {
        return EntradaAuditoria.de(ahora, programaId, coordinador, accion, NotificacionesCuenta.RECURSO_USUARIO,
                miembro.getId(), null, ResultadoAuditoria.EXITOSO, "programa=" + programaId);
    }
}
