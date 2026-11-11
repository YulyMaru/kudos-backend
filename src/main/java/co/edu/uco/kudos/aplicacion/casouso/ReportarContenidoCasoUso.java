package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.EstadoContenido;
import co.edu.uco.kudos.aplicacion.dto.ModerarComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ReportarContenidoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ContenidosModerables;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioModeracion;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.Reporte;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class ReportarContenidoCasoUso implements ReportarContenidoPuerto {

    private final RepositorioUsuarios usuarios;
    private final ContenidosModerables contenidos;
    private final RepositorioModeracion moderacion;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public ReportarContenidoCasoUso(RepositorioUsuarios usuarios, ContenidosModerables contenidos,
                                    RepositorioModeracion moderacion, RegistroAuditoria auditoria,
                                    GestorTransacciones transacciones, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.contenidos = Objects.requireNonNull(contenidos);
        this.moderacion = Objects.requireNonNull(moderacion);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public void reportar(ModerarComando comando) {
        Objects.requireNonNull(comando, "comando");
        Usuario reportante = Actores.activo(usuarios, comando.actorId());
        if (comando.tipoContenido() == null) {
            throw new CampoObligatorioExcepcion("tipoContenido");
        }
        if (comando.contenidoId() == null) {
            throw new CampoObligatorioExcepcion("contenidoId");
        }
        EstadoContenido contenido = contenidos.buscar(comando.tipoContenido(), comando.contenidoId())
                .filter(estado -> !estado.vetado())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("contenido", comando.contenidoId().toString()));
        if (moderacion.existeReporte(contenido.tipo(), contenido.id(), reportante.getId())) {
            throw new ReglaNegocioExcepcion("YA_REPORTADO", "contenidoId", "Ya reportaste este contenido");
        }
        Instant ahora = Instant.now(reloj);
        Reporte reporte = Reporte.crear(contenido.tipo(), contenido.id(), reportante.getId(), comando.motivo(), ahora);
        transacciones.enTransaccion(() -> {
            moderacion.guardarReporte(reporte);
            auditoria.registrar(EntradaAuditoria.de(ahora, contenido.kudoId(), reportante,
                    AccionAuditoria.CONTENIDO_REPORTADO, contenido.tipo().name(), contenido.id(), null,
                    ResultadoAuditoria.EXITOSO, null));
        });
    }
}
