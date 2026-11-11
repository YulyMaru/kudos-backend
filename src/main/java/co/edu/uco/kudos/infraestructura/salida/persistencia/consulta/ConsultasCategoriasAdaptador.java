package co.edu.uco.kudos.infraestructura.salida.persistencia.consulta;

import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasCategorias;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.CategoriaRepositorioJpa;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConsultasCategoriasAdaptador implements ConsultasCategorias {

    public static final String CACHE_CATEGORIAS_ACTIVAS = "categorias-activas";

    private final CategoriaRepositorioJpa repositorio;

    public ConsultasCategoriasAdaptador(CategoriaRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Cacheable(CACHE_CATEGORIAS_ACTIVAS)
    public List<CategoriaVista> activas() {
        return repositorio.findByActivaTrueOrderByNombreAsc().stream()
                .map(categoria -> new CategoriaVista(categoria.getId(), categoria.getNombre()))
                .toList();
    }
}
