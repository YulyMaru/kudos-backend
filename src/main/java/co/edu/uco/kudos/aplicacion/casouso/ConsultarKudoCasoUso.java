package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.AdjuntoDatos;
import co.edu.uco.kudos.aplicacion.dto.KudoDetalleDatos;
import co.edu.uco.kudos.aplicacion.dto.KudoDetalleVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarKudoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasKudos;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;

import java.util.Objects;
import java.util.UUID;

public final class ConsultarKudoCasoUso implements ConsultarKudoPuerto {

    private final ConsultasKudos consultas;

    public ConsultarKudoCasoUso(ConsultasKudos consultas) {
        this.consultas = Objects.requireNonNull(consultas);
    }

    @Override
    public KudoDetalleVista consultar(UUID id) {
        KudoDetalleDatos datos = consultas.detalle(Objects.requireNonNull(id, "id"))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("kudo", id.toString()));
        return new KudoDetalleVista(datos.id(), datos.autor(), datos.destinatario(), datos.categoria(),
                datos.contenido(), datos.fechaPublicacion(),
                datos.adjuntos().stream().map(ConsultarKudoCasoUso::aVista).toList(), datos.cantidadReplicas(),
                datos.promedioCalificacion(), datos.cantidadCalificaciones());
    }

    private static KudoDetalleVista.AdjuntoVista aVista(AdjuntoDatos adjunto) {
        return new KudoDetalleVista.AdjuntoVista(adjunto.id(), adjunto.nombreOriginal(), adjunto.tipoContenido(),
                adjunto.tamanoBytes());
    }
}
