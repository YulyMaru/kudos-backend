package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCodigos;
import co.edu.uco.kudos.dominio.modelo.CodigoVerificacion;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioCodigosJdbc implements RepositorioCodigos {

    private static final String COLUMNAS = """
            SELECT id, usuario_id, proposito, hash_codigo, creado_en, expira_en, intentos, usado_en
            FROM codigo_verificacion
            """;

    private static final RowMapper<CodigoVerificacion> CODIGO = (fila, numero) -> new CodigoVerificacion(
            ConversionesJdbc.uuid(fila, "id"), ConversionesJdbc.uuid(fila, "usuario_id"),
            PropositoCodigo.valueOf(fila.getString("proposito")), fila.getString("hash_codigo"),
            ConversionesJdbc.instante(fila, "creado_en"), ConversionesJdbc.instante(fila, "expira_en"),
            fila.getInt("intentos"), ConversionesJdbc.instante(fila, "usado_en"));

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioCodigosJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(CodigoVerificacion codigo) {
        jdbc.update("""
                INSERT INTO codigo_verificacion (id, usuario_id, proposito, hash_codigo, creado_en, expira_en,
                                                 intentos, usado_en)
                VALUES (:id, :usuarioId, :proposito, :hash, :creado, :expira, :intentos, :usado)
                """, new MapSqlParameterSource()
                .addValue("id", codigo.id())
                .addValue("usuarioId", codigo.usuarioId())
                .addValue("proposito", codigo.proposito().name())
                .addValue("hash", codigo.hashCodigo())
                .addValue("creado", ConversionesJdbc.fecha(codigo.creadoEn()))
                .addValue("expira", ConversionesJdbc.fecha(codigo.expiraEn()))
                .addValue("intentos", codigo.intentos())
                .addValue("usado", ConversionesJdbc.fecha(codigo.usadoEn())));
    }

    @Override
    public void actualizar(CodigoVerificacion codigo) {
        jdbc.update("UPDATE codigo_verificacion SET intentos = :intentos, usado_en = :usado WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", codigo.id())
                        .addValue("intentos", codigo.intentos())
                        .addValue("usado", ConversionesJdbc.fecha(codigo.usadoEn())));
    }

    @Override
    public Optional<CodigoVerificacion> ultimo(UUID usuarioId, PropositoCodigo proposito) {
        return jdbc.query(COLUMNAS + " WHERE usuario_id = :usuarioId AND proposito = :proposito ORDER BY creado_en DESC LIMIT 1",
                new MapSqlParameterSource("usuarioId", usuarioId).addValue("proposito", proposito.name()), CODIGO)
                .stream().findFirst();
    }

    @Override
    public Optional<CodigoVerificacion> buscarPorHash(PropositoCodigo proposito, String hashCodigo) {
        return jdbc.query(COLUMNAS + " WHERE proposito = :proposito AND hash_codigo = :hash ORDER BY creado_en DESC LIMIT 1",
                new MapSqlParameterSource("proposito", proposito.name()).addValue("hash", hashCodigo), CODIGO)
                .stream().findFirst();
    }

    @Override
    public int contarEmitidosDesde(UUID usuarioId, PropositoCodigo proposito, Instant desde) {
        Integer cantidad = jdbc.queryForObject(
                "SELECT count(*) FROM codigo_verificacion WHERE usuario_id = :usuarioId AND proposito = :proposito AND creado_en >= :desde",
                new MapSqlParameterSource("usuarioId", usuarioId).addValue("proposito", proposito.name())
                        .addValue("desde", ConversionesJdbc.fecha(desde)), Integer.class);
        return cantidad == null ? 0 : cantidad;
    }

    @Override
    public void invalidarVigentes(UUID usuarioId, PropositoCodigo proposito, Instant fecha) {
        jdbc.update(
                "UPDATE codigo_verificacion SET usado_en = :fecha WHERE usuario_id = :usuarioId AND proposito = :proposito AND usado_en IS NULL",
                new MapSqlParameterSource("usuarioId", usuarioId).addValue("proposito", proposito.name())
                        .addValue("fecha", ConversionesJdbc.fecha(fecha)));
    }
}
