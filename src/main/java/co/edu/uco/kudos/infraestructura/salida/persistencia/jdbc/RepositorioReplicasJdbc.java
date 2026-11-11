package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioReplicas;
import co.edu.uco.kudos.dominio.modelo.Replica;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RepositorioReplicasJdbc implements RepositorioReplicas {

    private static final String INSERTAR = """
            INSERT INTO replica (id, kudo_id, replica_padre_id, autor_id, contenido, fecha_publicacion)
            VALUES (:id, :kudoId, :padreId, :autorId, :contenido, :fecha)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioReplicasJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Replica replica) {
        jdbc.update(INSERTAR, new MapSqlParameterSource()
                .addValue("id", replica.id())
                .addValue("kudoId", replica.kudoId())
                .addValue("padreId", replica.replicaPadreId())
                .addValue("autorId", replica.autorId())
                .addValue("contenido", replica.contenido())
                .addValue("fecha", ConversionesJdbc.fecha(replica.fechaPublicacion())));
    }
}
