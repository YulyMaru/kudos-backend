package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioVinculos;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.dominio.modelo.Rol;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RepositorioVinculosJdbc implements RepositorioVinculos {

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioVinculosJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void vincular(UUID usuarioId, UUID programaId) {
        jdbc.update("""
                INSERT INTO vinculo_programa (usuario_id, programa_id) VALUES (:usuarioId, :programaId)
                ON CONFLICT (usuario_id, programa_id) DO NOTHING
                """, parametros(usuarioId, programaId));
    }

    @Override
    public boolean desvincular(UUID usuarioId, UUID programaId) {
        return jdbc.update("DELETE FROM vinculo_programa WHERE usuario_id = :usuarioId AND programa_id = :programaId",
                parametros(usuarioId, programaId)) > 0;
    }

    @Override
    public List<Programa> programasDe(UUID usuarioId) {
        return jdbc.query("""
                SELECT p.id, p.facultad_id, p.nombre, p.activo
                FROM vinculo_programa v
                JOIN programa p ON p.id = v.programa_id
                WHERE v.usuario_id = :usuarioId
                ORDER BY p.nombre
                """, new MapSqlParameterSource("usuarioId", usuarioId), (fila, numero) -> new Programa(
                ConversionesJdbc.uuid(fila, "id"), ConversionesJdbc.uuid(fila, "facultad_id"),
                fila.getString("nombre"), fila.getBoolean("activo")));
    }

    @Override
    public List<PersonaVista> miembros(UUID programaId) {
        return jdbc.query("""
                SELECT u.id, u.nombre_completo, u.rol
                FROM vinculo_programa v
                JOIN usuario u ON u.id = v.usuario_id
                WHERE v.programa_id = :programaId AND u.estado = 'ACTIVO'
                ORDER BY u.nombre_completo, u.id
                """, new MapSqlParameterSource("programaId", programaId), (fila, numero) -> new PersonaVista(
                ConversionesJdbc.uuid(fila, "id"), fila.getString("nombre_completo"),
                Rol.valueOf(fila.getString("rol"))));
    }

    private static MapSqlParameterSource parametros(UUID usuarioId, UUID programaId) {
        return new MapSqlParameterSource("usuarioId", usuarioId).addValue("programaId", programaId);
    }
}
