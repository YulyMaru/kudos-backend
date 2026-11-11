package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.puerto.entrada.AdministrarEstructuraPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioEstructura;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class AdministrarEstructuraCasoUso implements AdministrarEstructuraPuerto {

    public static final Set<Rol> ROLES_ADMINISTRADORES = Set.of(Rol.ADMINISTRADOR, Rol.COORDINADOR_ACADEMICO);

    private final RepositorioUsuarios usuarios;
    private final RepositorioEstructura estructura;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public AdministrarEstructuraCasoUso(RepositorioUsuarios usuarios, RepositorioEstructura estructura,
                                        RegistroAuditoria auditoria, GestorTransacciones transacciones, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.estructura = Objects.requireNonNull(estructura);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public Facultad crearFacultad(UUID actorId, String nombre) {
        Usuario actor = Actores.conRol(usuarios, actorId, ROLES_ADMINISTRADORES, "administrar facultades");
        Facultad facultad = Facultad.crear(nombre);
        if (estructura.existeFacultad(facultad.nombre())) {
            throw new ReglaNegocioExcepcion("FACULTAD_DUPLICADA", "nombre", "Ya existe una facultad con ese nombre");
        }
        Instant ahora = Instant.now(reloj);
        transacciones.enTransaccion(() -> {
            estructura.guardarFacultad(facultad);
            auditoria.registrar(EntradaAuditoria.de(ahora, facultad.id(), actor, AccionAuditoria.FACULTAD_CREADA,
                    "FACULTAD", facultad.id(), null, ResultadoAuditoria.EXITOSO, "nombre=" + facultad.nombre()));
        });
        return facultad;
    }

    @Override
    public Programa crearPrograma(UUID actorId, UUID facultadId, String nombre) {
        Usuario actor = Actores.conRol(usuarios, actorId, ROLES_ADMINISTRADORES, "administrar programas");
        Programa programa = Programa.crear(facultadId, nombre);
        estructura.facultad(facultadId)
                .filter(Facultad::activa)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("facultad", String.valueOf(facultadId)));
        if (estructura.existePrograma(facultadId, programa.nombre())) {
            throw new ReglaNegocioExcepcion("PROGRAMA_DUPLICADO", "nombre",
                    "Ya existe un programa con ese nombre en la facultad");
        }
        Instant ahora = Instant.now(reloj);
        transacciones.enTransaccion(() -> {
            estructura.guardarPrograma(programa);
            auditoria.registrar(EntradaAuditoria.de(ahora, programa.id(), actor, AccionAuditoria.PROGRAMA_CREADO,
                    "PROGRAMA", programa.id(), null, ResultadoAuditoria.EXITOSO, "nombre=" + programa.nombre()));
        });
        return programa;
    }

    @Override
    public List<Facultad> facultades() {
        return estructura.facultadesActivas();
    }

    @Override
    public List<Programa> programas(UUID facultadId) {
        return estructura.programasActivos(Objects.requireNonNull(facultadId, "facultadId"));
    }
}
