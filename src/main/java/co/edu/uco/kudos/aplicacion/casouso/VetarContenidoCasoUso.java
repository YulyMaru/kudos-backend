package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.EstadoContenido;
import co.edu.uco.kudos.aplicacion.dto.ModerarComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.VetarContenidoPuerto;
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
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.modelo.Veto;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class VetarContenidoCasoUso implements VetarContenidoPuerto {

    private final RepositorioUsuarios usuarios;
    private final ContenidosModerables contenidos;
    private final RepositorioModeracion moderacion;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public VetarContenidoCasoUso(RepositorioUsuarios usuarios, ContenidosModerables contenidos,
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
    public void vetar(ModerarComando comando) {
        Objects.requireNonNull(comando, "comando");
        Usuario moderador = Actores.activo(usuarios, comando.actorId());
        if (comando.tipoContenido() == null) {
            throw new CampoObligatorioExcepcion("tipoContenido");
        }
        if (comando.contenidoId() == null) {
            throw new CampoObligatorioExcepcion("contenidoId");
        }
        Instant ahora = Instant.now(reloj);
        Veto veto = Veto.aplicar(moderador, comando.tipoContenido(), comando.contenidoId(), comando.motivo(), ahora);
        EstadoContenido contenido = contenidos.buscar(veto.tipoContenido(), veto.contenidoId())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("contenido", veto.contenidoId().toString()));
        if (contenido.vetado()) {
            throw new ReglaNegocioExcepcion("YA_VETADO", "contenidoId", "El contenido ya fue vetado");
        }
        transacciones.enTransaccion(() -> {
            moderacion.guardarVeto(veto);
            contenidos.marcarVetado(contenido.tipo(), contenido.id());
            moderacion.marcarReportesAtendidos(contenido.tipo(), contenido.id(), ahora);
            auditoria.registrar(EntradaAuditoria.de(ahora, contenido.kudoId(), moderador,
                    AccionAuditoria.CONTENIDO_VETADO, contenido.tipo().name(), contenido.id(), null,
                    ResultadoAuditoria.EXITOSO, "autor=" + contenido.autorId()));
        });
    }
}
