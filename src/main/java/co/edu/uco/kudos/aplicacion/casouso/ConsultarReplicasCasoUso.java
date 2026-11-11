package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.PaginaReplicas;
import co.edu.uco.kudos.aplicacion.dto.ReplicaVista;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ConsultarReplicasPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasConversacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.ContenidosModerables;
import co.edu.uco.kudos.dominio.excepcion.ParametroInvalidoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarReplicasCasoUso implements ConsultarReplicasPuerto {

    public static final int LIMITE_POR_DEFECTO = 20;
    public static final int LIMITE_MAXIMO = 50;

    private final ConsultasConversacion consultas;
    private final ContenidosModerables contenidos;

    public ConsultarReplicasCasoUso(ConsultasConversacion consultas, ContenidosModerables contenidos) {
        this.consultas = Objects.requireNonNull(consultas);
        this.contenidos = Objects.requireNonNull(contenidos);
    }

    @Override
    public PaginaReplicas consultar(UUID kudoId, UUID replicaPadreId, String cursor, Integer limite) {
        int tamano = limite == null ? LIMITE_POR_DEFECTO : limite;
        if (tamano < 1 || tamano > LIMITE_MAXIMO) {
            throw new ParametroInvalidoExcepcion("limite", "El límite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
        contenidos.buscar(TipoContenido.KUDO, Objects.requireNonNull(kudoId, "kudoId"))
                .filter(estado -> !estado.vetado())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("kudo", kudoId.toString()));
        CursorFeed despuesDe = cursor == null || cursor.isBlank() ? null : CursorFeed.decodificar(cursor);
        List<ReplicaVista> encontradas = consultas.replicas(kudoId, replicaPadreId, despuesDe, tamano + 1);
        if (encontradas.size() <= tamano) {
            return new PaginaReplicas(encontradas, null);
        }
        List<ReplicaVista> pagina = encontradas.subList(0, tamano);
        ReplicaVista ultima = pagina.getLast();
        return new PaginaReplicas(pagina, new CursorFeed(ultima.fechaPublicacion(), ultima.id()).codificar());
    }
}
