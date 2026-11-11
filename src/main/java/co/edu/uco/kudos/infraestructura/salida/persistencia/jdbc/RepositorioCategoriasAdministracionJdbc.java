package co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc;


import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCategoriasAdministracion;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.infraestructura.salida.persistencia.consulta.ConsultasCategoriasAdaptador;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioCategoriasAdministracionJdbc implements RepositorioCategoriasAdministracion {

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioCategoriasAdministracionJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Categoria> buscarPorId(UUID id) {
        return jdbc.query("SELECT id, nombre, activa FROM categoria WHERE id = :id",
                new MapSqlParameterSource("id", id),
                (fila, numero) -> new Categoria(ConversionesJdbc.uuid(fila, "id"), fila.getString("nombre"),
                        fila.getBoolean("activa"))).stream().findFirst();
    }

    @Override
    public boolean existeNombre(String nombre, UUID excluirId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM categoria
                               WHERE lower(nombre) = lower(:nombre)
                                 AND (CAST(:excluir AS uuid) IS NULL OR id <> CAST(:excluir AS uuid)))
                """, new MapSqlParameterSource()
                .addValue("nombre", nombre)
                .addValue("excluir", ConversionesJdbc.texto(excluirId)), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    @Override
    @CacheEvict(value = ConsultasCategoriasAdaptador.CACHE_CATEGORIAS_ACTIVAS, allEntries = true)
    public void guardar(Categoria categoria) {
        jdbc.update("""
                INSERT INTO categoria (id, nombre, activa) VALUES (:id, :nombre, :activa)
                ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre, activa = EXCLUDED.activa
                """, new MapSqlParameterSource()
                .addValue("id", categoria.getId())
                .addValue("nombre", categoria.getNombre())
                .addValue("activa", categoria.isActiva()));
    }
}
