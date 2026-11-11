package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.EstadoContenido;
import co.edu.uco.kudos.aplicacion.dto.PublicarReplicaComando;
import co.edu.uco.kudos.aplicacion.dto.ReplicaPublicadaResultado;
import co.edu.uco.kudos.aplicacion.puerto.entrada.PublicarReplicaPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.ContenidosModerables;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioReplicas;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.Replica;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PublicarReplicaCasoUso implements PublicarReplicaPuerto {

    private final RepositorioUsuarios usuarios;
    private final ContenidosModerables contenidos;
    private final RepositorioReplicas replicas;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public PublicarReplicaCasoUso(RepositorioUsuarios usuarios, ContenidosModerables contenidos,
                                  RepositorioReplicas replicas, RegistroAuditoria auditoria,
                                  GestorTransacciones transacciones, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.contenidos = Objects.requireNonNull(contenidos);
        this.replicas = Objects.requireNonNull(replicas);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public ReplicaPublicadaResultado publicar(PublicarReplicaComando comando) {
        Objects.requireNonNull(comando, "comando");
        Usuario autor = Actores.activo(usuarios, comando.autorId());
        UUID kudoId = visible(TipoContenido.KUDO, comando.kudoId(), "kudo").id();
        if (comando.replicaPadreId() != null) {
            EstadoContenido padre = visible(TipoContenido.REPLICA, comando.replicaPadreId(), "replicaPadre");
            if (!kudoId.equals(padre.kudoId())) {
                throw new RecursoNoEncontradoExcepcion("replicaPadre", comando.replicaPadreId().toString());
            }
        }
        Instant ahora = Instant.now(reloj);
        Replica replica = Replica.publicar(kudoId, comando.replicaPadreId(), autor, comando.contenido(), ahora);
        transacciones.enTransaccion(() -> {
            replicas.guardar(replica);
            auditoria.registrar(EntradaAuditoria.de(ahora, kudoId, autor, AccionAuditoria.REPLICA_PUBLICADA,
                    TipoContenido.REPLICA.name(), replica.id(), null, ResultadoAuditoria.EXITOSO,
                    replica.replicaPadreId() == null ? "nivel=raiz" : "padre=" + replica.replicaPadreId()));
        });
        return new ReplicaPublicadaResultado(replica.id(), kudoId, replica.replicaPadreId(), ahora);
    }

    private EstadoContenido visible(TipoContenido tipo, UUID id, String recurso) {
        if (id == null) {
            throw new RecursoNoEncontradoExcepcion(recurso, "null");
        }
        return contenidos.buscar(tipo, id)
                .filter(estado -> !estado.vetado())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(recurso, id.toString()));
    }
}
