package co.edu.uco.kudos.infraestructura.salida.persistencia.consulta;

import co.edu.uco.kudos.aplicacion.dto.AdjuntoDatos;
import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.KudoDetalleDatos;
import co.edu.uco.kudos.aplicacion.dto.KudoResumenVista;
import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasKudos;
import co.edu.uco.kudos.dominio.modelo.Rol;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConsultasKudosAdaptador implements ConsultasKudos {

    static final int LONGITUD_EXTRACTO = 280;

    private static final String SELECCION = """
            SELECT k.id, k.fecha_publicacion,
                   left(k.contenido, %d) AS extracto,
                   char_length(k.contenido) > %d AS truncado,
                   k.contenido,
                   a.id AS autor_id, a.nombre_completo AS autor_nombre, a.rol AS autor_rol,
                   d.id AS destinatario_id, d.nombre_completo AS destinatario_nombre, d.rol AS destinatario_rol,
                   c.id AS categoria_id, c.nombre AS categoria_nombre,
                   (SELECT count(*) FROM adjunto ad WHERE ad.kudo_id = k.id) AS cantidad_adjuntos,
                   (SELECT count(*) FROM replica r WHERE r.kudo_id = k.id AND r.vetado = FALSE) AS cantidad_replicas,
                   COALESCE((SELECT round(avg(ca.valor)::numeric, 2) FROM calificacion ca
                             WHERE ca.tipo_contenido = 'KUDO' AND ca.contenido_id = k.id), 0) AS promedio,
                   (SELECT count(*) FROM calificacion ca
                    WHERE ca.tipo_contenido = 'KUDO' AND ca.contenido_id = k.id) AS cantidad_calificaciones
            FROM kudo k
            JOIN usuario a ON a.id = k.autor_id
            JOIN usuario d ON d.id = k.destinatario_id
            JOIN categoria c ON c.id = k.categoria_id
            WHERE k.vetado = FALSE
            """.formatted(LONGITUD_EXTRACTO, LONGITUD_EXTRACTO);

    private static final String FEED_PRIMERA_PAGINA = SELECCION + """
            ORDER BY k.fecha_publicacion DESC, k.id DESC
            LIMIT :limite
            """;

    private static final String FEED_SIGUIENTE_PAGINA = SELECCION + """
            AND (k.fecha_publicacion, k.id) < (:fecha, :id)
            ORDER BY k.fecha_publicacion DESC, k.id DESC
            LIMIT :limite
            """;

    private static final String DETALLE = SELECCION + " AND k.id = :id";

    private static final String ADJUNTOS = """
            SELECT id, nombre_original, tipo_contenido, tamano_bytes, clave_objeto
            FROM adjunto
            WHERE kudo_id = :id
            ORDER BY nombre_original, id
            """;

    private static final String ADJUNTO = """
            SELECT a.id, a.nombre_original, a.tipo_contenido, a.tamano_bytes, a.clave_objeto
            FROM adjunto a
            JOIN kudo k ON k.id = a.kudo_id
            WHERE a.id = :adjuntoId AND a.kudo_id = :kudoId AND k.vetado = FALSE
            """;

    private static final RowMapper<AdjuntoDatos> MAPEO_ADJUNTO = (fila, numero) -> new AdjuntoDatos(
            fila.getObject("id", UUID.class), fila.getString("nombre_original"), fila.getString("tipo_contenido"),
            fila.getLong("tamano_bytes"), fila.getString("clave_objeto"));

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultasKudosAdaptador(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<KudoResumenVista> feed(CursorFeed despuesDe, int limite) {
        MapSqlParameterSource parametros = new MapSqlParameterSource("limite", limite);
        if (despuesDe == null) {
            return jdbc.query(FEED_PRIMERA_PAGINA, parametros, (fila, numero) -> resumen(fila));
        }
        parametros.addValue("fecha", OffsetDateTime.ofInstant(despuesDe.fechaPublicacion(), ZoneOffset.UTC))
                .addValue("id", despuesDe.id());
        return jdbc.query(FEED_SIGUIENTE_PAGINA, parametros, (fila, numero) -> resumen(fila));
    }

    @Override
    public Optional<KudoDetalleDatos> detalle(UUID id) {
        MapSqlParameterSource parametros = new MapSqlParameterSource("id", id);
        List<KudoDetalleDatos> encontrados = jdbc.query(DETALLE, parametros, (fila, numero) -> new KudoDetalleDatos(
                fila.getObject("id", UUID.class), autor(fila), destinatario(fila), categoria(fila),
                fila.getString("contenido"), fecha(fila), List.of(), fila.getLong("cantidad_replicas"),
                fila.getDouble("promedio"), fila.getLong("cantidad_calificaciones")));
        return encontrados.stream().findFirst().map(datos -> new KudoDetalleDatos(datos.id(), datos.autor(),
                datos.destinatario(), datos.categoria(), datos.contenido(), datos.fechaPublicacion(),
                jdbc.query(ADJUNTOS, parametros, MAPEO_ADJUNTO), datos.cantidadReplicas(),
                datos.promedioCalificacion(), datos.cantidadCalificaciones()));
    }

    @Override
    public Optional<AdjuntoDatos> adjunto(UUID kudoId, UUID adjuntoId) {
        return jdbc.query(ADJUNTO, new MapSqlParameterSource("kudoId", kudoId).addValue("adjuntoId", adjuntoId),
                MAPEO_ADJUNTO).stream().findFirst();
    }

    private static KudoResumenVista resumen(ResultSet fila) throws SQLException {
        return new KudoResumenVista(fila.getObject("id", UUID.class), autor(fila), destinatario(fila),
                categoria(fila), fila.getString("extracto"), fila.getBoolean("truncado"), fecha(fila),
                fila.getInt("cantidad_adjuntos"), fila.getLong("cantidad_replicas"), fila.getDouble("promedio"),
                fila.getLong("cantidad_calificaciones"));
    }

    private static PersonaVista autor(ResultSet fila) throws SQLException {
        return new PersonaVista(fila.getObject("autor_id", UUID.class), fila.getString("autor_nombre"),
                Rol.valueOf(fila.getString("autor_rol")));
    }

    private static PersonaVista destinatario(ResultSet fila) throws SQLException {
        return new PersonaVista(fila.getObject("destinatario_id", UUID.class), fila.getString("destinatario_nombre"),
                Rol.valueOf(fila.getString("destinatario_rol")));
    }

    private static CategoriaVista categoria(ResultSet fila) throws SQLException {
        return new CategoriaVista(fila.getObject("categoria_id", UUID.class), fila.getString("categoria_nombre"));
    }

    private static Instant fecha(ResultSet fila) throws SQLException {
        return fila.getObject("fecha_publicacion", OffsetDateTime.class).toInstant();
    }
}
