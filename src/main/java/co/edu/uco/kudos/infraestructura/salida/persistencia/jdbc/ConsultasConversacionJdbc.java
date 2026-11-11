package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.dto.ReplicaVista;
import co.edu.uco.kudos.aplicacion.dto.ReporteVista;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasConversacion;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ConsultasConversacionJdbc implements ConsultasConversacion {

    private static final String SELECCION_REPLICAS = """
            SELECT r.id, r.kudo_id, r.replica_padre_id, r.contenido, r.fecha_publicacion,
                   u.id AS autor_id, u.nombre_completo AS autor_nombre, u.rol AS autor_rol,
                   (SELECT count(*) FROM replica h WHERE h.replica_padre_id = r.id AND h.vetado = FALSE)
                       AS cantidad_respuestas,
                   COALESCE((SELECT round(avg(c.valor)::numeric, 2) FROM calificacion c
                             WHERE c.tipo_contenido = 'REPLICA' AND c.contenido_id = r.id), 0) AS promedio,
                   (SELECT count(*) FROM calificacion c
                    WHERE c.tipo_contenido = 'REPLICA' AND c.contenido_id = r.id) AS cantidad_calificaciones
            FROM replica r
            JOIN usuario u ON u.id = r.autor_id
            WHERE r.kudo_id = :kudoId AND r.vetado = FALSE
            """;

    private static final String REPORTES = """
            SELECT rp.id, rp.tipo_contenido, rp.contenido_id, rp.motivo, rp.creado_en,
                   u.id AS reportante_id, u.nombre_completo AS reportante_nombre, u.rol AS reportante_rol,
                   left(COALESCE(k.contenido, r.contenido), 280) AS extracto
            FROM reporte rp
            JOIN usuario u ON u.id = rp.reportante_id
            LEFT JOIN kudo k ON rp.tipo_contenido = 'KUDO' AND k.id = rp.contenido_id
            LEFT JOIN replica r ON rp.tipo_contenido = 'REPLICA' AND r.id = rp.contenido_id
            WHERE rp.estado = 'PENDIENTE'
            ORDER BY rp.creado_en DESC
            LIMIT :limite
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultasConversacionJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ReplicaVista> replicas(UUID kudoId, UUID replicaPadreId, CursorFeed despuesDe, int limite) {
        StringBuilder sql = new StringBuilder(SELECCION_REPLICAS);
        MapSqlParameterSource parametros = new MapSqlParameterSource("kudoId", kudoId).addValue("limite", limite);
        if (replicaPadreId == null) {
            sql.append(" AND r.replica_padre_id IS NULL");
        } else {
            sql.append(" AND r.replica_padre_id = :padreId");
            parametros.addValue("padreId", replicaPadreId);
        }
        if (despuesDe != null) {
            sql.append(" AND (r.fecha_publicacion, r.id) > (:fecha, :id)");
            parametros.addValue("fecha", ConversionesJdbc.fecha(despuesDe.fechaPublicacion()))
                    .addValue("id", despuesDe.id());
        }
        sql.append(" ORDER BY r.fecha_publicacion, r.id LIMIT :limite");
        return jdbc.query(sql.toString(), parametros, (fila, numero) -> new ReplicaVista(
                ConversionesJdbc.uuid(fila, "id"), ConversionesJdbc.uuid(fila, "kudo_id"),
                ConversionesJdbc.uuid(fila, "replica_padre_id"),
                new PersonaVista(ConversionesJdbc.uuid(fila, "autor_id"), fila.getString("autor_nombre"),
                        Rol.valueOf(fila.getString("autor_rol"))),
                fila.getString("contenido"), ConversionesJdbc.instante(fila, "fecha_publicacion"),
                fila.getLong("cantidad_respuestas"), fila.getDouble("promedio"),
                fila.getLong("cantidad_calificaciones")));
    }

    @Override
    public List<ReporteVista> reportesPendientes(int limite) {
        return jdbc.query(REPORTES, new MapSqlParameterSource("limite", limite), (fila, numero) -> new ReporteVista(
                ConversionesJdbc.uuid(fila, "id"), TipoContenido.valueOf(fila.getString("tipo_contenido")),
                ConversionesJdbc.uuid(fila, "contenido_id"),
                new PersonaVista(ConversionesJdbc.uuid(fila, "reportante_id"), fila.getString("reportante_nombre"),
                        Rol.valueOf(fila.getString("reportante_rol"))),
                fila.getString("motivo"), ConversionesJdbc.instante(fila, "creado_en"), fila.getString("extracto")));
    }
}
