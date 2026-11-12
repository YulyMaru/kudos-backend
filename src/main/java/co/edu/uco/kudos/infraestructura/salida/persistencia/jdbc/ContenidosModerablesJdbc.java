package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.dto.EstadoContenido;
import co.edu.uco.kudos.aplicacion.puerto.salida.ContenidosModerables;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ContenidosModerablesJdbc implements ContenidosModerables {

    private static final String KUDO = "SELECT id, id AS kudo_id, autor_id, vetado FROM kudo WHERE id = :id";
    private static final String REPLICA = """
            SELECT r.id, r.kudo_id, r.autor_id, (r.vetado OR k.vetado) AS vetado
            FROM replica r
            JOIN kudo k ON k.id = r.kudo_id
            WHERE r.id = :id
            """;
    private static final String VETAR_KUDO = "UPDATE kudo SET vetado = TRUE WHERE id = :id";
    private static final String VETAR_REPLICA = "UPDATE replica SET vetado = TRUE WHERE id = :id";

    private final NamedParameterJdbcTemplate jdbc;

    public ContenidosModerablesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<EstadoContenido> buscar(TipoContenido tipo, UUID id) {
        String sql = tipo == TipoContenido.KUDO ? KUDO : REPLICA;
        return jdbc.query(sql, new MapSqlParameterSource("id", id), (fila, numero) -> new EstadoContenido(tipo,
                ConversionesJdbc.uuid(fila, "id"), ConversionesJdbc.uuid(fila, "kudo_id"),
                ConversionesJdbc.uuid(fila, "autor_id"), fila.getBoolean("vetado"))).stream().findFirst();
    }

    @Override
    public void marcarVetado(TipoContenido tipo, UUID id) {
        String sql = tipo == TipoContenido.KUDO ? VETAR_KUDO : VETAR_REPLICA;
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }
}
