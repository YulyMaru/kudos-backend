package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.CalificarComando;
import co.edu.uco.kudos.aplicacion.dto.ResumenCalificacion;
import co.edu.uco.kudos.aplicacion.puerto.entrada.CalificarContenidoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ContenidosModerables;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCalificaciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.modelo.Calificacion;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class CalificarContenidoCasoUso implements CalificarContenidoPuerto {

    private final RepositorioUsuarios usuarios;
    private final ContenidosModerables contenidos;
    private final RepositorioCalificaciones calificaciones;
    private final Clock reloj;

    public CalificarContenidoCasoUso(RepositorioUsuarios usuarios, ContenidosModerables contenidos,
                                     RepositorioCalificaciones calificaciones, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.contenidos = Objects.requireNonNull(contenidos);
        this.calificaciones = Objects.requireNonNull(calificaciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public ResumenCalificacion calificar(CalificarComando comando) {
        Objects.requireNonNull(comando, "comando");
        Usuario usuario = Actores.activo(usuarios, comando.usuarioId());
        if (comando.valor() == null) {
            throw new CampoObligatorioExcepcion("valor");
        }
        Calificacion calificacion = new Calificacion(usuario.getId(), comando.tipoContenido(), comando.contenidoId(),
                comando.valor(), Instant.now(reloj));
        contenidos.buscar(calificacion.tipoContenido(), calificacion.contenidoId())
                .filter(estado -> !estado.vetado())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(calificacion.tipoContenido().name().toLowerCase(),
                        calificacion.contenidoId().toString()));
        calificaciones.guardarOReemplazar(calificacion);
        return calificaciones.resumen(calificacion.tipoContenido(), calificacion.contenidoId(), usuario.getId());
    }
}
