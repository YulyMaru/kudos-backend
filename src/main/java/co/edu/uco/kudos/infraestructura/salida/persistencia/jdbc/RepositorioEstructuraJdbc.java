package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioEstructura;
import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Programa;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioEstructuraJdbc implements RepositorioEstructura {

    private static final RowMapper<Facultad> FACULTAD = (fila, numero) -> new Facultad(
            ConversionesJdbc.uuid(fila, "id"), fila.getString("nombre"), fila.getBoolean("activa"));
    private static final RowMapper<Programa> PROGRAMA = (fila, numero) -> new Programa(
            ConversionesJdbc.uuid(fila, "id"), ConversionesJdbc.uuid(fila, "facultad_id"), fila.getString("nombre"),
            fila.getBoolean("activo"));

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioEstructuraJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Facultad> facultad(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jdbc.query("SELECT id, nombre, activa FROM facultad WHERE id = :id",
                new MapSqlParameterSource("id", id), FACULTAD).stream().findFirst();
    }

    @Override
    public Optional<Programa> programa(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jdbc.query("SELECT id, facultad_id, nombre, activo FROM programa WHERE id = :id",
                new MapSqlParameterSource("id", id), PROGRAMA).stream().findFirst();
    }

    @Override
    public boolean existeFacultad(String nombre) {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM facultad WHERE lower(nombre) = lower(:nombre))",
                new MapSqlParameterSource("nombre", nombre), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    @Override
    public boolean existePrograma(UUID facultadId, String nombre) {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM programa WHERE facultad_id = :facultadId AND lower(nombre) = lower(:nombre))",
                new MapSqlParameterSource("facultadId", facultadId).addValue("nombre", nombre), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    @Override
    public void guardarFacultad(Facultad facultad) {
        jdbc.update("INSERT INTO facultad (id, nombre, activa) VALUES (:id, :nombre, :activa)",
                new MapSqlParameterSource("id", facultad.id()).addValue("nombre", facultad.nombre())
                        .addValue("activa", facultad.activa()));
    }

    @Override
    public void guardarPrograma(Programa programa) {
        jdbc.update("INSERT INTO programa (id, facultad_id, nombre, activo) VALUES (:id, :facultadId, :nombre, :activo)",
                new MapSqlParameterSource("id", programa.id()).addValue("facultadId", programa.facultadId())
                        .addValue("nombre", programa.nombre()).addValue("activo", programa.activo()));
    }

    @Override
    public List<Facultad> facultadesActivas() {
        return jdbc.query("SELECT id, nombre, activa FROM facultad WHERE activa = TRUE ORDER BY nombre",
                new MapSqlParameterSource(), FACULTAD);
    }

    @Override
    public List<Programa> programasActivos(UUID facultadId) {
        return jdbc.query(
                "SELECT id, facultad_id, nombre, activo FROM programa WHERE facultad_id = :facultadId AND activo = TRUE ORDER BY nombre",
                new MapSqlParameterSource("facultadId", facultadId), PROGRAMA);
    }
}
