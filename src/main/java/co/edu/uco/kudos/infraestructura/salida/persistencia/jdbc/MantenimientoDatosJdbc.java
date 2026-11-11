package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.puerto.salida.MantenimientoDatos;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class MantenimientoDatosJdbc implements MantenimientoDatos {

    private final NamedParameterJdbcTemplate jdbc;

    public MantenimientoDatosJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public int eliminarTokensRefrescoVencidos(Instant antesDe) {
        return jdbc.update("""
                DELETE FROM token_refresco
                WHERE expira_en < :antes OR (revocado_en IS NOT NULL AND revocado_en < :antes)
                """, parametros(antesDe));
    }

    @Override
    @Transactional
    public int eliminarNotificacionesFinalizadas(Instant antesDe) {
        return jdbc.update("""
                DELETE FROM outbox_notificacion
                WHERE estado IN ('ENVIADO', 'NO_APLICA', 'FALLIDO') AND creado_en < :antes
                """, parametros(antesDe));
    }

    @Override
    @Transactional
    public int eliminarCodigosVencidos(Instant antesDe) {
        return jdbc.update("DELETE FROM codigo_verificacion WHERE expira_en < :antes", parametros(antesDe));
    }

    private static MapSqlParameterSource parametros(Instant antesDe) {
        return new MapSqlParameterSource("antes", ConversionesJdbc.fecha(antesDe));
    }
}
