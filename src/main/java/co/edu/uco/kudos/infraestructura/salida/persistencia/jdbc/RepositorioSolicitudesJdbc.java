package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.dto.SolicitudPendienteVista;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioSolicitudes;
import co.edu.uco.kudos.dominio.modelo.EstadoAprobacion;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioSolicitudesJdbc implements RepositorioSolicitudes {

    private static final String COLUMNAS = """
            SELECT id, usuario_id, rol_solicitado, facultad_id, programa_id, estado, decidida_por, motivo_rechazo,
                   politica_aceptada_en, creada_en, decidida_en
            FROM solicitud_registro
            """;

    private static final RowMapper<SolicitudRegistro> SOLICITUD = (fila, numero) -> new SolicitudRegistro(
            ConversionesJdbc.uuid(fila, "id"), ConversionesJdbc.uuid(fila, "usuario_id"),
            Rol.valueOf(fila.getString("rol_solicitado")), ConversionesJdbc.uuid(fila, "facultad_id"),
            ConversionesJdbc.uuid(fila, "programa_id"), EstadoAprobacion.valueOf(fila.getString("estado")),
            ConversionesJdbc.uuid(fila, "decidida_por"), fila.getString("motivo_rechazo"),
            ConversionesJdbc.instante(fila, "politica_aceptada_en"), ConversionesJdbc.instante(fila, "creada_en"),
            ConversionesJdbc.instante(fila, "decidida_en"));

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioSolicitudesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(SolicitudRegistro solicitud) {
        jdbc.update("""
                INSERT INTO solicitud_registro (id, usuario_id, rol_solicitado, facultad_id, programa_id, estado,
                                                decidida_por, motivo_rechazo, politica_aceptada_en, creada_en,
                                                decidida_en)
                VALUES (:id, :usuarioId, :rol, :facultadId, :programaId, :estado, :decididaPor, :motivo,
                        :politica, :creada, :decidida)
                """, parametros(solicitud));
    }

    @Override
    public void actualizar(SolicitudRegistro solicitud) {
        jdbc.update("""
                UPDATE solicitud_registro
                SET estado = :estado, decidida_por = :decididaPor, motivo_rechazo = :motivo, decidida_en = :decidida
                WHERE id = :id
                """, parametros(solicitud));
    }

    @Override
    public Optional<SolicitudRegistro> buscarPorId(UUID id) {
        return jdbc.query(COLUMNAS + " WHERE id = :id", new MapSqlParameterSource("id", id), SOLICITUD)
                .stream().findFirst();
    }

    @Override
    public Optional<SolicitudRegistro> buscarPorUsuario(UUID usuarioId) {
        return jdbc.query(COLUMNAS + " WHERE usuario_id = :usuarioId",
                new MapSqlParameterSource("usuarioId", usuarioId), SOLICITUD).stream().findFirst();
    }

    @Override
    public List<SolicitudPendienteVista> pendientes(List<Rol> rolesSolicitados, UUID programaId, UUID facultadId,
                                                    int limite) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.id, s.usuario_id, u.nombre_completo, u.correo, s.rol_solicitado,
                       f.nombre AS facultad, p.nombre AS programa, u.correo_verificado, u.celular_verificado,
                       s.creada_en
                FROM solicitud_registro s
                JOIN usuario u ON u.id = s.usuario_id
                LEFT JOIN programa p ON p.id = s.programa_id
                LEFT JOIN facultad f ON f.id = COALESCE(s.facultad_id, p.facultad_id)
                WHERE s.estado = 'PENDIENTE' AND s.rol_solicitado IN (:roles)
                """);
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("roles", rolesSolicitados.stream().map(Rol::name).toList())
                .addValue("limite", limite);
        if (programaId != null) {
            sql.append(" AND s.programa_id = :programaId");
            parametros.addValue("programaId", programaId);
        }
        if (facultadId != null) {
            sql.append(" AND p.facultad_id = :facultadId");
            parametros.addValue("facultadId", facultadId);
        }
        sql.append(" ORDER BY s.creada_en LIMIT :limite");
        return jdbc.query(sql.toString(), parametros, (fila, numero) -> new SolicitudPendienteVista(
                ConversionesJdbc.uuid(fila, "id"), ConversionesJdbc.uuid(fila, "usuario_id"),
                fila.getString("nombre_completo"), fila.getString("correo"),
                Rol.valueOf(fila.getString("rol_solicitado")), fila.getString("facultad"), fila.getString("programa"),
                fila.getBoolean("correo_verificado"), fila.getBoolean("celular_verificado"),
                ConversionesJdbc.instante(fila, "creada_en")));
    }

    private static MapSqlParameterSource parametros(SolicitudRegistro solicitud) {
        return new MapSqlParameterSource()
                .addValue("id", solicitud.id())
                .addValue("usuarioId", solicitud.usuarioId())
                .addValue("rol", solicitud.rolSolicitado().name())
                .addValue("facultadId", solicitud.facultadId())
                .addValue("programaId", solicitud.programaId())
                .addValue("estado", solicitud.estado().name())
                .addValue("decididaPor", solicitud.decididaPor())
                .addValue("motivo", solicitud.motivoRechazo())
                .addValue("politica", ConversionesJdbc.fecha(solicitud.politicaAceptadaEn()))
                .addValue("creada", ConversionesJdbc.fecha(solicitud.creadaEn()))
                .addValue("decidida", ConversionesJdbc.fecha(solicitud.decididaEn()));
    }
}
