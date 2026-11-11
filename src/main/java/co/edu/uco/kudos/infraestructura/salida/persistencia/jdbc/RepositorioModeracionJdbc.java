package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioModeracion;
import co.edu.uco.kudos.dominio.modelo.Reporte;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import co.edu.uco.kudos.dominio.modelo.Veto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class RepositorioModeracionJdbc implements RepositorioModeracion {

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioModeracionJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existeReporte(TipoContenido tipo, UUID contenidoId, UUID reportanteId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM reporte
                               WHERE tipo_contenido = :tipo AND contenido_id = :contenidoId
                                 AND reportante_id = :reportanteId)
                """, new MapSqlParameterSource()
                .addValue("tipo", tipo.name())
                .addValue("contenidoId", contenidoId)
                .addValue("reportanteId", reportanteId), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    @Override
    public void guardarReporte(Reporte reporte) {
        jdbc.update("""
                INSERT INTO reporte (id, tipo_contenido, contenido_id, reportante_id, motivo, estado, creado_en)
                VALUES (:id, :tipo, :contenidoId, :reportanteId, :motivo, 'PENDIENTE', :fecha)
                """, new MapSqlParameterSource()
                .addValue("id", reporte.id())
                .addValue("tipo", reporte.tipoContenido().name())
                .addValue("contenidoId", reporte.contenidoId())
                .addValue("reportanteId", reporte.reportanteId())
                .addValue("motivo", reporte.motivo())
                .addValue("fecha", ConversionesJdbc.fecha(reporte.creadoEn())));
    }

    @Override
    public void guardarVeto(Veto veto) {
        jdbc.update("""
                INSERT INTO veto (id, tipo_contenido, contenido_id, moderador_id, motivo, creado_en)
                VALUES (:id, :tipo, :contenidoId, :moderadorId, :motivo, :fecha)
                """, new MapSqlParameterSource()
                .addValue("id", veto.id())
                .addValue("tipo", veto.tipoContenido().name())
                .addValue("contenidoId", veto.contenidoId())
                .addValue("moderadorId", veto.moderadorId())
                .addValue("motivo", veto.motivo())
                .addValue("fecha", ConversionesJdbc.fecha(veto.creadoEn())));
    }

    @Override
    public void marcarReportesAtendidos(TipoContenido tipo, UUID contenidoId, Instant fecha) {
        jdbc.update("""
                UPDATE reporte SET estado = 'ATENDIDO', atendido_en = :fecha
                WHERE tipo_contenido = :tipo AND contenido_id = :contenidoId AND estado = 'PENDIENTE'
                """, new MapSqlParameterSource()
                .addValue("tipo", tipo.name())
                .addValue("contenidoId", contenidoId)
                .addValue("fecha", ConversionesJdbc.fecha(fecha)));
    }
}
