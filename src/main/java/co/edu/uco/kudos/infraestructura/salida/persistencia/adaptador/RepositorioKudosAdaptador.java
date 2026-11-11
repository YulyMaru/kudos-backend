package co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador;

import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioKudos;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador.KudoMapeador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.KudoRepositorioJpa;
import org.springframework.stereotype.Component;

@Component
public class RepositorioKudosAdaptador implements RepositorioKudos {

    private final KudoRepositorioJpa repositorio;

    public RepositorioKudosAdaptador(KudoRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(Kudo kudo) {
        repositorio.save(KudoMapeador.aEntidad(kudo));
    }
}
