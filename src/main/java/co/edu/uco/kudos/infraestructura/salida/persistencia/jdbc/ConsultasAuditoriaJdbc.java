package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.dto.FiltroAuditoria;
import co.edu.uco.kudos.aplicacion.dto.PaginaAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasAuditoria;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConsultasAuditoriaJdbc implements ConsultasAuditoria {

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultasAuditoriaJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PaginaAuditoria buscar(FiltroAuditoria filtro, int pagina, int tamano) {
        List<String> condiciones = new ArrayList<>();
        MapSqlParameterSource parametros = new MapSqlParameterSource();
        if (filtro.desde() != null) {
            condiciones.add("fecha_hora >= :desde");
            parametros.addValue("desde", ConversionesJdbc.fecha(filtro.desde()));
        }
        if (filtro.hasta() != null) {
            condiciones.add("fecha_hora <= :hasta");
            parametros.addValue("hasta", ConversionesJdbc.fecha(filtro.hasta()));
        }
        if (filtro.actorId() != null) {
            condiciones.add("actor_id = :actorId");
            parametros.addValue("actorId", filtro.actorId());
        }
        if (filtro.accion() != null) {
            condiciones.add("accion = :accion");
            parametros.addValue("accion", filtro.accion().name());
        }
        if (filtro.recursoTipo() != null && !filtro.recursoTipo().isBlank()) {
            condiciones.add("recurso_tipo = :recursoTipo");
            parametros.addValue("recursoTipo", filtro.recursoTipo().trim().toUpperCase());
        }
        if (filtro.recursoId() != null) {
            condiciones.add("recurso_id = :recursoId");
            parametros.addValue("recursoId", filtro.recursoId());
        }
        if (filtro.idCorrelacion() != null) {
            condiciones.add("id_correlacion = :idCorrelacion");
            parametros.addValue("idCorrelacion", filtro.idCorrelacion());
        }
        String donde = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        Long total = jdbc.queryForObject("SELECT count(*) FROM registro_auditoria" + donde, parametros, Long.class);
        parametros.addValue("limite", tamano).addValue("desplazamiento", (long) pagina * tamano);
        List<EntradaAuditoria> registros = jdbc.query("""
                SELECT id, fecha_hora, id_correlacion, actor_id, rol_actor, accion, recurso_tipo, recurso_id,
                       canal, resultado, detalle
                FROM registro_auditoria""" + donde + " ORDER BY fecha_hora DESC, id LIMIT :limite OFFSET :desplazamiento",
                parametros, (fila, numero) -> {
                    String rol = fila.getString("rol_actor");
                    return new EntradaAuditoria(ConversionesJdbc.uuid(fila, "id"),
                            ConversionesJdbc.instante(fila, "fecha_hora"), ConversionesJdbc.uuid(fila, "id_correlacion"),
                            ConversionesJdbc.uuid(fila, "actor_id"), rol == null ? null : Rol.valueOf(rol),
                            AccionAuditoria.valueOf(fila.getString("accion")), fila.getString("recurso_tipo"),
                            ConversionesJdbc.uuid(fila, "recurso_id"), canal(fila.getString("canal")),
                            ResultadoAuditoria.valueOf(fila.getString("resultado")), fila.getString("detalle"));
                });
        return new PaginaAuditoria(registros, pagina, tamano, total == null ? 0 : total);
    }

    private static Canal canal(String valor) {
        if (valor == null) {
            return null;
        }
        return "WHATSAPP".equals(valor) ? Canal.SMS : Canal.valueOf(valor);
    }
}
