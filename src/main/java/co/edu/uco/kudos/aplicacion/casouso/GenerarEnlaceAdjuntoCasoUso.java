package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.AdjuntoDatos;
import co.edu.uco.kudos.aplicacion.dto.EnlaceAdjunto;
import co.edu.uco.kudos.aplicacion.puerto.entrada.GenerarEnlaceAdjuntoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasKudos;
import co.edu.uco.kudos.aplicacion.puerto.salida.EnlacesArchivos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class GenerarEnlaceAdjuntoCasoUso implements GenerarEnlaceAdjuntoPuerto {

    private final RepositorioUsuarios usuarios;
    private final ConsultasKudos consultas;
    private final EnlacesArchivos enlaces;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;
    private final Duration vigencia;

    public GenerarEnlaceAdjuntoCasoUso(RepositorioUsuarios usuarios, ConsultasKudos consultas, EnlacesArchivos enlaces,
                                       RegistroAuditoria auditoria, Clock reloj, Duration vigencia) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.consultas = Objects.requireNonNull(consultas);
        this.enlaces = Objects.requireNonNull(enlaces);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.reloj = Objects.requireNonNull(reloj);
        this.vigencia = Objects.requireNonNull(vigencia);
    }

    @Override
    public EnlaceAdjunto generar(UUID kudoId, UUID adjuntoId, UUID usuarioId) {
        Usuario usuario = Actores.activo(usuarios, usuarioId);
        AdjuntoDatos adjunto = consultas.adjunto(Objects.requireNonNull(kudoId, "kudoId"),
                        Objects.requireNonNull(adjuntoId, "adjuntoId"))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("adjunto", adjuntoId.toString()));
        String url = enlaces.generarEnlaceTemporal(adjunto.claveObjeto(), vigencia)
                .orElseThrow(() -> new ReglaNegocioExcepcion("ENLACE_NO_DISPONIBLE", "adjunto",
                        "El almacenamiento configurado no permite generar enlaces de descarga"));
        Instant ahora = Instant.now(reloj);
        auditoria.registrar(EntradaAuditoria.de(ahora, kudoId, usuario, AccionAuditoria.ADJUNTO_DESCARGADO, "ADJUNTO",
                adjunto.id(), null, ResultadoAuditoria.EXITOSO, "tipo=" + adjunto.tipoContenido()));
        return new EnlaceAdjunto(url, ahora.plus(vigencia));
    }
}
