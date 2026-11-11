package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.dto.ResumenCalificacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCalificaciones;
import co.edu.uco.kudos.dominio.modelo.Calificacion;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RepositorioCalificacionesJdbc implements RepositorioCalificaciones {

    private static final String GUARDAR = """
            INSERT INTO calificacion (usuario_id, tipo_contenido, contenido_id, valor, actualizado_en)
            VALUES (:usuarioId, :tipo, :contenidoId, :valor, :fecha)
            ON CONFLICT (usuario_id, contenido_id, tipo_contenido)
            DO UPDATE SET valor = EXCLUDED.valor, actualizado_en = EXCLUDED.actualizado_en
            """;

    private static final String RESUMEN = """
            SELECT COALESCE(round(avg(valor)::numeric, 2), 0) AS promedio,
                   count(*) AS cantidad,
                   max(CASE WHEN usuario_id = :usuarioId THEN valor END) AS mia
            FROM calificacion
            WHERE tipo_contenido = :tipo AND contenido_id = :contenidoId
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioCalificacionesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardarOReemplazar(Calificacion calificacion) {
        jdbc.update(GUARDAR, new MapSqlParameterSource()
                .addValue("usuarioId", calificacion.usuarioId())
                .addValue("tipo", calificacion.tipoContenido().name())
                .addValue("contenidoId", calificacion.contenidoId())
                .addValue("valor", calificacion.valor())
                .addValue("fecha", ConversionesJdbc.fecha(calificacion.fecha())));
    }

    @Override
    public ResumenCalificacion resumen(TipoContenido tipo, UUID contenidoId, UUID usuarioId) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("tipo", tipo.name())
                .addValue("contenidoId", contenidoId)
                .addValue("usuarioId", usuarioId);
        return jdbc.queryForObject(RESUMEN, parametros, (fila, numero) -> {
            int mia = fila.getInt("mia");
            Integer miCalificacion = fila.wasNull() ? null : mia;
            return new ResumenCalificacion(tipo, contenidoId, fila.getDouble("promedio"), fila.getLong("cantidad"),
                    miCalificacion);
        });
    }
}
