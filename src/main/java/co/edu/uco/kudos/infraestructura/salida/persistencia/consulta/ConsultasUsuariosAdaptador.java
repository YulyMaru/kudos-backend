package co.edu.uco.kudos.infraestructura.salida.persistencia.consulta;

import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasUsuarios;
import co.edu.uco.kudos.dominio.modelo.Rol;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ConsultasUsuariosAdaptador implements ConsultasUsuarios {

    private static final String BUSQUEDA = """
            SELECT id, nombre_completo, rol
            FROM usuario
            WHERE estado = 'ACTIVO'
              AND (CAST(:excluir AS uuid) IS NULL OR id <> CAST(:excluir AS uuid))
              AND (nombre_completo ILIKE :patron ESCAPE '\\' OR correo ILIKE :patron ESCAPE '\\')
            ORDER BY nombre_completo, id
            LIMIT :limite
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultasUsuariosAdaptador(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PersonaVista> buscarActivos(String texto, int limite, UUID excluir) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("patron", "%" + escaparComodines(texto) + "%")
                .addValue("excluir", excluir == null ? null : excluir.toString())
                .addValue("limite", limite);
        return jdbc.query(BUSQUEDA, parametros, (fila, numero) -> new PersonaVista(
                fila.getObject("id", UUID.class), fila.getString("nombre_completo"),
                Rol.valueOf(fila.getString("rol"))));
    }

    static String escaparComodines(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
